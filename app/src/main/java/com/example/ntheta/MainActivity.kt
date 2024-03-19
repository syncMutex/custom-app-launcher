package com.example.ntheta

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.example.ntheta.ui.theme.NthetaTheme
import kotlinx.coroutines.launch

lateinit var PACKAGE_NAME: String

val Context.dataStore: DataStore<Store> by dataStore("appInfo.pb", serializer = StoreSerializer)

const val ONE_TAP_LOCKSCREEN = "com.coloros.onekeylockscreen"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dm = getSystemService(DevicePolicyManager::class.java)
        val compName = ComponentName(this, AdminReceiver::class.java)

        PACKAGE_NAME = applicationContext.packageName

        if(dm != null && !dm.isAdminActive(compName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra("android.app.extra.DEVICE_ADMIN", compName)
            intent.putExtra("android.app.extra.ADD_EXPLANATION", "")
            startActivity(intent, null)
        }

        val lockScreen = fun() {
            val intent = packageManager.getLaunchIntentForPackage(ONE_TAP_LOCKSCREEN)
            if(intent != null) {
                ContextCompat.startActivity(this, intent, null)
                return
            }

            if(dm.isAdminActive(compName)) {
                dm.lockNow()
            } else {
                Toast.makeText( this, "no admin policy thingie", Toast.LENGTH_SHORT).show()
            }
        }

        val dataStoreHiddenAppMapManager = DataStoreManager(this)

        setContent {
            NthetaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLayout(lockScreen, dataStoreHiddenAppMapManager)
                }
            }
        }
    }
}

class CustomScrollManager {
    var offset by mutableFloatStateOf(0f)
    var sensitivity by mutableFloatStateOf(1f)

    fun scrollTo(to: Float) {
        offset = to
    }
}

@Composable
fun MainLayout(lockScreen: () -> Unit, dataStoreManager: DataStoreManager) {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val scope = rememberCoroutineScope()
    // don't make it mutableListOf because it won't update the ui because i am reassigning the array itself
    val apps = remember { mutableStateOf(getInstalledApps(pm)) }
    var hiddenApps by remember { mutableStateOf(mapOf<String, AppInfo>()) }
    val fl = dataStoreManager.getHiddenList()
    val forceComposeToggle = remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf(Screen.All) }
    val searchPat = remember { SearchPat(null) }
    val curRow = remember { CurInteractRow() }
    val scrollState = remember { CustomScrollManager() }

    val forceCompose = fun () {
        forceComposeToggle.value = !forceComposeToggle.value
    }
    LaunchedEffect(forceComposeToggle.value) {}

    val filterHiddenApps = fun() {
        apps.value = apps.value.filter { !hiddenApps.containsKey(it.id) }
    }

    val addToApps = fun(app: AppInfo) {
        val l = apps.value.toMutableList()
        l.add(app)
        apps.value = l.toSortedSet(compareBy({ it.label }, { it.label })).toList()
    }

    var isHiddenAppsReady by remember { mutableStateOf(false) }

    LaunchedEffect(fl) {
        fl.collect {
            hiddenApps = it.hiddenMapMap
            filterHiddenApps()
            scrollState.sensitivity = it.scrollSensitivity
            if(it.scrollSensitivity == 0f) {
                dataStoreManager.setSensitivity(1f)
                scrollState.sensitivity = 1f
            }
            isHiddenAppsReady = true
        }
    }

    val refresh = fun () {
        apps.value = getInstalledApps(pm)
        filterHiddenApps()
    }

    val setCurrentScreen = fun(type: Screen) {
        currentScreen = type
        curRow.reset()
        searchPat.reset()
        scrollState.scrollTo(0f)
    }

    SystemBroadcastRecvr(systemAction = Intent.ACTION_SCREEN_OFF) {intent ->
        if(intent?.action == Intent.ACTION_SCREEN_OFF) {
            setCurrentScreen(Screen.All)
        }
    }

    SystemBroadcastRecvr(systemAction = Intent.ACTION_PACKAGE_ADDED, dataScheme = "package") {intent ->
        Toast.makeText( ctx, "new package added", Toast.LENGTH_SHORT).show()
        if(intent?.action == Intent.ACTION_PACKAGE_ADDED) {
            refresh()
        }
    }

    SystemBroadcastRecvr(systemAction = Intent.ACTION_PACKAGE_FULLY_REMOVED, dataScheme = "package") {intent ->
        if(intent?.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
            refresh()
            val uninstalledPackage = intent.data?.schemeSpecificPart ?: return@SystemBroadcastRecvr

            curRow.reset()

            for((k, v) in hiddenApps) {
                if(v.packageName == uninstalledPackage) {
                    scope.launch {
                        dataStoreManager.remove(k)
                        forceCompose()
                    }
                    break
                }
            }
        }
    }

    val count = if(currentScreen == Screen.All) {
        apps.value.size
    } else {
        hiddenApps.size
    }.toString()

    Column {
        TopBar(refresh, setCurrentScreen, currentScreen, count, searchPat)

        when(currentScreen) {
            Screen.Settings -> {
                Text("Scroll Sensitivity: ${scrollState.sensitivity}", color = Color.White)
                Slider(
                    value = scrollState.sensitivity,
                    onValueChange = {
                        scrollState.sensitivity = it
                        scope.launch {
                            dataStoreManager.setSensitivity(it)
                        }
                    },
                    valueRange = 1f..4f,
                    modifier = Modifier
                        .width((LocalConfiguration.current.screenWidthDp - 20).dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
            else -> {
                if (isHiddenAppsReady) {
                    ListOfApps(currentScreen, apps.value,
                        hiddenApps, forceCompose,
                        searchPat, curRow,
                        scrollState, dataStoreManager,
                        filterHiddenApps, addToApps
                    )
                } else {
                    Text("Loading...", fontFamily = FontFamily.Monospace, color = Color(0xFF777777))
                }
            }
        }
    }

    if(currentScreen == Screen.All) {
        LockButton(lockScreen)
    }
}

enum class Screen {
    All,
    Hidden,
    Settings
}

@Composable
fun LockButton(lockScreen: () -> Unit) {
    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp
    val screenWidth = config.screenWidthDp.dp

    Box {
        Box(
            Modifier
                .offset(screenWidth - 70.dp, screenHeight - 70.dp)
                .size(35.dp)
                .background(Color.White)
                .clickable(onClick = lockScreen)
        ) {
            Icon(
                Icons.Sharp.Lock,
                "",
                Modifier
                    .align(Alignment.Center)
                    .size(22.dp),
                tint = Color.Black
            )
        }
    }
}
