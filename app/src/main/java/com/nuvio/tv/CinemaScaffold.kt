package com.nuvio.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.tv.material3.*
import com.nuvio.tv.core.perf.NawflixPerformance
import com.nuvio.tv.core.sound.UiSounds
import com.nuvio.tv.ui.navigation.NuvioNavHost
import com.nuvio.tv.ui.theme.NuvioTheme

private val BarShape = RoundedCornerShape(40.dp)
private val TabShape = RoundedCornerShape(30.dp)

/** Native TV navigation: persistent on root screens, hidden during playback/details. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CinemaScaffold(
    navController: NavHostController,
    startDestination: String,
    currentRoute: String?,
    onExit: () -> Unit
) {
    val tabs = remember {
        listOf("Accueil" to "home", "Bibliothèque" to "library", "Recherche" to "search", "Découvrir" to "discover", "Réglage" to "settings")
    }
    val potato = NawflixPerformance.potato
    val accent = NuvioTheme.colors.Secondary
    val routeBase = currentRoute?.substringBefore("?")?.substringBefore("/")
    val root = tabs.any { it.second == routeBase }
    val first = remember { FocusRequester() }
    val content = remember { FocusRequester() }
    BackHandler(enabled = root) {
        if (routeBase != "home") navController.navigate("home") { popUpTo("home"); launchSingleTop = true }
        else onExit()
    }
    // UI sounds stay silent while the player owns the screen.
    LaunchedEffect(currentRoute) { UiSounds.suspended = currentRoute?.startsWith("player/") == true }
    DisposableEffect(Unit) { onDispose { UiSounds.suspended = false; UiSounds.inTopBar = false } }

    // No extra background here: the root Surface already paints the page colour (one less full-screen layer).
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalSidebarExpanded provides false, LocalContentFocusRequester provides content) {
            Box(Modifier.fillMaxSize()
                .padding(top = if (root && routeBase != "home") 66.dp else 0.dp)
                .focusRequester(content)
                .focusGroup()) {
                NuvioNavHost(navController = navController, startDestination = startDestination, hideBuiltInHeaders = true)
            }
        }
        if (root) {
            // Logo on a transparent background: the N mark and the wordmark are drawn straight over the content.
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 56.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.nawflix_mark),
                    contentDescription = "Nawflix",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(32.dp).aspectRatio(175f / 196f)
                )
                Text("NAWFLIX", fontSize = 17.sp, color = Color(0xFFE9EAEE),
                    fontWeight = FontWeight.Light, letterSpacing = 3.sp)
            }
            Row(Modifier.align(Alignment.TopCenter).padding(top = 14.dp)
                .onFocusChanged { UiSounds.inTopBar = it.hasFocus }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                        runCatching { content.requestFocus() }.getOrDefault(false)
                    } else false
                },
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                tabs.forEachIndexed { index, (title, route) ->
                    val selected = routeBase == route
                    Button(onClick = {
                        navController.navigate(route) { popUpTo("home"); launchSingleTop = true }
                    }, modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            focusedContainerColor = accent.copy(alpha = .18f),
                            focusedContentColor = Color.White),
                        shape = ButtonDefaults.shape(shape = TabShape),
                        scale = ButtonDefaults.scale(scale = 1f, focusedScale = if (potato) 1f else 1.04f)) {
                        // The selected section follows the accent chosen in Appearance.
                        Box(Modifier.drawBehind {
                            if (selected) drawRect(
                                accent,
                                topLeft = Offset(size.width * .3f, size.height + 3.dp.toPx()),
                                size = Size(size.width * .4f, 2.dp.toPx())
                            )
                        }) { Text(title, fontSize = 13.sp) }
                    }
                }
            }
            LaunchedEffect(Unit) { first.requestFocus() }
        }
    }
}
