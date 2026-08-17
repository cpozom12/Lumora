package com.cpozom.mediahub

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class HubActivity : Activity() {

    private lateinit var providerGrid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    override fun onResume() {
        super.onResume()
        if (::providerGrid.isInitialized) renderProviders()
    }

    private fun buildContent(): ScrollView {
        val density = resources.displayMetrics.density
        val pad = (20 * density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = getString(R.string.hub_title)
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.hub_subtitle)
            textSize = 16f
            setPadding(0, (8 * density).toInt(), 0, (4 * density).toInt())
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.hub_note)
            textSize = 13f
            setPadding(0, 0, 0, (16 * density).toInt())
        })

        providerGrid = GridLayout(this).apply {
            columnCount = 2
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = true
        }
        root.addView(
            providerGrid,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        renderProviders()

        return ScrollView(this).apply { addView(root) }
    }

    private fun renderProviders() {
        providerGrid.removeAllViews()
        ProviderRegistry.providers.forEachIndexed { index, provider ->
            val installed = launchIntent(provider) != null
            val button = Button(this).apply {
                isAllCaps = false
                gravity = Gravity.CENTER
                text = buildString {
                    append(getString(provider.labelRes))
                    append("\n")
                    append(
                        getString(
                            if (installed) R.string.status_installed
                            else R.string.status_not_installed,
                        ),
                    )
                }
                isEnabled = installed
                setOnClickListener { launch(provider) }
            }
            val row = index / 2
            val column = index % 2
            providerGrid.addView(
                button,
                GridLayout.LayoutParams(
                    GridLayout.spec(row, 1f),
                    GridLayout.spec(column, 1f),
                ).apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                },
            )
        }
    }

    private fun launchIntent(provider: ProviderTarget): Intent? =
        packageManager.getLaunchIntentForPackage(provider.packageName)

    private fun launch(provider: ProviderTarget) {
        val label = getString(provider.labelRes)
        val intent = launchIntent(provider)
        if (intent == null) {
            Toast.makeText(this, getString(R.string.launch_missing, label), Toast.LENGTH_LONG).show()
            return
        }

        try {
            startActivity(intent)
        } catch (_: SecurityException) {
            Toast.makeText(this, getString(R.string.launch_blocked, label), Toast.LENGTH_LONG).show()
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.launch_missing, label), Toast.LENGTH_LONG).show()
        }
    }
}
