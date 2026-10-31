package com.nuvio.tv.ui.screens.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size as CoilSize
import com.nuvio.tv.core.perf.NawflixPerformance
import com.nuvio.tv.core.qr.QrCodeGenerator
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.ui.components.CinemaScrimTransformation
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PageColor = Color(0xFF08090C)
private val SideMargin = 56.dp
private val TileWidth = 158.dp
private val TileHeight = 92.dp
private val TileShape = RoundedCornerShape(10.dp)
private val PrimaryButtonShape = RoundedCornerShape(12.dp)
private const val RegeCineClubUrl = "https://letterboxd.com/regelegorila/list/rege-cine-club/"

/**
 * Home, organised like a streaming shelf screen: a hero billboard on top, titled shelves floating directly on the
 * page (no panels), the hero fading out and the page dimming as soon as focus goes down into the shelves.
 * Everything animated here is read in the draw phase, so scrolling never recomposes the hero.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CinemaHomeContent(
    state: HomeUiState,
    onDetail: (String, String, String) -> Unit,
    onResume: (ContinueWatchingItem) -> Unit,
    onImport: () -> Unit,
    onLibrary: (MetaPreview) -> Unit
) {
    val potato = NawflixPerformance.potato
    // Zen layout: keep the billboard stable while moving through a shelf.
    // This also avoids decoding and repainting a full-screen backdrop on every DPAD press.
    val hero = weeklyMovieSelection.firstOrNull()
    var showRegeCineClubQr by remember { mutableStateOf(false) }
    val catalogRows = remember(state.catalogRows) { state.catalogRows.filter { it.items.isNotEmpty() } }
    val rowLimit = if (potato) 12 else 24
    // The shelves list spans the whole screen (it scrolls over the hero), so geometric "down" from the hero buttons
    // cannot find it: point it explicitly at the first tile.
    val entryTile = remember { FocusRequester() }
    val heroEntry = remember { FocusRequester() }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // A synchronous scroll keeps the first shelf attached while the DPAD moves between it and the hero.
    val scrollToTop: () -> Unit = {
        scope.launch { listState.scrollToItem(0) }
    }
    val focusHero: () -> Unit = {
        scope.launch {
            listState.scrollToItem(0)
            heroEntry.requestFocus()
        }
    }
    val focusFirstShelf: () -> Unit = {
        scope.launch {
            listState.scrollToItem(0)
            entryTile.requestFocus()
        }
    }
    val scrolledPastHero by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24 }
    }
    val heroAlpha = if (scrolledPastHero) 0f else 1f

    // Lead with the V2 artwork shelf even when the existing installation has watch history.
    // Keep Continue Watching immediately after it so resume stays easy to reach.
    val appendContinueShelf: LazyListScope.() -> Unit = {
        if (state.continueWatchingItems.isNotEmpty()) {
            shelf(CONTINUE_KEY, "Reprendre la lecture") {
                itemsIndexed(state.continueWatchingItems.take(rowLimit)) { index, item ->
                    val entry = Modifier
                    when (item) {
                        is ContinueWatchingItem.InProgress -> CinemaTile(
                            title = item.progress.name,
                            url = item.episodeThumbnail ?: item.progress.backdrop ?: item.progress.poster,
                            progress = item.progress.progressPercentage,
                            onClick = { onResume(item) },
                            potato = potato,
                            modifier = entry
                        )
                        is ContinueWatchingItem.NextUp -> CinemaTile(
                            title = item.info.name,
                            url = item.info.thumbnail ?: item.info.backdrop ?: item.info.poster,
                            progress = null,
                            onClick = { onResume(item) },
                            subtitle = "Saison ${item.info.season} · Ép. ${item.info.episode}",
                            potato = potato,
                            modifier = entry
                        )
                    }
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val shelvesTop = maxHeight * .60f

        val context = LocalContext.current
        run {
            val url = hero?.backdropUrl
            // The contrast gradients are baked into the bitmap (see CinemaScrimTransformation), so the GPU draws
            // one opaque image. Potato: 720p and RGB565 to keep the heap and the fill rate low.
            val model = remember(url, potato) {
                ImageRequest.Builder(context)
                    .data(url)
                    .size(if (potato) CoilSize(1280, 720) else CoilSize(1920, 1080))
                    .transformations(CinemaScrimTransformation(if (potato) 1280 else 1920, rgb565 = potato))
                    .build()
            }
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Hero(
            hero = hero,
            isLoading = state.isLoading,
            onDetail = onDetail,
            onLibrary = onLibrary,
            onImport = onImport,
            entryTile = entryTile,
            heroEntry = heroEntry,
            onFocus = scrollToTop,
            onDown = focusFirstShelf,
            modifier = Modifier
                .align(Alignment.TopStart)
                .height(shelvesTop)
                .padding(start = SideMargin, end = 28.dp, top = 72.dp, bottom = 28.dp)
                .widthIn(max = 565.dp)
                .graphicsLayer { alpha = heroAlpha }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item(key = "hero-spacer") { Spacer(Modifier.height(shelvesTop)) }

            shelf(WEEKLY_KEY, "Film de la semaine") {
                itemsIndexed(weeklyMovieSelection, key = { _, item -> item.id }) { index, item ->
                    CinemaTile(
                        title = item.name,
                        url = item.landscapePoster,
                        progress = null,
                        onClick = { onDetail(item.id, item.apiType, item.sourceAddonBaseUrl.orEmpty()) },
                        onFocus = {},
                        potato = potato,
                        modifier = firstShelfModifier(index == 0, entryTile, focusHero)
                    )
                }
            }
            appendContinueShelf()
            catalogRows.forEach { row ->
                val shelfKey = "${row.addonId}/${row.catalogId}/${row.apiType}"
                shelf(shelfKey, shelfTitle(row.catalogName, row.apiType)) {
                    itemsIndexed(row.items.take(rowLimit), key = { _, it -> it.id }) { index, item ->
                        CinemaTile(
                            title = item.name,
                            url = item.landscapePoster ?: item.background ?: item.poster,
                            progress = null,
                            onClick = { onDetail(item.id, item.apiType, row.addonBaseUrl) },
                            onFocus = {},
                            potato = potato,
                            modifier = Modifier
                        )
                    }
                }
            }
            if (!state.isLoading && catalogRows.isEmpty()) {
                item(key = "empty-import") {
                    Column(Modifier.padding(horizontal = SideMargin)) {
                        Text("Votre prochaine séance", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Importez vos extensions pour composer votre cinéma.",
                            color = Color.White.copy(alpha = .7f),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        PrimaryAction("Importer mes extensions", onClick = onImport)
                    }
                }
            }
            shelf(REGE_KEY, "Rege Ciné Club") {
                itemsIndexed(regeCineClubSelection, key = { _, item -> item.id }) { _, item ->
                    CinemaTile(
                        title = item.name,
                        url = item.landscapePoster ?: item.poster,
                        progress = null,
                        onClick = { onDetail(item.id, item.apiType, item.sourceAddonBaseUrl.orEmpty()) },
                        potato = potato
                    )
                }
            }
            item(key = "rege-cine-club") {
                Column(Modifier.padding(horizontal = SideMargin)) {
                    Text(
                        "70 films sélectionnés par Regelegorila · Liste originale sur Letterboxd",
                        color = Color.White.copy(alpha = .7f),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryAction("Voir la liste sur téléphone") { showRegeCineClubQr = true }
                }
            }
        }
    }
    if (showRegeCineClubQr) {
        val qr = remember { QrCodeGenerator.generate(RegeCineClubUrl, 512).asImageBitmap() }
        Dialog(onDismissRequest = { showRegeCineClubQr = false }) {
            Column(
                modifier = Modifier.background(PageColor, RoundedCornerShape(20.dp)).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Rege Ciné Club", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Scannez pour voir la liste de Regelegorila", color = Color.White.copy(alpha = .7f), fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                Image(qr, contentDescription = "QR code vers le Rege Ciné Club", modifier = Modifier.size(220.dp))
                Spacer(Modifier.height(20.dp))
                PrimaryAction("Fermer") { showRegeCineClubQr = false }
            }
        }
    }
}

private const val CONTINUE_KEY = "continue-watching"
private const val WEEKLY_KEY = "weekly-movies"
private const val REGE_KEY = "rege-cine-club-films"

/** Route Up explicitly so a horizontally scrolled shelf always returns to a visible hero button. */
private fun firstShelfModifier(isFirstTile: Boolean, entry: FocusRequester, onUp: () -> Unit): Modifier {
    val up = Modifier.onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionUp) {
            onUp()
            true
        } else false
    }
    return if (isFirstTile) up.focusRequester(entry) else up
}

/** "Popular" alone is ambiguous when movies and series both have one: add the content type. */
private fun shelfTitle(name: String, apiType: String): String = when (apiType) {
    "movie" -> "$name · Films"
    "series" -> "$name · Séries"
    else -> name
}

private fun LazyListScope.shelf(key: String, title: String, content: LazyListScope.() -> Unit) {
    item(key = key) {
        Column {
            Text(
                title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = SideMargin, end = SideMargin, bottom = 9.dp)
            )
            // Focus ring and scale need room: the row is padded instead of clipped.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = SideMargin, vertical = 4.dp),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Hero(
    hero: MetaPreview?,
    isLoading: Boolean,
    onDetail: (String, String, String) -> Unit,
    onLibrary: (MetaPreview) -> Unit,
    onImport: () -> Unit,
    entryTile: FocusRequester,
    heroEntry: FocusRequester,
    onFocus: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val down = Modifier.focusProperties { this.down = entryTile }
        .onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) {
                onDown()
                true
            } else false
        }
        .onFocusChanged { if (it.isFocused) onFocus() }
    Column(modifier, verticalArrangement = Arrangement.Bottom) {
        if (!hero?.logo.isNullOrBlank()) {
            AsyncImage(
                model = hero.logo,
                contentDescription = hero.name,
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier.widthIn(max = 400.dp).heightIn(max = 64.dp)
            )
        } else {
            Text(
                text = hero?.name ?: "Votre cinéma commence ici",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                lineHeight = 42.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(10.dp))

        val metadata = listOfNotNull(
            hero?.releaseInfo,
            hero?.runtime,
            hero?.genres?.take(2)?.joinToString(", "),
            hero?.imdbRating?.let { "★ ${"%.1f".format(it)}" }
        ).filter(String::isNotBlank)
        if (metadata.isNotEmpty()) {
            Text(
                metadata.joinToString("  •  "),
                color = Color.White.copy(alpha = .74f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = hero?.description
                ?: if (isLoading) "Préparation de votre sélection…"
                else "Ajoutez vos extensions dans Réglages pour retrouver vos films et séries.",
            color = Color.White.copy(alpha = .86f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (hero != null) {
                PrimaryAction("▶  Regarder", down.focusRequester(heroEntry)) { onDetail(hero.id, hero.apiType, hero.sourceAddonBaseUrl.orEmpty()) }
                SecondaryAction("＋  Ma liste", down) { onLibrary(hero) }
            } else {
                PrimaryAction("Importer mes extensions", down, onImport)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PrimaryAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accentRing = NuvioTheme.colors.Secondary.copy(alpha = .4f)
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = Color.White,
            contentColor = Color(0xFF111216),
            focusedContainerColor = Color.White,
            focusedContentColor = Color(0xFF111216)
        ),
        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(3.dp, accentRing))),
        shape = ButtonDefaults.shape(shape = PrimaryButtonShape),
        scale = ButtonDefaults.scale(focusedScale = 1f)
    ) { Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SecondaryAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accentRing = NuvioTheme.colors.Secondary.copy(alpha = .4f)
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = .16f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color(0xFF111216)
        ),
        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(3.dp, accentRing))),
        shape = ButtonDefaults.shape(shape = PrimaryButtonShape),
        scale = ButtonDefaults.scale(focusedScale = 1f)
    ) { Text(label, fontSize = 15.sp) }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CinemaTile(
    title: String,
    url: String?,
    progress: Float?,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    subtitle: String? = null,
    potato: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val accent = NuvioTheme.colors.Secondary
    Column(Modifier.width(TileWidth)) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(TileHeight).onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus()
            },
            shape = CardDefaults.shape(TileShape),
            border = CardDefaults.border(focusedBorder = Border(BorderStroke(2.dp, accent), shape = TileShape)),
            scale = CardDefaults.scale(focusedScale = if (potato) 1f else 1.05f)
        ) {
            Box(Modifier.fillMaxSize().clip(TileShape)) {
                AsyncImage(url, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                if (progress != null) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(36.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .75f))))
                    )
                    Box(
                        Modifier.align(Alignment.BottomCenter).padding(horizontal = 9.dp, vertical = 8.dp)
                            .fillMaxWidth().height(3.dp)
                            .background(Color.White.copy(alpha = .35f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
                                .background(accent, RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
        Text(
            title,
            color = if (isFocused) Color.White else Color.White.copy(alpha = .62f),
            fontSize = 12.5.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, top = 7.dp)
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                color = Color.White.copy(alpha = .5f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

