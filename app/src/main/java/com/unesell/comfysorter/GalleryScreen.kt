package com.unesell.comfysorter

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.unesell.comfysorter.network.FolderItem
import com.unesell.comfysorter.network.ImageItem
import com.unesell.comfysorter.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun FolderItem.getSortDate(): Long { return 0L }
fun ImageItem.getSortDate(): Long { return this.mtime ?: 0L }

class GalleryViewModel : ViewModel() {
    var images by mutableStateOf<List<ImageItem>>(emptyList())
    var folders by mutableStateOf<List<FolderItem>>(emptyList())
    var isLoading by mutableStateOf(true)
    var pathStack = mutableStateListOf<String>("")

    var currentQuery by mutableStateOf("")
    var searchInput by mutableStateOf("")

    fun fetchContent(baseUrl: String, subpath: String, query: String = currentQuery) {
        viewModelScope.launch {
            images = emptyList()
            folders = emptyList()
            isLoading = true
            currentQuery = query
            try {
                val response = RetrofitClient.create(baseUrl).getList(subpath, query)
                images = response.images ?: emptyList()
                folders = response.folders ?: emptyList()
            } catch (e: Exception) {} finally { isLoading = false }
        }
    }

    fun openFolder(baseUrl: String, folderPath: String) {
        // Защита от дублирования: если уже на последней позиции — не добавляем
        if (pathStack.lastOrNull() != folderPath) {
            pathStack.add(folderPath)
        }
        currentQuery = ""
        searchInput = ""
        fetchContent(baseUrl, folderPath, "")
    }

    fun goBack(baseUrl: String): Boolean {
        if (pathStack.size > 1) {
            // ИСПРАВЛЕНО: Используем removeAt вместо removeLast
            pathStack.removeAt(pathStack.lastIndex)
            currentQuery = ""
            searchInput = ""
            fetchContent(baseUrl, pathStack.last(), "")
            return true
        }
        return false
    }

    fun navigateToCrumb(baseUrl: String, path: String) {
        val index = pathStack.indexOf(path)
        if (index != -1 && index < pathStack.size - 1) {
            while (pathStack.size > index + 1) {
                // ИСПРАВЛЕНО: Используем removeAt вместо removeLast
                pathStack.removeAt(pathStack.lastIndex)
            }
            currentQuery = ""
            searchInput = ""
            fetchContent(baseUrl, path, "")
        }
    }
}

fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

@Composable
fun AnimatedBlobsBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "blobs")
    val x1 by infiniteTransition.animateFloat(-100f, 200f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "x1")
    val y1 by infiniteTransition.animateFloat(-50f, 300f, infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse), label = "y1")
    val x2 by infiniteTransition.animateFloat(300f, -50f, infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse), label = "x2")
    val y2 by infiniteTransition.animateFloat(200f, 500f, infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), label = "y2")
    val x3 by infiniteTransition.animateFloat(-50f, 250f, infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), label = "x3")
    val y3 by infiniteTransition.animateFloat(600f, 100f, infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "y3")
    val x4 by infiniteTransition.animateFloat(200f, -100f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "x4")

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF111317))) {
        Box(modifier = Modifier.offset(x = x1.dp, y = y1.dp).size(350.dp).background(Brush.radialGradient(listOf(Color(0xFF2563EB).copy(alpha = 0.35f), Color.Transparent))))
        Box(modifier = Modifier.offset(x = x2.dp, y = y2.dp).size(400.dp).background(Brush.radialGradient(listOf(Color(0xFFD97706).copy(alpha = 0.25f), Color.Transparent))))
        Box(modifier = Modifier.offset(x = x3.dp, y = y3.dp).size(300.dp).background(Brush.radialGradient(listOf(Color(0xFF9333EA).copy(alpha = 0.3f), Color.Transparent))))
        Box(modifier = Modifier.offset(x = x4.dp, y = 400.dp).size(450.dp).background(Brush.radialGradient(listOf(Color(0xFF0D9488).copy(alpha = 0.25f), Color.Transparent))))
    }
}

@Composable
fun ChaoticPlanetsLoader() {
    val colors = listOf(Color(0xFFE94560), Color(0xFFF9A826), Color(0xFF00ADB5), Color(0xFF9333EA))
    val infiniteTransition = rememberInfiniteTransition(label = "planets")
    val paths = listOf(
        Pair(listOf(-15f, 15f), listOf(-10f, 10f)),
        Pair(listOf(15f, -15f), listOf(10f, -10f)),
        Pair(listOf(-10f, 10f), listOf(15f, -15f)),
        Pair(listOf(10f, -10f), listOf(-15f, 15f))
    )
    val planets = paths.mapIndexed { index, path ->
        val x by infiniteTransition.animateFloat(path.first[0], path.first[1], infiniteRepeatable(tween(1200 + index * 200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "x")
        val y by infiniteTransition.animateFloat(path.second[0], path.second[1], infiniteRepeatable(tween(1000 + index * 250, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
        Triple(Offset(x, y), colors[index % colors.size], 1f)
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
            planets.forEach { (offset, color, _) ->
                Box(modifier = Modifier.offset(x = offset.x.dp, y = offset.y.dp).size(10.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
fun GlassChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (selected) Color(0xFF2563EB).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
    val borderColor = if (selected) Color(0xFF3B82F6).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f)
    val contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.6f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = contentColor, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    baseUrl: String,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("comfy_gallery_prefs", Context.MODE_PRIVATE) }

    var showSettingsSheet by remember { mutableStateOf(false) }

    var gridSize by remember { mutableFloatStateOf(prefs.getFloat("gridSize", 150f)) }
    var sortOption by remember { mutableIntStateOf(prefs.getInt("sortOption", 0)) }
    var filterFolders by remember { mutableStateOf(prefs.getBoolean("filterFolders", true)) }
    var filterImages by remember { mutableStateOf(prefs.getBoolean("filterImages", true)) }
    var filterFavorites by remember { mutableStateOf(prefs.getBoolean("filterFavorites", false)) }

    LaunchedEffect(gridSize, sortOption, filterFolders, filterImages, filterFavorites) {
        prefs.edit()
            .putFloat("gridSize", gridSize)
            .putInt("sortOption", sortOption)
            .putBoolean("filterFolders", filterFolders)
            .putBoolean("filterImages", filterImages)
            .putBoolean("filterFavorites", filterFavorites)
            .apply()
    }

    LaunchedEffect(viewModel.searchInput) {
        delay(500)
        if (viewModel.searchInput != viewModel.currentQuery && !viewModel.isLoading) {
            viewModel.fetchContent(baseUrl, viewModel.pathStack.lastOrNull() ?: "", viewModel.searchInput)
        }
    }

    LaunchedEffect(baseUrl) {
        // Гарантированно сбрасываем стек при каждом открытии экрана
        viewModel.pathStack.clear()
        viewModel.pathStack.add("")
        viewModel.currentQuery = ""
        viewModel.searchInput = ""
        viewModel.fetchContent(baseUrl, "")
    }

    BackHandler { if (!viewModel.goBack(baseUrl)) onBack() }

    val listState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val scrollProgress by remember { derivedStateOf { ((listState.firstVisibleItemScrollOffset + (listState.firstVisibleItemIndex * 200)) / 400f).coerceIn(0f, 1f) } }

    val headerScale = 1f - (scrollProgress * 0.15f)
    val headerAlpha = 1f - (scrollProgress * 0.4f)
    val headerOffsetY = -(scrollProgress * 40f)
    val frameTopPadding = (240f - (scrollProgress * 80f)).dp

    val sortedFolders = viewModel.folders.let { list ->
        if (!filterFolders) emptyList()
        else when (sortOption) {
            0 -> list.sortedBy { it.name?.lowercase() }
            1 -> list.sortedByDescending { it.name?.lowercase() }
            else -> list.sortedBy { it.name?.lowercase() }
        }
    }

    val sortedImages = viewModel.images.let { list ->
        val filtered = if (filterFavorites) list.filter { it.isFavorite } else list
        if (!filterImages) emptyList()
        else when (sortOption) {
            0 -> filtered.sortedBy { it.name?.lowercase() }
            1 -> filtered.sortedByDescending { it.name?.lowercase() }
            2 -> filtered.sortedByDescending { it.getSortDate() }
            else -> filtered.sortedBy { it.getSortDate() }
        }
    }

    // Расчёт количества колонок на основе размера карточки и ширины экрана
    val configuration = LocalConfiguration.current
    val screenWidthPx = configuration.screenWidthDp.dp
    val density = LocalDensity.current
    val columnCount = with(density) {
        val spacingTotal = 16.dp * 2 + 16.dp // start + end + между колонками
        val available = screenWidthPx.toPx() - spacingTotal.toPx()
        val cellSizePx = gridSize.dp.toPx()
        maxOf(1, (available / cellSizePx).toInt())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBlobsBackground()

        GalleryHeader(
            modifier = Modifier.align(Alignment.TopStart).graphicsLayer { scaleX = headerScale; scaleY = headerScale; alpha = headerAlpha; translationY = headerOffsetY },
            pathStack = viewModel.pathStack, folderCount = sortedFolders.size, imageCount = sortedImages.size,
            onHomeClick = { viewModel.pathStack.clear(); viewModel.pathStack.add(""); viewModel.searchInput = ""; viewModel.fetchContent(baseUrl, "") },
            onCrumbClick = { path -> viewModel.searchInput = ""; viewModel.navigateToCrumb(baseUrl, path) }
        )

        Box(
            modifier = Modifier.fillMaxSize().padding(top = frameTopPadding).clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color(0xFF16181D).copy(alpha = 0.85f)).fadingEdge(Brush.verticalGradient(0f to Color.Transparent, 0.06f to Color.Black, 1f to Color.Black))
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount), state = listState,
                contentPadding = PaddingValues(top = 24.dp, bottom = 140.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedFolders) { folder -> FolderCard(folder) { viewModel.searchInput = ""; folder.relPath?.let { viewModel.openFolder(baseUrl, it) } } }
                items(sortedImages) { item -> ImageCard(item, baseUrl, onClick = { item.relPath?.let { onImageClick(it) } }) }
            }
        }

        FloatingControlsLayout(
            modifier = Modifier.align(Alignment.BottomCenter),
            searchQuery = viewModel.searchInput, onSearchQueryChange = { viewModel.searchInput = it },
            onScrollTop = { coroutineScope.launch { listState.animateScrollToItem(0) } },
            onSettingsClick = { showSettingsSheet = true }
        )

        if (viewModel.isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ChaoticPlanetsLoader() }

        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = Color(0xFF111317).copy(alpha = 0.95f), scrimColor = Color.Black.copy(alpha = 0.7f),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 48.dp).fillMaxWidth()) {
                    Text("Настройки отображения", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(32.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Размер карточек", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("${gridSize.toInt()} px", color = Color(0xFF3B82F6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(value = gridSize, onValueChange = { gridSize = it }, valueRange = 100f..300f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF2563EB), inactiveTrackColor = Color.White.copy(alpha = 0.1f)))
                    Spacer(Modifier.height(24.dp))

                    Text("Сортировка", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassChip(text = "Имя (А-Я)", icon = Icons.Filled.SortByAlpha, selected = sortOption == 0, onClick = { sortOption = 0 }, modifier = Modifier.weight(1f))
                            GlassChip(text = "Имя (Я-А)", icon = Icons.Filled.SortByAlpha, selected = sortOption == 1, onClick = { sortOption = 1 }, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassChip(text = "Новые", icon = Icons.Filled.AccessTime, selected = sortOption == 2, onClick = { sortOption = 2 }, modifier = Modifier.weight(1f))
                            GlassChip(text = "Старые", icon = Icons.Filled.History, selected = sortOption == 3, onClick = { sortOption = 3 }, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(32.dp))

                    Text("Показывать", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassChip(text = "Папки", icon = Icons.Filled.Folder, selected = filterFolders, onClick = { filterFolders = !filterFolders }, modifier = Modifier.weight(1f))
                        GlassChip(text = "Файлы", icon = Icons.Filled.Image, selected = filterImages, onClick = { filterImages = !filterImages }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassChip(text = "Избранное", icon = Icons.Filled.Favorite, selected = filterFavorites, onClick = { filterFavorites = !filterFavorites }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryHeader(modifier: Modifier, pathStack: List<String>, folderCount: Int, imageCount: Int, onHomeClick: () -> Unit, onCrumbClick: (String) -> Unit) {
    val isRoot = pathStack.size <= 1
    val currentPath = if (isRoot) "Галерея" else pathStack.last().substringAfterLast("/")

    val scrollState = rememberScrollState()
    LaunchedEffect(pathStack.size) { scrollState.animateScrollTo(scrollState.maxValue) }

    Column(modifier = modifier.padding(top = 64.dp, start = 24.dp, end = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(scrollState)) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White).clickable { onHomeClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Home, contentDescription = "Home", tint = Color.Black, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp))
                Text("Главная", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (!isRoot) {
                val pathsToDisplay = pathStack.drop(1)
                pathsToDisplay.forEachIndexed { index, path ->
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ChevronRight, contentDescription = ">", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    val isLast = index == pathsToDisplay.size - 1
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isLast) Color(0xFF28242E).copy(alpha = 0.9f) else Color(0xFF28242E).copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).clickable(enabled = !isLast) { onCrumbClick(path) }.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text(path.substringAfterLast("/"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(text = currentPath, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.01.sp)
        Spacer(Modifier.height(6.dp))
        Text(text = "$folderCount папок • $imageCount файлов", color = Color(0xFF9A9A9A), fontSize = 15.sp, letterSpacing = 0.01.sp)
    }
}

@Composable
fun FloatingControlsLayout(modifier: Modifier, searchQuery: String, onSearchQueryChange: (String) -> Unit, onScrollTop: () -> Unit, onSettingsClick: () -> Unit) {
    var isSearchActive by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    val focusManager = LocalFocusManager.current
    val targetBottom = if (isSearchActive) 64.dp else 32.dp
    val bottomPadding by animateDpAsState(targetValue = targetBottom, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "padding")

    Box(modifier = modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = bottomPadding)) {
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(end = 80.dp).fillMaxWidth().height(60.dp).clip(RoundedCornerShape(30.dp)).background(Color(0xFF20232A).copy(alpha = 0.97f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp)).clickable { isSearchActive = true }.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
            if (isSearchActive) {
                BasicTextField(
                    value = searchQuery, onValueChange = onSearchQueryChange, modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.White, fontSize = 17.sp), singleLine = true, cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
                IconButton(onClick = { isSearchActive = false; onSearchQueryChange(""); focusManager.clearFocus() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            } else { Text("Поиск...", color = Color.White.copy(alpha = 0.4f), fontSize = 17.sp) }
        }

        Column(modifier = Modifier.align(Alignment.BottomEnd), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF20232A).copy(alpha = 0.97f)).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Filled.Tune, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(26.dp)) }
            IconButton(onClick = onScrollTop, modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF20232A).copy(alpha = 0.97f)).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Up", tint = Color.White, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
fun FolderCard(folder: FolderItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xFF20232A)).border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(48.dp)); Spacer(Modifier.height(12.dp))
            Text(text = folder.name ?: "Папка", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (folder.count != null) { Spacer(Modifier.height(4.dp)); Text("${folder.count} файлов", color = Color(0xFF9A9A9A), fontSize = 12.sp) }
        }
    }
}

// Теперь ImageCard тоже использует SubcomposeAsyncImage, и жестко закреплены ключи кэша, чтобы галерея НЕ моргала
@Composable
fun ImageCard(item: ImageItem, baseUrl: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val thumbPath = item.thumb ?: ""
    val fullUrl = if (thumbPath.startsWith("http")) thumbPath else "${baseUrl.trimEnd('/')}$thumbPath"

    val imageRequest = ImageRequest.Builder(context)
        .data(fullUrl)
        .addHeader("ngrok-skip-browser-warning", "true")
        .memoryCacheKey(fullUrl) // ОЧЕНЬ ВАЖНО: Исключает моргание и перезагрузку
        .diskCacheKey(fullUrl)   // ОЧЕНЬ ВАЖНО: Исключает моргание и перезагрузку
        .crossfade(true)
        .build()

    Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xFF20232A)).clickable { onClick() }) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF28242E)))
            }
        )
        // Затемнение снизу с названием
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to Color.Black.copy(alpha = 0.6f), 1f to Color.Black.copy(alpha = 0.95f))).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(text = item.name ?: "Unknown", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Индикатор избранного
        if (item.isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE94560).copy(0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}