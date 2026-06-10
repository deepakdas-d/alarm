package com.deepak.alarm.engine

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Detects OEM manufacturer and provides intent to auto-start settings.
 * Needed for Xiaomi, Oppo, Vivo, Huawei, Realme, OnePlus, Samsung.
 */
object AutoStartHelper {

    data class AutoStartInfo(
        val manufacturer: String,
        val intent: Intent?,
        val instructions: String
    )

    fun getManufacturer(): String = Build.MANUFACTURER.lowercase()

    fun getAutoStartInfo(context: Context): AutoStartInfo? {
        val manufacturer = getManufacturer()

        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                AutoStartInfo(
                    manufacturer = "Xiaomi",
                    intent = Intent().apply {
                        component = ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    },
                    instructions = "Go to Settings → Apps → Manage Apps → Find this app → Auto-start → Enable"
                )
            }
            manufacturer.contains("oppo") -> {
                AutoStartInfo(
                    manufacturer = "Oppo",
                    intent = Intent().apply {
                        component = ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                        )
                    },
                    instructions = "Go to Settings → App Management → Auto-start → Enable for this app"
                )
            }
            manufacturer.contains("vivo") -> {
                AutoStartInfo(
                    manufacturer = "Vivo",
                    intent = Intent().apply {
                        component = ComponentName(
                            "com.vivo.permissionmanager",
                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                        )
                    },
                    instructions = "Go to Settings → More Settings → Applications → Auto-start → Enable"
                )
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                AutoStartInfo(
                    manufacturer = "Huawei",
                    intent = Intent().apply {
                        component = ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                        )
                    },
                    instructions = "Go to Settings → Battery → App Launch → Set this app to Manage Manually → Enable Auto-launch"
                )
            }
            manufacturer.contains("realme") -> {
                AutoStartInfo(
                    manufacturer = "Realme",
                    intent = Intent().apply {
                        component = ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                        )
                    },
                    instructions = "Go to Settings → App Management → Auto-start → Enable for this app"
                )
            }
            manufacturer.contains("oneplus") -> {
                AutoStartInfo(
                    manufacturer = "OnePlus",
                    intent = Intent().apply {
                        component = ComponentName(
                            "com.oneplus.security",
                            "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                        )
                    },
                    instructions = "Go to Settings → Battery → Battery Optimization → Choose this app → Don't optimize"
                )
            }
            manufacturer.contains("samsung") -> {
                AutoStartInfo(
                    manufacturer = "Samsung",
                    intent = Intent().apply {
                        component = ComponentName(
                            "com.samsung.android.lool",
                            "com.samsung.android.sm.battery.ui.BatteryActivity"
                        )
                    },
                    instructions = "Go to Settings → Battery → App Power Management → Put this app in 'Never Sleeping' list"
                )
            }
            else -> null
        }
    }

    fun isAutoStartAvailable(context: Context): Boolean {
        val info = getAutoStartInfo(context) ?: return false
        return info.intent?.let { intent ->
            context.packageManager.resolveActivity(intent, 0) != null
        } ?: false
    }
}
