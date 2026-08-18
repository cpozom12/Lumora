package com.lumora.hub

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Minimal personal CPZ Media Hub entry surface.
 *
 * The activity does not browse provider catalogues, receive provider credentials, perform media
 * playback, or contact provider servers. It only detects a reviewed allow-list of installed
 * official apps and asks Android to open the selected app on the phone. Android Auto/head-unit
 * presentation remains controlled by the host platform.
 */
class HubActivity : AppCompatActivity() {

    private lateinit var launcher: ExternalProviderLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher = ExternalProviderLauncher(this)
        setContentView(buildHubView())
    }

    override fun onResume() {
        super.onResume()
        // Installed-state labels can change after returning from package management or another app.
        setContentView(buildHubView())
    }

    private fun buildHubView(): ScrollView {
        val density = resources.displayMetrics.density
        val pad = (20 * density).toInt()

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        content.addView(
            TextView(this).apply {
                text = getString(com.lumora.R.string.car_hub_title)
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, pad)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        ExternalMediaProviderRegistry.providers.forEach { provider ->
            val installed = launcher.isInstalled(provider)
            content.addView(
                Button(this).apply {
                    isAllCaps = false
                    text = buildString {
                        append(provider.displayName)
                        append("\n")
                        append(
                            getString(
                                if (installed) com.lumora.R.string.car_hub_installed
                                else com.lumora.R.string.car_hub_not_installed,
                            )
                        )
                    }
                    isEnabled = installed
                    setOnClickListener { launch(provider) }
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = (12 * density).toInt()
                },
            )
        }

        return ScrollView(this).apply { addView(content) }
    }

    private fun launch(provider: ExternalMediaProvider) {
        val message = when (launcher.launchOnPhone(provider)) {
            ExternalLaunchResult.Launched ->
                getString(com.lumora.R.string.car_hub_opening, provider.displayName)

            ExternalLaunchResult.NotInstalled ->
                getString(com.lumora.R.string.car_hub_not_installed_named, provider.displayName)

            is ExternalLaunchResult.Blocked ->
                getString(com.lumora.R.string.car_hub_launch_blocked, provider.displayName)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
