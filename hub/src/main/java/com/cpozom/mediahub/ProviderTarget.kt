package com.cpozom.mediahub

import androidx.annotation.StringRes

internal data class ProviderTarget(
    @StringRes val labelRes: Int,
    val packageName: String,
)

internal object ProviderRegistry {
    val providers: List<ProviderTarget> = listOf(
        ProviderTarget(R.string.provider_movistar, "pe.movistar.go"),
        ProviderTarget(R.string.provider_netflix, "com.netflix.mediaclient"),
        ProviderTarget(R.string.provider_disney, "com.disney.disneyplus"),
        ProviderTarget(R.string.provider_youtube, "com.google.android.youtube"),
        ProviderTarget(R.string.provider_prime_video, "com.amazon.avod.thirdpartyclient"),
        ProviderTarget(R.string.provider_max, "com.wbd.stream"),
    )
}
