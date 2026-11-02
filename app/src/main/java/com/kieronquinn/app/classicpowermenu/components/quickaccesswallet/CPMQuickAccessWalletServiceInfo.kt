package com.kieronquinn.app.classicpowermenu.components.quickaccesswallet

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.provider.Settings
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.service.quickaccesswallet.QuickAccessWalletService
import android.util.Log

object CPMQuickAccessWalletServiceInfoBuilder {

    @SuppressLint("BlockedPrivateApi")
    fun create(context: Context): Any? {
        val quickAccessWalletServiceInfoClass = Class.forName("android.service.quickaccesswallet.QuickAccessWalletServiceInfo")
        val quickAccessWalletServiceMetadataClass = Class.forName("android.service.quickaccesswallet.QuickAccessWalletServiceInfo\$ServiceMetadata")
        val defaultPaymentApp = getDefaultPaymentApp(context) ?: return null
        val serviceInfo = getWalletServiceInfo(context, defaultPaymentApp.packageName) ?: return null
        if (Manifest.permission.BIND_QUICK_ACCESS_WALLET_SERVICE != serviceInfo.permission) {
            Log.w("QAWalletSInfo", String.format("%s.%s does not require permission %s", serviceInfo.packageName, serviceInfo.name, Manifest.permission.BIND_QUICK_ACCESS_WALLET_SERVICE))
            return null
        }
        val metadata = quickAccessWalletServiceInfoClass.getDeclaredMethod("parseServiceMetadata", Context::class.java, ServiceInfo::class.java).apply {
            isAccessible = true
        }.invoke(null, context, serviceInfo)
        return quickAccessWalletServiceInfoClass.getDeclaredConstructor(ServiceInfo::class.java, quickAccessWalletServiceMetadataClass).apply {
            isAccessible = true
        }.newInstance(serviceInfo, metadata)
    }

    /**
     *  Graphite: upstream hard-codes Google's service. This uses the wallet the user has chosen: the
     *  default contactless payment app, then the holder of the wallet role.
     */
    @JvmStatic
    fun getDefaultPaymentApp(context: Context): ComponentName? {
        val comp = Settings.Secure.getString(context.contentResolver, "nfc_payment_default_component")
        comp?.let { ComponentName.unflattenFromString(it) }?.let { return it }
        val holders = RoleManager::class.java.getMethod("getRoleHolders", String::class.java)
            .invoke(context.getSystemService(RoleManager::class.java), "android.app.role.WALLET") as List<*>
        val holder = holders.firstOrNull() as? String ?: return null
        return ComponentName(holder, "")
    }

    private fun getWalletServiceInfo(context: Context, packageName: String): ServiceInfo? {
        val intent = Intent(QuickAccessWalletService.SERVICE_INTERFACE)
        intent.setPackage(packageName)
        val resolveInfos = context.packageManager.queryIntentServices(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return if (resolveInfos.isEmpty()) null else resolveInfos[0].serviceInfo
    }

}