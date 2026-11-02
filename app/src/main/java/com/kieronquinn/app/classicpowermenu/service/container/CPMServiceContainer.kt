package com.kieronquinn.app.classicpowermenu.service.container

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.kieronquinn.app.classicpowermenu.IClassicPowerMenu
import com.kieronquinn.app.classicpowermenu.service.root.CPMRootService
import com.kieronquinn.app.classicpowermenu.service.root.impl.CPMRootServiceImpl
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface CPMServiceContainer {

    suspend fun <T> runWithService(block: suspend (IClassicPowerMenu) -> T): T
    fun <T> runWithServiceIfAvailable(block: (IClassicPowerMenu) -> T?): T?
    suspend fun unbindServiceIfNeeded()

}

/**
 *  Graphite: the privileged calls run in this process as a system app. Root is only used for
 *  loyalty cards, and only if the device has it.
 */
class CPMServiceContainerImpl(private val context: Context): CPMServiceContainer {

    private val service by lazy {
        bindRootIfAvailable()
        CPMRootServiceImpl(context) { rootService }
    }

    @Volatile
    private var rootService: IClassicPowerMenu? = null
    private var rootConnection: ServiceConnection? = null

    private fun bindRootIfAvailable() = GlobalScope.launch {
        if(Shell.isAppGrantedRoot() == false || !Shell.getShell().isRoot) return@launch
        withContext(Dispatchers.Main) {
            val connection = object: ServiceConnection {
                override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                    rootService = IClassicPowerMenu.Stub.asInterface(binder)
                }

                override fun onServiceDisconnected(component: ComponentName) {
                    rootService = null
                }
            }
            rootConnection = connection
            RootService.bind(Intent(context, CPMRootService::class.java), connection)
        }
    }

    override suspend fun <T> runWithService(block: suspend (IClassicPowerMenu) -> T): T {
        return block(service)
    }

    override suspend fun unbindServiceIfNeeded() {
        rootConnection?.let {
            RootService.unbind(it)
            rootConnection = null
            rootService = null
        }
    }

    override fun <T> runWithServiceIfAvailable(block: (IClassicPowerMenu) -> T?): T? {
        return block(service)
    }

}
