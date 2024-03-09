package com.example.ntheta

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBar(
    refresh: () -> Unit,
    setCurrentListType: (ListType) -> Unit,
    currentListType: ListType,
    allCount: Int,
    hiddenCount: Int,
    searchPat: SearchPat
) {
    val ctx = LocalContext.current
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp
    val focusRequester = remember { FocusRequester() }
    val modifier = Modifier
        .fillMaxHeight()
        .padding(horizontal = 4.dp)
        .width(70.dp)
        .background(Color(0xFF212121))

    if(searchPat.pattern != null) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(40.dp)
                        .padding(horizontal = 4.dp)
                        .background(Color(0xFF212121))
                        .clickable { searchPat.setPat("") }
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = "", Modifier.align(Alignment.Center))
                }

                Box(modifier.clickable { setCurrentListType(ListType.All) }) {
                    Text("all", Modifier.align(Alignment.Center), fontFamily = FontFamily.Monospace)
                }

                Box(modifier.clickable {
                    refresh()
                    Toast.makeText(ctx, "refreshed", Toast.LENGTH_SHORT).show()
                }) {
                    Text("refresh", Modifier.align(Alignment.Center), fontFamily = FontFamily.Monospace)
                }

                Box(modifier.clickable { setCurrentListType(ListType.Hidden) }) {
                    Text("hidden", Modifier.align(Alignment.Center), fontFamily = FontFamily.Monospace)
                }

                val count = when(currentListType) {
                    ListType.All -> (allCount - hiddenCount).toString()
                    ListType.Hidden -> hiddenCount.toString()
                }

                Text("(${count})",
                    Modifier
                        .align(Alignment.CenterVertically)
                        .fillMaxWidth()
                        .padding(end = 10.dp), textAlign = TextAlign.Left, fontFamily = FontFamily.Monospace)
            }
        }
    }

    LaunchedEffect(searchPat.pattern) {
        if(searchPat.pattern != null) {
            focusRequester.requestFocus()
        }
    }
}

