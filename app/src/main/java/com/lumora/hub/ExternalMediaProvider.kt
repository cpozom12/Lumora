package com.lumora.hub

/**
 * Commercial media apps that CPZ Media Hub can hand off to on the phone.
 *
 * Package ids are explicit and reviewed rather than discovered remotely. This keeps the hub
 * independent from executable plugin stores and prevents a remote catalogue from changing which
 * application is launched.
 */
data class ExternalMediaProvider(
    val id: String,
    val displayName: String,
    val packageNames: List<String>,
)

object ExternalMediaProviderRegistry {
    val providers: List<ExternalMediaProvider> = listOf(
        ExternalMediaProvider(
            id = "movistar_tv_pe",
            displayName = "Movistar TV App",
            packageNames = listOf("pe.movistar.go"),
        ),
        ExternalMediaProvider(
            id = "netflix",
            displayName = "Netflix",
            packageNames = listOf("com.netflix.mediaclient"),
        ),
        ExternalMediaProvider(
            id = "disney_plus",
            displayName = "Disney+",
            packageNames = listOf("com.disney.disneyplus"),
        ),
        ExternalMediaProvider(
            id = "youtube",
            displayName = "YouTube",
            packageNames = listOf("com.google.android.youtube"),
        ),
        ExternalMediaProvider(
            id = "prime_video",
            displayName = "Prime Video",
            packageNames = listOf("com.amazon.avod.thirdpartyclient"),
        ),
        ExternalMediaProvider(
            id = "hbo_max",
            displayName = "HBO Max",
            packageNames = listOf("com.wbd.stream"),
        ),
    )
}
