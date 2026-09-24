package com.fearmikey.garage.ui.vehicle.scan

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fearmikey.garage.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class CustomScannerActivity : AppCompatActivity() {

    private lateinit var capture: CaptureManager
    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private lateinit var btnTorch: FloatingActionButton
    private lateinit var btnBack: FloatingActionButton
    private lateinit var btnShutter: ImageButton
    private var isTorchOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_barcode_scanner)

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner)

        capture = CaptureManager(this, barcodeScannerView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.setShowMissingCameraPermissionDialog(false)
        capture.decode()

        btnBack = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnTorch = findViewById(R.id.btn_torch)

        ViewCompat.setOnApplyWindowInsetsListener(barcodeScannerView) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val baseMargin = (20 * resources.displayMetrics.density).toInt()

            (btnBack.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.topMargin = statusBarHeight + baseMargin
                btnBack.layoutParams = params
            }

            (btnTorch.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.topMargin = statusBarHeight + baseMargin
                btnTorch.layoutParams = params
            }

            insets
        }

        btnShutter = findViewById(R.id.btn_shutter)
        btnShutter.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            barcodeScannerView.decodeSingle(
                object : BarcodeCallback {
                    override fun barcodeResult(result: BarcodeResult?) {
                        if ((result != null) && !result.text.isNullOrBlank()) {
                            val resultIntent = CaptureManager.resultIntent(result, null)
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@CustomScannerActivity,
                                getString(R.string.scan_vin_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
                },
            )
        }

        if (!hasFlash()) {
            btnTorch.visibility = View.GONE
        } else {
            btnTorch.setOnClickListener {
                if (isTorchOn) {
                    barcodeScannerView.setTorchOff()
                } else {
                    barcodeScannerView.setTorchOn()
                }
            }
        }

        barcodeScannerView.setTorchListener(
            object : DecoratedBarcodeView.TorchListener {
                override fun onTorchOn() {
                    isTorchOn = true
                }

                override fun onTorchOff() {
                    isTorchOn = false
                }
            },
        )
    }

    private fun hasFlash(): Boolean {
        return applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }
}
