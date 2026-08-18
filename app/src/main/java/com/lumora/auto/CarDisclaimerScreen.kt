package com.lumora.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

/** First screen of each car session: short, explicit parked-use boundary. */
class CarDisclaimerScreen(
    carContext: CarContext,
    private val session: LumoraCarSession,
) : Screen(carContext) {

    override fun onGetTemplate(): Template =
        MessageTemplate.Builder(carContext.getString(com.lumora.R.string.car_hub_disclaimer))
            .setTitle(carContext.getString(com.lumora.R.string.car_hub_title))
            .setHeaderAction(Action.APP_ICON)
            .addAction(
                Action.Builder()
                    .setTitle(carContext.getString(com.lumora.R.string.ui_not_driving_continue))
                    .setOnClickListener {
                        session.disclaimerAccepted = true
                        screenManager.push(CarHubScreen(carContext, session))
                    }
                    .build()
            )
            .build()
}
