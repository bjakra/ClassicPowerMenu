package com.kieronquinn.app.classicpowermenu.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 *  Graphite: SystemUI stays bound to this while a user is unlocked, which keeps the app's process
 *  and its cached cards alive, so holding power opens the menu without a cold start.
 */
class GraphiteWarmService: Service() {

    private val binder = Binder()

    override fun onBind(intent: Intent): IBinder = binder

}
