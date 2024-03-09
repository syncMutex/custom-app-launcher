package com.example.ntheta

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

lateinit var PACKAGE_NAME: String

val Context.dataStore: DataStore<HiddenAppMap> by dataStore("appInfo.pb", serializer = HiddenAppMapSerializer)

const val ONE_TAP_LOCKSCREEN = "com.coloros.onekeylockscreen"

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dm = getSystemService(DevicePolicyManager::class.java)
        val compName = ComponentName(this, AdminReceiver::class.java)

        PACKAGE_NAME = applicationContext.packageName

        if(dm != null && !dm.isAdminActive(compName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra("android.app.extra.DEVICE_ADMIN", compName);
            intent.putExtra("android.app.extra.ADD_EXPLANATION", "");
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

        val dataStoreHiddenAppMapManager = DataStoreHiddenAppMapManager(this)

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

@Composable
fun MainLayout(lockScreen: () -> Unit, dataStoreManager: DataStoreHiddenAppMapManager) {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val scope = rememberCoroutineScope()
    // don't make it mutableListOf because it won't update the ui because i am reassigning the array itself
    val apps = remember { mutableStateOf(getInstalledApps(pm)) }
    var hiddenApps by remember { mutableStateOf(mapOf<String, AppInfo>()) }
    val fl = dataStoreManager.getHiddenList()
    val forceComposeToggle = remember { mutableStateOf(true) }
    var currentListType by remember { mutableStateOf(ListType.All) }
    val searchPat = remember { SearchPat(null) }
    val curRow = remember { CurInteractRow() }
    val scrollState = rememberScrollState()

    val forceCompose = fun () {
        forceComposeToggle.value = !forceComposeToggle.value
    }
    LaunchedEffect(forceComposeToggle.value) {}

    var isHiddenAppsReady by remember { mutableStateOf(false) }
    LaunchedEffect(fl) {
        fl.collect() {
            hiddenApps = it.hiddenMapMap
            isHiddenAppsReady = true
        }
    }

    val refresh = fun () {
        apps.value = getInstalledApps(pm)
    }

    val setCurrentListType = fun(type: ListType) {
        currentListType = type
        curRow.reset()
        searchPat.reset()
        scope.launch {
            scrollState.scrollTo(0)
        }
    }

    SystemBroadcastRecvr(systemAction = Intent.ACTION_SCREEN_OFF) {intent ->
        if(intent?.action == Intent.ACTION_SCREEN_OFF) {
            curRow.reset()
            searchPat.reset()
            scope.launch {
                scrollState.scrollTo(0)
            }
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

    Column {
        TopBar(refresh, setCurrentListType, currentListType, apps.value.size, hiddenApps.size, searchPat)
        if(isHiddenAppsReady) {
            ListOfApps(
                currentListType,
                apps.value,
                hiddenApps,
                forceCompose,
                searchPat,
                curRow,
                scrollState,
                dataStoreManager
            )
        } else {
            Text("Loading...", fontFamily = FontFamily.Monospace, color = Color(0xFF777777))
        }
    }

    LockButton(lockScreen)
}

enum class ListType {
    All,
    Hidden
}

class CurInteractRow {
    var id by mutableStateOf("")
    var offsetX by mutableFloatStateOf(0f)

    fun reset() {
        id = ""
        offsetX = 0f
    }
}

@Composable
fun ListOfApps(
    listType: ListType,
    apps: List<AppInfo>,
    hiddenApps: Map<String, AppInfo>,
    forceCompose: () -> Unit,
    searchPat: SearchPat,
    curRow: CurInteractRow,
    scrollState: ScrollState,
    dataStoreManager: DataStoreHiddenAppMapManager
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val pm = LocalContext.current.packageManager

    val hide = fun(app: AppInfo) {
        if(hiddenApps.containsKey(app.id)) {
            scope.launch {
                dataStoreManager.remove(app.id)
            }
        } else {
            scope.launch {
                dataStoreManager.add(app.id, app)
            }
        }
        forceCompose()
    }

    val openApp = fun (packageName: String) {
        val intent = pm.getLaunchIntentForPackage(packageName)
        if(intent != null) {
            ContextCompat.startActivity(ctx, intent, null)
        }
        searchPat.reset()
        curRow.reset()
    }

    when(listType) {
        ListType.All -> Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            for(it in apps) {
                if(!hiddenApps.containsKey(it.id) && searchPat.containsMatchIn(it.label)) {
                    App(it, hide, openApp, curRow)
                }
            }
        }

        ListType.Hidden -> Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            for(it in hiddenApps.values) {
                if(searchPat.containsMatchIn(it.label)) {
                    App(it, hide, openApp, curRow)
                }
            }
        }
    }
}

@Composable
fun LockButton(lockScreen: () -> Unit) {
    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp
    val screenWidth = config.screenWidthDp.dp

    Box {
        Box(
            Modifier
                .offset(screenWidth - 60.dp, screenHeight - 50.dp)
                .size(30.dp)
                .background(Color.White)
                .clickable(onClick = lockScreen)
        ) {
            Icon(
                Icons.Rounded.Lock,
                "",
                Modifier
                    .align(Alignment.Center)
                    .size(20.dp),
                tint = Color.Black
            )
        }
    }
}
