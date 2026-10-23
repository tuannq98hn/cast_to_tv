package com.casttotv.screen.mirroring.ui.activities.function.screen_mirror

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityQrScannerBinding

/** Port of `QrScanActivity`: scans the bcastmirror.com QR code and returns its text. */
class QrScanActivity : BaseActivity() {

    private lateinit var scanner: CodeScanner
    private var cameraGranted = false

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (granted) scanner.startPreview()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.llBack.setOnClickListener { finish() }
        scanner = CodeScanner(this, binding.scanner)
        scanner.decodeCallback = DecodeCallback { result ->
            runOnUiThread {
                setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULT, result.text))
                finish()
            }
        }
        scanner.errorCallback = ErrorCallback { error ->
            runOnUiThread { Toast.makeText(this, getString(R.string.scanner_error, error), Toast.LENGTH_LONG).show() }
        }
        cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!cameraGranted) cameraPermission.launch(Manifest.permission.CAMERA)
    }

    override fun onResume() {
        super.onResume()
        if (cameraGranted) scanner.startPreview()
    }

    override fun onPause() {
        scanner.releaseResources()
        super.onPause()
    }

    companion object {
        const val EXTRA_RESULT = "qr_result"
    }
}
