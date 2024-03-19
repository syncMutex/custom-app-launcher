package com.example.ntheta

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun ListOfApps(
    screen: Screen,
    apps: List<AppInfo>,
    hiddenApps: Map<String, AppInfo>,
    forceCompose: () -> Unit,
    searchPat: SearchPat,
    curRow: CurInteractRow,
    scrollState: CustomScrollManager,
    dataStoreManager: DataStoreManager,
    filterHiddenApps: () -> Unit,
    addToApps: (AppInfo) -> Unit
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val pm = LocalContext.current.packageManager
    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp

    val hide = fun(app: AppInfo) {
        if (hiddenApps.containsKey(app.id)) {
            addToApps(app)
            scope.launch {
                dataStoreManager.remove(app.id)
            }
        } else {
            scope.launch {
                dataStoreManager.add(app.id, app)
                filterHiddenApps()
            }
        }
        forceCompose()
    }

    val openApp = fun(packageName: String) {
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            ContextCompat.startActivity(ctx, intent, null)
        }
        searchPat.reset()
        curRow.reset()
    }

    var maxScrollCount by remember { mutableIntStateOf(0) }
    var maxScroll by remember { mutableFloatStateOf(0f) }
    val pad = remember { ((screenHeight - TOP_BAR_HEIGHT) % BOX_SIZE.toInt()).dp }

    val calcAndSetMaxScroll = fun() {
        maxScrollCount = maxOf(((screenHeight - TOP_BAR_HEIGHT) / BOX_SIZE).toInt() - 1, 0)
        maxScroll = when (screen) {
            Screen.All -> ((apps.size - maxScrollCount - 1) * BOX_SIZE)
            Screen.Hidden -> ((hiddenApps.size - maxScrollCount - 1) * BOX_SIZE)
            else -> maxScroll
        }
    }

    LaunchedEffect(apps, hiddenApps, screen) {
        calcAndSetMaxScroll()
    }

    val scrollModifier = Modifier
        .fillMaxWidth()
        .padding(PaddingValues(bottom = pad))
        .scrollable(
            orientation = Orientation.Vertical,
            enabled = true,
            state = rememberScrollableState { dy ->
                scrollState.offset -= (dy * scrollState.sensitivity)

                if (scrollState.offset < 0) {
                    scrollState.offset = 0f
                }

                scrollState.offset = minOf(scrollState.offset, maxScroll)

                dy
            },
        )

    val from = maxOf((scrollState.offset / BOX_SIZE).toInt(), 0)

    Box {
        val isAtEnd = when(screen) {
            Screen.All ->
                displayList(apps, from, maxScrollCount, searchPat, hide, openApp, curRow, scrollModifier)
            Screen.Hidden ->
                displayList(hiddenApps.values.toList(), from, maxScrollCount, searchPat, hide, openApp, curRow, scrollModifier)
            else -> false
        }

        if(isAtEnd) {
            Text("```", Modifier.align(Alignment.BottomStart), color = Color.Gray)
        }
    }
}

@Composable
fun displayList(
    list: List<AppInfo>,
    from: Int,
    max: Int,
    searchPat: SearchPat,
    hide: (AppInfo) -> Unit,
    openApp: (String) -> Unit,
    curRow: CurInteractRow,
    scrollModifier: Modifier
): Boolean {
    val maxItemIdx: Int = list.size - 1
    val displayableRange = from..(minOf(from + max, maxItemIdx))

    Column(scrollModifier) {
        for(i in displayableRange) {
            val it = list[i]
            if(searchPat.containsMatchIn(it.label)) {
                App(it, hide, openApp, curRow)
            }
        }
    }

    return displayableRange.last == maxItemIdx
}

class CurInteractRow {
    var id by mutableStateOf("")
    var offsetX by mutableFloatStateOf(0f)

    fun reset() {
        id = ""
        offsetX = 0f
    }
}
