plugins {
    id("com.android.library")
}

// Customized ConnectSDK ("inshot cast core" fork) as shipped in the APK: upstream sources where the APK copy is
// unchanged, APK-specific services/utilities ported from the decompile.
android {
    namespace = "com.connectsdk"
    compileSdk = 35
    useLibrary("org.apache.http.legacy")

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(fileTree("libs") { include("*.jar") })
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.mediarouter:mediarouter:1.7.0")
    implementation("com.google.android.gms:play-services-cast-framework:22.0.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    implementation("org.jmdns:jmdns:3.5.12")
    implementation("com.googlecode.plist:dd-plist:1.28")
    implementation("com.nimbusds:srp6a:2.1.0")
    implementation("net.i2p.crypto:eddsa:0.3.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "5000"))
}
