package com.example.ntheta

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

const val BOX_SIZE = 40f
const val BOX_SIZEx2 = BOX_SIZE * 2
const val BOX_SIZEx4 = BOX_SIZE * 4
const val BOX_SIZEx6 = BOX_SIZE * 6

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App(
    app: AppInfo,
    hide: (AppInfo) -> Unit,
    openApp: (String) -> Unit,
    curRow: CurInteractRow,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current

    val openInfo = fun () {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setData(Uri.parse("package:" + app.packageName))
        ContextCompat.startActivity(ctx, intent, null)
    }

    val uninstall = fun () {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setData(Uri.parse("package:${app.packageName}"))
        ContextCompat.startActivity(ctx, intent, null)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BOX_SIZE.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row {
                Box(Modifier.size(40.dp).background(Color(0xff151515)).clickable(onClick = openInfo)) {
                    Icon(Icons.Sharp.Info, "", Modifier.align(Alignment.Center), tint = Color.White)
                }
                Box(Modifier.size(40.dp).background(Color(0xff151515)).clickable(onClick = uninstall)) {
                    Icon(Icons.Sharp.Delete, "", Modifier.align(Alignment.Center), tint = Color(0xffe34646))
                }
                Box(Modifier.size(40.dp).background(Color(0xff151515)).clickable { hide(app) }) {
                    Text("H", Modifier.align(Alignment.Center), color = Color(0xffAAAAAA), fontWeight = FontWeight.Bold)
                }
            }

            Box(
                Modifier
                    .offset { IntOffset(
                        if(curRow.id == app.id) {
                            curRow.offsetX.roundToInt()
                        } else {
                            0
                        }, 0)
                    }
                    .background(Color.Black)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { dx ->
                            curRow.offsetX += dx

                            if (curRow.offsetX > BOX_SIZEx6) {
                                curRow.offsetX = BOX_SIZEx6
                            }

                            if (curRow.offsetX < 0) {
                                curRow.offsetX = 0f
                            }
                        },
                        onDragStarted = {
                            if(curRow.id != app.id) {
                                curRow.offsetX = 0f
                                curRow.id = app.id
                            }
                        },
                        onDragStopped = {
                            curRow.offsetX = when {
                                curRow.offsetX < 0 -> 0f
                                curRow.offsetX <= BOX_SIZE -> 0f
                                curRow.offsetX <= BOX_SIZEx2 -> BOX_SIZEx2
                                curRow.offsetX <= BOX_SIZEx2 + BOX_SIZE -> BOX_SIZEx2
                                curRow.offsetX <= BOX_SIZEx4 -> BOX_SIZEx4
                                curRow.offsetX < BOX_SIZEx4 + BOX_SIZE -> BOX_SIZEx4
                                curRow.offsetX < BOX_SIZEx6 -> BOX_SIZEx6
                                curRow.offsetX > BOX_SIZEx6 -> BOX_SIZEx6
                                else -> curRow.offsetX
                            }
                        }
                    )
                    .combinedClickable(onClick = { openApp(app.packageName) }, onLongClick = {
                        Toast
                            .makeText(ctx, app.packageName, Toast.LENGTH_SHORT)
                            .show()
                    })
            ) {
                Text(
                    app.label,
                    fontFamily = FontFamily.Monospace,
                    color = textColor,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .align(Alignment.CenterStart)
                )
            }
        }
    }
}
