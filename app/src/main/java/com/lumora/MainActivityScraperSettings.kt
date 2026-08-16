package com.lumora

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Compatibility settings pane for the CPZ trusted build.
 *
 * Site scraping and custom scraper DNS controls are intentionally unavailable. Keeping this
 * source-level facade avoids a broad settings-layout migration while removing every runtime edge
 * from the settings UI to the inherited scraper implementation.
 */
internal fun MainActivity.wireScraperSettingsPane(root: View) {
    val host = root.findViewById<LinearLayout>(R.id.settingsSitesRows) ?: return
    host.removeAllViews()
    host.addView(
        TextView(this).apply {
            text = "Web-site scrapers are disabled in the CPZ trusted build"
            setTextColor(getColor(R.color.text_secondary))
            setPadding(
                resources.getDimensionPixelSize(R.dimen.settings_gap_l),
                resources.getDimensionPixelSize(R.dimen.settings_gap_l),
                resources.getDimensionPixelSize(R.dimen.settings_gap_l),
                resources.getDimensionPixelSize(R.dimen.settings_gap_l),
            )
        }
    )
}
