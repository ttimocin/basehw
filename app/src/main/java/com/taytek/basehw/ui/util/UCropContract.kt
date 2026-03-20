package com.taytek.basehw.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.yalantis.ucrop.UCrop

class UCropContract : ActivityResultContract<UCropContract.UCropInput, Uri?>() {
    data class UCropInput(
        val source: Uri,
        val destination: Uri,
        val aspectRatioX: Float = 1f,
        val aspectRatioY: Float = 1f
    )

    override fun createIntent(context: Context, input: UCropInput): Intent {
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
            setHideBottomControls(false)
            setToolbarColor(Color(0xFFE53935).toArgb()) // HotWheelsRed
            setStatusBarColor(Color(0xFFB71C1C).toArgb()) // Darker red for status bar
            setToolbarTitle(" ")
            setToolbarWidgetColor(Color.White.toArgb())
            setActiveControlsWidgetColor(Color(0xFFE53935).toArgb())
        }

        return UCrop.of(input.source, input.destination)
            .withAspectRatio(input.aspectRatioX, input.aspectRatioY)
            .withOptions(options)
            .getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent != null) {
            UCrop.getOutput(intent)
        } else {
            null
        }
    }
}
