package com.lumora

import android.widget.Toast
import com.lumora.model.Channel

/**
 * Compatibility entry point retained while the personal Media Hub is separated from the inherited
 * scraper stack.
 *
 * The trusted CPZ build does not resolve films/series through bundled scraper sites or executable
 * plugins. Keeping this small facade lets the inherited catalogue UI compile without retaining a
 * runtime edge to [StreamResolver] and its third-party scraper graph. R8 can therefore remove that
 * graph from the shipped APK.
 */
internal fun MainActivity.showFindStreamDialog(
    @Suppress("UNUSED_PARAMETER") item: Channel,
    @Suppress("UNUSED_PARAMETER") season: Int? = null,
    @Suppress("UNUSED_PARAMETER") episode: Int? = null,
    @Suppress("UNUSED_PARAMETER") onResolved: ((Channel) -> Unit)? = null,
) {
    Toast.makeText(
        this,
        "Inherited web-scraper playback is disabled in the CPZ trusted build",
        Toast.LENGTH_LONG,
    ).show()
}
