package com.example.ntheta

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.sharp.List
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val TOP_BAR_HEIGHT = 30
val TOP_BAR_HEIGHT_DP = TOP_BAR_HEIGHT.dp

@Composable
fun TopBar(
    refresh: () -> Unit,
    setCurrentScreen: (Screen) -> Unit,
    currentScreen: Screen,
    count: String,
    searchPat: SearchPat
) {
    val ctx = LocalContext.current
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp
    val focusRequester = remember { FocusRequester() }

    if(searchPat.pattern != null) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(TOP_BAR_HEIGHT_DP)
        ) {
            BasicTextField(
                value = searchPat.pattern as String,
                decorationBox = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        it()
                    }
                },
                onValueChange = { searchPat.setPat(it) },
                modifier = Modifier
                    .fillMaxHeight()
                    .width((screenWidth - 40).dp)
                    .padding(horizontal = 4.dp)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                cursorBrush = SolidColor(Color.White),
                singleLine = true
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .requiredWidth(40.dp)
                    .clickable { searchPat.pattern = null }
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "", Modifier.align(Alignment.Center))
            }
        }
    } else {
        val iconModifier = Modifier
            .fillMaxHeight()
            .width(40.dp)
            .padding(horizontal = 4.dp)
            .background(Color(0xFF212121))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(TOP_BAR_HEIGHT_DP)
            ) {
                Box(iconModifier.clickable { searchPat.setPat("") }) {
                    Icon(Icons.Sharp.Search, contentDescription = "", Modifier.align(Alignment.Center), tint = Color.White)
                }

                if(currentScreen == Screen.Hidden) {
                    Box(iconModifier.clickable { setCurrentScreen(Screen.All) }) {
                        Icon(Icons.Sharp.List, contentDescription = "", Modifier.align(Alignment.Center), tint = Color.White)
                    }
                } else {
                    Box(iconModifier.clickable { setCurrentScreen(Screen.Hidden) }) {
                        Text("H", Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Box(iconModifier.clickable { refresh(); Toast.makeText(ctx, "refreshed", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Sharp.Refresh, contentDescription = "", Modifier.align(Alignment.Center), tint = Color.White)
                }

                Box(iconModifier.clickable { setCurrentScreen(
                    if(currentScreen == Screen.Settings) { Screen.All } else { Screen.Settings }
                ) }) {
                    Icon(Icons.Sharp.Settings, contentDescription = "", Modifier.align(Alignment.Center), tint = Color.White)
                }

                if(currentScreen != Screen.Settings) {
                    Text(
                        text = "(${count})",
                        Modifier.align(Alignment.CenterVertically).fillMaxWidth().padding(end = 10.dp),
                        textAlign = TextAlign.Left,
                        fontFamily = FontFamily.Monospace,
                        color = if(currentScreen == Screen.All) { Color.White } else { Color.Gray }
                    )
                }
            }
        }
    }

    LaunchedEffect(searchPat.pattern) {
        if(searchPat.pattern != null) {
            focusRequester.requestFocus()
        }
    }
}

