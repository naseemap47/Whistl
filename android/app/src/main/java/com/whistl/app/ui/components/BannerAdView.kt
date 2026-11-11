package com.whistl.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Google's official sample Banner ad unit ID for testing.
 * Replace with your production Ad Unit ID before publishing to Google Play.
 */
const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

/**
 * A Jetpack Compose wrapper for Google AdMob Banner ads.
 *
 * If the device is offline or the ad fails to load, this composable collapses
 * to 0 height so that no empty blank space or placeholder is shown to the user.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = TEST_BANNER_AD_UNIT_ID
) {
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }

    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    isAdLoaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    isAdLoaded = false
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    // Outer container: collapses to 0.dp height if offline or ad not loaded
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isAdLoaded) 50.dp else 0.dp)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.requiredSize(width = 320.dp, height = 50.dp)
        )
    }
}

