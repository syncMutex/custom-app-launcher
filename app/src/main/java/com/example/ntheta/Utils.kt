package com.example.ntheta

import android.app.admin.DeviceAdminReceiver
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.regex.PatternSyntaxException

class AdminReceiver : DeviceAdminReceiver() {
}

@Stable
class SearchPat(pat: String?) {
    var pattern by mutableStateOf(pat)
    private var reg by mutableStateOf(Regex("", option = RegexOption.IGNORE_CASE))

    fun setPat(newPat: String) {
        pattern = newPat
        try {
            reg = Regex(newPat, option = RegexOption.IGNORE_CASE)
        } catch (_: PatternSyntaxException) {
        }
    }

    fun reset() {
        pattern = null
    }

    fun containsMatchIn(s: String): Boolean {
        return if(pattern == null) {
            true
        } else {
            reg.containsMatchIn(s)
        }
    }
}

fun getInstalledApps(pm: PackageManager): List<AppInfo> {
    val listMut = mutableListOf<AppInfo>()

    val intent = Intent(Intent.ACTION_MAIN, null)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)

    pm.queryIntentActivities(intent, 0).forEach {
        val aInfo = it.activityInfo.applicationInfo
        val label = pm.getApplicationLabel(aInfo) as String
        val appInfo = AppInfo.newBuilder()
            .setPackageName(aInfo.packageName)
            .setLabel(label)
            .setId("${label}${aInfo.packageName}")
            .build()

        if(appInfo.packageName != PACKAGE_NAME) {
            listMut.add(appInfo)
        }
    }

    return listMut.toSortedSet(compareBy({ it.label }, { it.label })).toList()
}
