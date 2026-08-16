package com.lumora.auto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.Screen
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/** Lumora on Android Auto. */
class LumoraCarAppService : CarAppService() {

    /**
     * Accept only real/known Car App Library hosts.
     *
     * Upstream used ALLOW_ALL_HOSTS_VALIDATOR, which Android documents as development-only.
     * The official sample allow-list includes known Android Auto/Automotive hosts that do not
     * hold TEMPLATE_RENDERER_PERMISSION; hosts on newer Android may also be accepted through the
     * privileged permission check performed by HostValidator.
     */
    override fun createHostValidator(): HostValidator =
        HostValidator.Builder(this)
            .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
            .build()

    override fun onCreateSession(): Session = LumoraCarSession()
}

/** One connection to the car. Owns playback that outlives individual screens. */
class LumoraCarSession : Session(), DefaultLifecycleObserver {

    val playback: CarPlayback by lazy { CarPlayback(carContext) }

    init {
        lifecycle.addObserver(this)
    }

    var disclaimerAccepted = false

    override fun onCreateScreen(intent: Intent): Screen = CarDisclaimerScreen(carContext, this)

    override fun onDestroy(owner: LifecycleOwner) {
        playback.release()
    }
}
