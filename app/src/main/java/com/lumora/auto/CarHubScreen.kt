package com.lumora.auto

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.lumora.R
import com.lumora.hub.ExternalLaunchResult
import com.lumora.hub.ExternalMediaProvider
import com.lumora.hub.ExternalMediaProviderRegistry
import com.lumora.hub.ExternalProviderLauncher

/**
 * Personal CPZ Media Hub root shown after the parked-use notice.
 *
 * The hub deliberately contains only reviewed, static provider package ids. A row can hand off to
 * an official app on the phone, but it never tries to impersonate a provider, copy its credentials,
 * or force a third-party app onto the Android Auto screen.
 */
class CarHubScreen(
    carContext: CarContext,
    private val session: LumoraCarSession,
) : Screen(carContext) {

    private val launcher = ExternalProviderLauncher(carContext)

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder().apply {
            ExternalMediaProviderRegistry.providers.forEach { provider ->
                addItem(providerRow(provider))
            }
        }.build()

        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.car_hub_title))
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list)
            .build()
    }

    private fun providerRow(provider: ExternalMediaProvider): Row {
        val installed = launcher.isInstalled(provider)
        return Row.Builder()
            .setTitle(provider.displayName)
            .addText(
                carContext.getString(
                    if (installed) R.string.car_hub_installed else R.string.car_hub_not_installed
                )
            )
            .setOnClickListener { onProviderSelected(provider) }
            .build()
    }

    private fun onProviderSelected(provider: ExternalMediaProvider) {
        when (launcher.launchOnPhone(provider)) {
            ExternalLaunchResult.Launched ->
                CarToast.makeText(
                    carContext,
                    carContext.getString(R.string.car_hub_opening, provider.displayName),
                    CarToast.LENGTH_SHORT,
                ).show()

            ExternalLaunchResult.NotInstalled ->
                CarToast.makeText(
                    carContext,
                    carContext.getString(R.string.car_hub_not_installed_named, provider.displayName),
                    CarToast.LENGTH_LONG,
                ).show()

            is ExternalLaunchResult.Blocked ->
                CarToast.makeText(
                    carContext,
                    carContext.getString(R.string.car_hub_launch_blocked, provider.displayName),
                    CarToast.LENGTH_LONG,
                ).show()
        }
    }
}
