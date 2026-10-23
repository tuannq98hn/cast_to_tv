package com.casttotv.screen.mirroring.tv

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Date
import java.util.concurrent.Executors
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/**
 * Port of `gf`/`ff` (the obfuscated "UniMote" client): Android TV Remote protocol v2.
 * Pairing runs on port 6467 (polo messages, 6-digit hex code shown on the TV); keys go to port 6466.
 */
object AndroidTvRemote {
    interface Listener {
        fun onConnected()
        fun onPairingRequired()
        fun onFailed()
    }

    private val io = Executors.newSingleThreadExecutor()
    private var socket: SSLSocket? = null
    private var out: OutputStream? = null
    private var host: String? = null
    private lateinit var keyStoreFile: File
    private var pairingSocket: SSLSocket? = null

    val isConnected get() = socket?.isConnected == true && !socket!!.isClosed

    fun connect(context: Context, host: String, listener: Listener) {
        keyStoreFile = File(context.filesDir, "atvremote.p12")
        io.execute {
            this.host = host
            runCatching { openRemote(host) }
                .onSuccess { listener.onConnected() }
                .onFailure {
                    // An unknown client certificate is rejected during the handshake: pair first.
                    runCatching { startPairing(host) }.onSuccess { listener.onPairingRequired() }.onFailure { listener.onFailed() }
                }
        }
    }

    /** Finishes pairing with the [code] shown on the TV, then opens the remote channel. */
    fun pair(code: String, listener: Listener) {
        io.execute {
            val ok = runCatching { finishPairing(code.trim().uppercase()) }.getOrDefault(false)
            if (ok) runCatching { openRemote(host!!) }.onSuccess { listener.onConnected() }.onFailure { listener.onFailed() }
            else listener.onFailed()
        }
    }

    /** `df` case 0: a key is START_LONG followed by END_LONG. */
    fun sendKey(keyCode: Int) {
        io.execute {
            runCatching {
                send(field(10, msg(varint(1, keyCode) + varint(2, 1))))
                send(field(10, msg(varint(1, keyCode) + varint(2, 2))))
            }.onFailure { close() }
        }
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
        out = null
    }

    // region remote channel

    private fun openRemote(host: String) {
        close()
        val ssl = sslContext().socketFactory.createSocket(host, 6466) as SSLSocket
        ssl.useClientMode = true
        ssl.keepAlive = true
        ssl.tcpNoDelay = true
        ssl.startHandshake()
        socket = ssl
        out = ssl.outputStream
        val input = ssl.inputStream
        Thread { readLoop(input) }.start()
    }

    /** `cf` case 0: answer configure / set-active / ping so the TV keeps the session. */
    private fun readLoop(input: InputStream) {
        while (isConnected) {
            val message = runCatching { readDelimited(input) }.getOrNull() ?: break
            val fields = parse(message)
            when {
                1 in fields -> {
                    val code = parse(fields.getValue(1) as ByteArray)[1] as? Long ?: 622
                    val info = msg(
                        string(1, Build.MODEL) + string(2, Build.MANUFACTURER) + varint(3, 1) +
                            string(4, Build.VERSION.RELEASE) + string(5, "com.casttotv.screen.mirroring") + string(6, "1.0.0"),
                    )
                    send(field(1, msg(varint(1, code.toInt()) + field(2, info))))
                }
                2 in fields -> send(field(2, msg(varint(1, 622))))
                8 in fields -> {
                    val value = parse(fields.getValue(8) as ByteArray)[1] as? Long ?: 0
                    send(field(9, msg(varint(1, value.toInt()))))
                }
            }
        }
        close()
    }

    @Synchronized
    private fun send(message: ByteArray) {
        val stream = out ?: return
        stream.write(lengthPrefix(message.size) + message)
        stream.flush()
    }

    // endregion

    // region pairing

    private fun startPairing(host: String) {
        val ssl = sslContext().socketFactory.createSocket(host, 6467) as SSLSocket
        ssl.startHandshake()
        pairingSocket = ssl
        val ok = { m: ByteArray -> varint(1, 2) + varint(2, 200) + m }
        sendPairing(ok(field(10, msg(string(1, "atvremote") + string(2, Build.MODEL)))))
        readPairing()
        val hex6 = msg(varint(1, 3) + varint(2, 6))
        sendPairing(ok(field(20, msg(field(1, hex6) + field(2, hex6) + varint(3, 1)))))
        readPairing()
        sendPairing(ok(field(30, msg(field(1, hex6) + varint(2, 1)))))
        readPairing()
    }

    /** Secret = SHA-256(client n, client e, server n, server e, code[2..]); code[0..2] must match hash[0]. */
    private fun finishPairing(code: String): Boolean {
        val ssl = pairingSocket ?: return false
        val client = ssl.session.localCertificates[0].publicKey as RSAPublicKey
        val server = (ssl.session.peerCertificates[0] as X509Certificate).publicKey as RSAPublicKey
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(unsigned(client.modulus))
            update(unsigned(client.publicExponent))
            update(unsigned(server.modulus))
            update(unsigned(server.publicExponent))
            update(hex(code.substring(2)))
        }.digest()
        if (digest[0] != code.substring(0, 2).toInt(16).toByte()) return false
        sendPairing(varint(1, 2) + varint(2, 200) + field(40, msg(bytesField(1, digest))))
        val answer = parse(readPairing())
        runCatching { ssl.close() }
        pairingSocket = null
        return (answer[2] as? Long) == 200L
    }

    private fun sendPairing(message: ByteArray) {
        val stream = pairingSocket!!.outputStream
        stream.write(lengthPrefix(message.size) + message)
        stream.flush()
    }

    private fun readPairing(): ByteArray = readDelimited(pairingSocket!!.inputStream)

    // endregion

    // region TLS identity

    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private fun sslContext(): SSLContext {
        val keyStore = identity()
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(keyStore, CharArray(0)) }
        // ponytail: TVs present self-signed certificates; trust-all mirrors the original `xe` trust manager.
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        return SSLContext.getInstance("TLS").apply { init(kmf.keyManagers, arrayOf(trustAll), null) }
    }

    /** Self-signed RSA client certificate kept across runs (the TV remembers it after pairing). */
    private fun identity(): KeyStore {
        val store = KeyStore.getInstance("PKCS12")
        if (keyStoreFile.exists()) {
            keyStoreFile.inputStream().use { store.load(it, CharArray(0)) }
            return store
        }
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val name = X500Name("CN=atvremote")
        val now = System.currentTimeMillis()
        val holder = JcaX509v3CertificateBuilder(
            name, BigInteger.valueOf(now), Date(now - 86_400_000L), Date(now + 3650L * 86_400_000L), name, pair.public,
        ).build(JcaContentSignerBuilder("SHA256WithRSA").build(pair.private))
        val cert = JcaX509CertificateConverter().getCertificate(holder)
        store.load(null, null)
        store.setKeyEntry("atvremote", pair.private, CharArray(0), arrayOf(cert))
        keyStoreFile.outputStream().use { store.store(it, CharArray(0)) }
        return store
    }

    // endregion

    // region minimal protobuf

    private fun varintBytes(value: Long): ByteArray {
        val out = ByteArrayOutputStream()
        var v = value
        while (true) {
            if (v and 0x7F.inv().toLong() == 0L) {
                out.write(v.toInt())
                return out.toByteArray()
            }
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
    }

    private fun varint(field: Int, value: Int) = varintBytes((field shl 3).toLong()) + varintBytes(value.toLong())
    private fun bytesField(field: Int, value: ByteArray) = varintBytes(((field shl 3) or 2).toLong()) + varintBytes(value.size.toLong()) + value
    private fun string(field: Int, value: String) = bytesField(field, value.toByteArray())
    private fun field(field: Int, message: ByteArray) = bytesField(field, message)
    private fun msg(content: ByteArray) = content
    private fun lengthPrefix(size: Int) = varintBytes(size.toLong())

    private fun readDelimited(input: InputStream): ByteArray {
        var shift = 0
        var length = 0L
        while (true) {
            val b = input.read()
            if (b < 0) throw java.io.EOFException()
            length = length or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        val data = ByteArray(length.toInt())
        var read = 0
        while (read < data.size) {
            val n = input.read(data, read, data.size - read)
            if (n < 0) throw java.io.EOFException()
            read += n
        }
        return data
    }

    /** Field number → Long (varint) or ByteArray (length-delimited); enough for the messages above. */
    private fun parse(data: ByteArray): Map<Int, Any> {
        val result = HashMap<Int, Any>()
        var i = 0
        fun readVarint(): Long {
            var shift = 0
            var v = 0L
            while (true) {
                val b = data[i++].toInt() and 0xFF
                v = v or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return v
                shift += 7
            }
        }
        while (i < data.size) {
            val key = readVarint()
            val number = (key ushr 3).toInt()
            when ((key and 7).toInt()) {
                0 -> result[number] = readVarint()
                2 -> {
                    val len = readVarint().toInt()
                    result[number] = data.copyOfRange(i, i + len)
                    i += len
                }
                5 -> i += 4
                1 -> i += 8
                else -> return result
            }
        }
        return result
    }

    private fun unsigned(value: BigInteger): ByteArray {
        var hexValue = value.toString(16).uppercase()
        if (hexValue.length % 2 != 0) hexValue = "0$hexValue"
        return hex(hexValue)
    }

    private fun hex(value: String) = ByteArray(value.length / 2) { value.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    // endregion
}
