package com.unesell.comfysorter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.unesell.comfysorter.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// DATA / LORA PARSER
// ─────────────────────────────────────────────────────────────────────────────
data class LoraData(val name: String, val weightClip: Float, val weightModel: Float)

fun parseLorasSafely(value: Any?): List<LoraData> {
    val result = mutableListOf<LoraData>()
    if (value == null) return result

    if (value is List<*>) {
        for (item in value) {
            if (item is Map<*, *>) {
                result.add(
                    LoraData(
                        name = item["name"]?.toString() ?: "Unknown",
                        weightClip = item["weight_clip"]?.toString()?.toFloatOrNull()
                            ?: item["strength_clip"]?.toString()?.toFloatOrNull() ?: 1f,
                        weightModel = item["weight_model"]?.toString()?.toFloatOrNull()
                            ?: item["strength_model"]?.toString()?.toFloatOrNull() ?: 1f
                    )
                )
            }
        }
        return result
    }

    val raw = value.toString()
    val eqFormat = Regex("""\{name=([^,}]+),\s*(?:strength_clip|weight_clip)=([\d.]+),\s*(?:strength_model|weight_model)=([\d.]+)\}""")
    val eqMatches = eqFormat.findAll(raw)
    if (eqMatches.count() > 0) {
        eqMatches.forEach { m ->
            result.add(LoraData(m.groupValues[1].trim(), m.groupValues[2].toFloatOrNull() ?: 1f, m.groupValues[3].toFloatOrNull() ?: 1f))
        }
        return result
    }

    val jsonFormat = Regex("""['"]?name['"]?\s*[:=]\s*['"]?([^'",}\]]+)['"]?.*?(?:weight_clip|strength_clip)['"]?\s*[:=]\s*([\d.]+).*?(?:weight_model|strength_model)['"]?\s*[:=]\s*([\d.]+)""")
    jsonFormat.findAll(raw).forEach { m ->
        result.add(LoraData(m.groupValues[1].trim(), m.groupValues[2].toFloatOrNull() ?: 1f, m.groupValues[3].toFloatOrNull() ?: 1f))
    }
    return result
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────
class ViewerViewModel : ViewModel() {
    var images by mutableStateOf<List<ImageItem>>(emptyList())
    var currentDetails by mutableStateOf<ImageDetails?>(null)
    var isLoadingDetails by mutableStateOf(false)
    var initialIndex by mutableIntStateOf(0)
    private var isInitialized = false

    fun init(baseUrl: String, initialRelPath: String) {
        if (isInitialized) return
        isInitialized = true
        val subpath = if (initialRelPath.contains("/")) initialRelPath.substringBeforeLast("/") else ""
        viewModelScope.launch {
            try {
                val response = RetrofitClient.create(baseUrl).getList(subpath = subpath, query = "")
                val loaded = response.images ?: emptyList()
                images = loaded
                initialIndex = loaded.indexOfFirst { it.relPath == initialRelPath }.coerceAtLeast(0)
            } catch (_: Exception) {}
        }
    }

    fun fetchDetails(baseUrl: String, relPath: String) {
        viewModelScope.launch {
            isLoadingDetails = true
            currentDetails = null
            try {
                currentDetails = RetrofitClient.create(baseUrl).getImageDetails(relPath)
            } catch (_: Exception) {} finally {
                isLoadingDetails = false
            }
        }
    }

    fun toggleFavorite(baseUrl: String, relPath: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.create(baseUrl).toggleFavorite(FavoriteRequest(relPath))
                // Обновляем isFavorite в текущих деталях
                currentDetails = currentDetails?.copy(isFavorite = response.isFavorite)
                // Обновляем в списке изображений
                images = images.map { img ->
                    if (img.relPath == relPath) img.copy(isFavorite = response.isFavorite)
                    else img
                }
            } catch (_: Exception) {
                // В случае ошибки просто инвертируем локально
                currentDetails = currentDetails?.copy(isFavorite = !currentFavorite)
                images = images.map { img ->
                    if (img.relPath == relPath) img.copy(isFavorite = !currentFavorite)
                    else img
                }
            }
        }
    }

    fun deleteImage(baseUrl: String, relPath: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val client = RetrofitClient.getOkHttpClient()
                val url = "${baseUrl.trimEnd('/')}/api/delete"
                val jsonBody = """{"relpath":"$relPath"}"""
                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    onSuccess()
                }
            } catch (_: Exception) {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER
// ─────────────────────────────────────────────────────────────────────────────
fun buildFullUrl(baseUrl: String, path: String?): String {
    if (path == null) return ""
    return if (path.startsWith("http")) path else "${baseUrl.trimEnd('/')}$path"
}

// ─────────────────────────────────────────────────────────────────────────────
// ЗАГРУЗКА С ПРЕВЬЮ — показываем thumb пока грузится full
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ImageLoaderWithPreview(
    fullModel: Any,
    thumbModel: Any?,
    imageUrl: String,
    onScaleChange: (Float) -> Unit,
    onSingleTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isGesturing by remember { mutableStateOf(false) }

    val animSc by animateFloatAsState(scale, spring(stiffness = Spring.StiffnessMediumLow), label = "sc")
    val animOX by animateFloatAsState(offsetX, spring(stiffness = Spring.StiffnessMediumLow), label = "ox")
    val animOY by animateFloatAsState(offsetY, spring(stiffness = Spring.StiffnessMediumLow), label = "oy")

    val rawProgress by remember(imageUrl) { derivedStateOf { ProgressTracker.progressMap[imageUrl] ?: 0f } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        isGesturing = false
                        if (scale > 1f) { scale = 1f; offsetX = 0f; offsetY = 0f }
                        else scale = 2.5f
                        onScaleChange(scale)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    isGesturing = true
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    onScaleChange(scale)
                    if (scale > 1f) {
                        val maxX = (size.width * (scale - 1)) / 2f
                        val maxY = (size.height * (scale - 1)) / 2f
                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                    } else { offsetX = 0f; offsetY = 0f }
                }
            }
    ) {
        SubcomposeAsyncImage(
            model = fullModel,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = if (isGesturing) scale else animSc,
                    scaleY = if (isGesturing) scale else animSc,
                    translationX = if (isGesturing) offsetX else animOX,
                    translationY = if (isGesturing) offsetY else animOY
                ),
            loading = {
                // Показываем thumbnail как превью во время загрузки
                if (thumbModel != null) {
                    Image(
                        painter = coil.compose.rememberAsyncImagePainter(thumbModel),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(12.dp)
                            .graphicsLayer(alpha = 0.6f)
                    )
                }
                // Круглый прогресс поверх
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularLoadingProgress(progress = rawProgress)
                }
            },
            success = { state ->
                Image(
                    painter = state.painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. КРУГЛЫЙ ПРОГРЕСС-БАР — реальный прогресс + проценты
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CircularLoadingProgress(progress: Float) {
    val animProg by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(200, easing = LinearOutSlowInEasing),
        label = "cp"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
        if (progress <= 0f) {
            // Индетерминированный спиннер пока нет данных о прогрессе
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                color = Color.White.copy(0.7f),
                strokeWidth = 2.5.dp
            )
        } else {
            // Детерминированный прогресс с процентом
            CircularProgressIndicator(
                progress = { animProg },
                modifier = Modifier.size(44.dp),
                color = Color(0xFF6B9BF2),
                strokeWidth = 2.5.dp,
                trackColor = Color.White.copy(0.1f)
            )
            Text(
                "${(animProg * 100).toInt()}%",
                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ЗУМИРУЕМОЕ ИЗОБРАЖЕНИЕ (без превью, для совместимости)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ZoomableImage(
    model: Any,
    contentDescription: String?,
    imageUrl: String,
    onScaleChange: (Float) -> Unit,
    onSingleTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isGesturing by remember { mutableStateOf(false) }

    val animSc by animateFloatAsState(scale, spring(stiffness = Spring.StiffnessMediumLow), label = "sc")
    val animOX by animateFloatAsState(offsetX, spring(stiffness = Spring.StiffnessMediumLow), label = "ox")
    val animOY by animateFloatAsState(offsetY, spring(stiffness = Spring.StiffnessMediumLow), label = "oy")

    val rawProgress by remember(imageUrl) { derivedStateOf { ProgressTracker.progressMap[imageUrl] ?: 0f } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        isGesturing = false
                        if (scale > 1f) { scale = 1f; offsetX = 0f; offsetY = 0f }
                        else scale = 2.5f
                        onScaleChange(scale)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    isGesturing = true
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    onScaleChange(scale)
                    if (scale > 1f) {
                        val maxX = (size.width * (scale - 1)) / 2f
                        val maxY = (size.height * (scale - 1)) / 2f
                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                    } else { offsetX = 0f; offsetY = 0f }
                }
            }
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = if (isGesturing) scale else animSc,
                    scaleY = if (isGesturing) scale else animSc,
                    translationX = if (isGesturing) offsetX else animOX,
                    translationY = if (isGesturing) offsetY else animOY
                ),
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularLoadingProgress(progress = rawProgress)
                }
            },
            success = { state ->
                Image(
                    painter = state.painter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. ВИДЕО-ПЛЕЕР
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VideoPlayerItem(
    videoUrl: String,
    showControls: Boolean,
    onToggleUi: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            val item = MediaItem.Builder().setUri(videoUrl).build()
            setMediaItem(item)
            playWhenReady = true
            prepare()
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            })
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPos = exoPlayer.currentPosition.coerceAtLeast(0)
            duration = exoPlayer.duration.coerceAtLeast(1L)
            delay(200)
        }
    }

    DisposableEffect(videoUrl) { onDispose { exoPlayer.release() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onToggleUi)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 152.dp, start = 16.dp, end = 16.dp)
        ) {
            VideoTrackBar(
                isPlaying = isPlaying,
                isMuted = isMuted,
                currentPos = currentPos,
                duration = duration,
                onPlayPause = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                onMute = { isMuted = !isMuted; exoPlayer.volume = if (isMuted) 0f else 1f },
                onSeek = { fraction -> exoPlayer.seekTo((fraction * duration).toLong()) }
            )
        }
    }
}

@Composable
fun VideoTrackBar(
    isPlaying: Boolean,
    isMuted: Boolean,
    currentPos: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onMute: () -> Unit,
    onSeek: (Float) -> Unit
) {
    fun fmt(ms: Long): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }
    val progress = (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val animProg by animateFloatAsState(progress, tween(180), label = "vp")
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1A1C24).copy(0.88f))
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.10f))
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                null, tint = Color.White, modifier = Modifier.size(18.dp)
            )
        }

        Text(fmt(currentPos), color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)

        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(0.15f))
                    .pointerInput(duration) {
                        barWidthPx = size.width.toFloat()
                        detectTapGestures { offset ->
                            if (barWidthPx > 0) onSeek(offset.x / barWidthPx)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animProg)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                )
            }
            val thumbOffsetDp = with(density) { (animProg * (barWidthPx - 10f)).coerceAtLeast(0f).toDp() }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffsetDp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Text(fmt(duration), color = Color.White.copy(0.35f), fontSize = 11.sp)

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.10f))
                .clickable(onClick = onMute),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ГЛАВНЫЙ ЭКРАН
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    baseUrl: String,
    relPath: String,
    onBack: () -> Unit,
    viewModel: ViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    var showUi by remember { mutableStateOf(true) }
    var showMetadataSheet by remember { mutableStateOf(false) }
    var currentScale by remember { mutableFloatStateOf(1f) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(relPath) { viewModel.init(baseUrl, relPath) }

    BackHandler {
        if (currentScale > 1f) { currentScale = 1f } else onBack()
    }

    if (viewModel.images.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0D0E12)), contentAlignment = Alignment.Center) {
            CircularLoadingProgress(0f)
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = viewModel.initialIndex) { viewModel.images.size }
    val filmstripState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentItem = viewModel.images.getOrElse(pagerState.currentPage) { viewModel.images.first() }
    val isVideo = currentItem.type == "video"
    val isFavorite = viewModel.currentDetails?.isFavorite ?: currentItem.isFavorite

    LaunchedEffect(pagerState.currentPage) {
        currentScale = 1f
        currentItem.relPath?.let { viewModel.fetchDetails(baseUrl, it) }
        scope.launch { filmstripState.animateScrollToItem(maxOf(0, pagerState.currentPage - 3)) }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0D0E12))) {

        // ── PAGER ────────────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = currentScale <= 1f,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val item = viewModel.images[page]
            val fullUrl = buildFullUrl(baseUrl, item.url)
            val thumbUrl = buildFullUrl(baseUrl, item.thumb)

            if (item.type == "video") {
                VideoPlayerItem(
                    videoUrl = fullUrl,
                    showControls = showUi,
                    onToggleUi = { showUi = !showUi }
                )
            } else {
                val fullReq = ImageRequest.Builder(context)
                    .data(fullUrl)
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .memoryCacheKey(fullUrl)
                    .diskCacheKey(fullUrl)
                    .crossfade(true)
                    .build()
                val thumbReq = ImageRequest.Builder(context)
                    .data(thumbUrl)
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .memoryCacheKey(thumbUrl)
                    .diskCacheKey(thumbUrl)
                    .build()

                ImageLoaderWithPreview(
                    fullModel = fullReq,
                    thumbModel = thumbReq,
                    imageUrl = fullUrl,
                    onScaleChange = { currentScale = it },
                    onSingleTap = { showUi = !showUi }
                )
            }
        }

        // ── TOP BAR — градиент от верхнего края ───────────────────────────
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) + slideInVertically(tween(200, easing = FastOutSlowInEasing)) { -it },
            exit = fadeOut(tween(150, easing = FastOutSlowInEasing)) + slideOutVertically(tween(150, easing = FastOutSlowInEasing)) { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GlassCircleBtn(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(19.dp))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "${pagerState.currentPage + 1} / ${viewModel.images.size}",
                            color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            currentItem.name ?: "",
                            color = Color.White.copy(0.4f), fontSize = 10.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isVideo) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF6B9BF2).copy(0.15f))
                                .border(1.dp, Color(0xFF6B9BF2).copy(0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.Videocam, null, tint = Color(0xFF6B9BF2), modifier = Modifier.size(11.dp))
                                Text("ВИДЕО", color = Color(0xFF6B9BF2), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Spacer(Modifier.size(40.dp))
                    }
                }
            }
        }

        // ── BOTTOM — градиент от нижнего края ───────────────────────────────
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) + slideInVertically(tween(200, easing = FastOutSlowInEasing)) { it },
            exit = fadeOut(tween(150, easing = FastOutSlowInEasing)) + slideOutVertically(tween(150, easing = FastOutSlowInEasing)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Filmstrip
                    LazyRow(
                        state = filmstripState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        itemsIndexed(viewModel.images) { index, item ->
                            val selected = index == pagerState.currentPage
                            val thumbUrl = buildFullUrl(baseUrl, item.thumb)
                            val req = ImageRequest.Builder(context)
                                .data(thumbUrl)
                                .addHeader("ngrok-skip-browser-warning", "true")
                                .memoryCacheKey(thumbUrl)
                                .diskCacheKey(thumbUrl)
                                .crossfade(true)
                                .build()

                            val ts by animateFloatAsState(if (selected) 1.1f else 1f, label = "ts")

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .graphicsLayer(scaleX = ts, scaleY = ts)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        if (selected) 2.dp else 0.5.dp,
                                        if (selected) Color.White else Color.White.copy(0.20f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                            ) {
                                SubcomposeAsyncImage(
                                    model = req, contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(if (selected) 8.dp else 10.dp))
                                )
                                if (!selected) Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)))

                                if (item.type == "video") {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Индикатор избранного
                                if (item.isFavorite) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE94560).copy(0.85f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Favorite,
                                            null, tint = Color.White, modifier = Modifier.size(9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Action Pill — только иконки, без подписей
                    ActionPillBar(
                        isFavorite = isFavorite,
                        onFavorite = {
                            val relPath = currentItem.relPath
                            if (relPath != null) {
                                viewModel.toggleFavorite(baseUrl, relPath, isFavorite)
                            }
                        },
                        onInfo = { showMetadataSheet = true },
                        onCopyPrompt = {
                            val prompt = viewModel.currentDetails?.positive
                            if (!prompt.isNullOrBlank()) copyToClipboard(context, "Prompt", prompt)
                            else Toast.makeText(context, "Метаданные не загружены", Toast.LENGTH_SHORT).show()
                        },
                        onDownload = {
                            val rp = currentItem.relPath
                            if (rp != null) downloadFile(context, baseUrl, rp, currentItem.name ?: "image")
                        },
                        onShare = {
                            val rp = currentItem.relPath
                            if (rp != null) shareFile(context, baseUrl, rp, currentItem.name ?: "image", scope)
                        },
                        onDelete = { showDeleteDialog = true }
                    )
                }
            }
        }

        // ── METADATA SHEET ───────────────────────────────────────────────────
        if (showMetadataSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMetadataSheet = false },
                containerColor = Color(0xFF111318),
                scrimColor = Color.Black.copy(0.55f),
                dragHandle = {
                    Box(
                        Modifier
                            .padding(top = 12.dp, bottom = 6.dp)
                            .size(36.dp, 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(0.18f))
                    )
                },
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            ) {
                when {
                    viewModel.isLoadingDetails -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularLoadingProgress(0f) }
                    viewModel.currentDetails != null -> MetadataPanelContent(viewModel.currentDetails!!, context)
                    else -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Нет метаданных", color = Color.White.copy(0.3f), fontSize = 14.sp)
                    }
                }
            }
        }

        // ── DELETE DIALOG ────────────────────────────────────────────────────
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color(0xFF1A1C24),
                title = { Text("Удалить файл", color = Color.White) },
                text = {
                    Text(
                        "Переместить «${currentItem.name ?: "файл"}» в корзину?",
                        color = Color.White.copy(0.7f), fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        val rp = currentItem.relPath
                        if (rp != null) {
                            viewModel.deleteImage(baseUrl, rp) {
                                Toast.makeText(context, "Файл удалён", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    }) { Text("Удалить", color = Color(0xFFCD3737)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена", color = Color.White.copy(0.6f)) }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTION PILL BAR — только иконки, без подписей
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ActionPillBar(
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onInfo: () -> Unit,
    onCopyPrompt: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color(0xFF16181F).copy(0.75f))
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PillCircleBtn(Icons.Outlined.Info, "Информация") { onInfo() }
        PillCircleBtn(
            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDesc = "Избранное",
            tint = if (isFavorite) Color(0xFFE94560) else Color.White,
            onClick = onFavorite
        )
        PillCircleBtn(Icons.Filled.ContentCopy, "Скопировать промпт") { onCopyPrompt() }
        PillCircleBtn(Icons.Filled.Download, "Скачать") { onDownload() }
        PillCircleBtn(Icons.Outlined.Share, "Поделиться") { onShare() }
        PillCircleBtn(Icons.Outlined.DeleteOutline, "Удалить", tint = Color(0xFFCD3737)) { onDelete() }
    }
}

@Composable
fun PillCircleBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDesc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlassCircleBtn(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.10f))
            .border(1.dp, Color.White.copy(0.08f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. СКАЧАТЬ / ПОДЕЛИТЬСЯ
// ─────────────────────────────────────────────────────────────────────────────
fun downloadFile(context: Context, baseUrl: String, relPath: String, name: String) {
    try {
        val downloadUrl = buildFullUrl(baseUrl, "/api/download?relpath=${Uri.encode(relPath)}")
        val request = android.app.DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(name)
            .setDescription("Comfy Sorter")
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, name)
            .addRequestHeader("ngrok-skip-browser-warning", "true")
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Загрузка: $name", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareFile(
    context: Context,
    baseUrl: String,
    relPath: String,
    name: String,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val mediaUrl = buildFullUrl(baseUrl, "/media/$relPath")
    scope.launch(Dispatchers.IO) {
        try {
            val ext = name.substringAfterLast('.', "jpg")
            val tempFile = File(context.cacheDir, "share_${System.currentTimeMillis()}.$ext")
            URL(mediaUrl).openConnection().apply {
                setRequestProperty("ngrok-skip-browser-warning", "true")
                connect()
            }.getInputStream().use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
            val mime = when (ext.lowercase()) {
                "mp4", "mov", "webm", "mkv" -> "video/*"
                else -> "image/*"
            }
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mime
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(android.content.Intent.createChooser(intent, name))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cb.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
}

// ─────────────────────────────────────────────────────────────────────────────
// METADATA PANEL
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetadataPanelContent(details: ImageDetails, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 18.dp)) {
            Box(
                Modifier
                    .size(42.dp).clip(CircleShape)
                    .background(Color(0xFF6B9BF2).copy(0.10f))
                    .border(1.dp, Color(0xFF6B9BF2).copy(0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Image, null, tint = Color(0xFF6B9BF2), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Свойства", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                val sizeKb = (details.size ?: 0L) / 1024
                val sizeStr = if (sizeKb > 1024) "${sizeKb / 1024} MB" else "${sizeKb} KB"
                val dimStr = "${details.width ?: 0} × ${details.height ?: 0}"
                Text(
                    "$dimStr  •  $sizeStr",
                    color = Color(0xFF666677), fontSize = 12.sp
                )
            }
        }

        if (!details.positive.isNullOrBlank()) {
            PromptSection("Промпт", details.positive, Color(0xFF10B981), context)
            Spacer(Modifier.height(14.dp))
        }
        if (!details.negative.isNullOrBlank()) {
            PromptSection("Негативный", details.negative, Color(0xFFEF4444), context)
            Spacer(Modifier.height(14.dp))
        }

        val params = details.parameters
        if (!params.isNullOrEmpty()) {
            Text("Параметры", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                params.forEach { (key, value) ->
                    if (key.lowercase() !in listOf("loras", "lora")) {
                        ParameterChip(key, value.toString())
                    }
                }
            }

            val lorasRaw = params.entries.firstOrNull { it.key.lowercase() in listOf("loras", "lora") }?.value
            val loras = parseLorasSafely(lorasRaw)
            if (loras.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("LoRA", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    loras.forEach { LoraChip(it) }
                }
            }
        }
    }
}

@Composable
fun PromptSection(title: String, content: String, accent: Color, context: Context) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(8.dp))
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                Modifier
                    .size(28.dp).clip(CircleShape)
                    .background(Color.White.copy(0.05f))
                    .clickable { copyToClipboard(context, title, content) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ContentCopy, null, tint = Color.White.copy(0.35f), modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White.copy(0.04f))
                .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(13.dp))
                .padding(13.dp)
        ) {
            Text(content, color = Color.White.copy(0.82f), fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun ParameterChip(key: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White.copy(0.05f))
            .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$key: ", color = Color.White.copy(0.35f), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LoraChip(lora: LoraData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF9333EA).copy(0.10f))
            .border(1.dp, Color(0xFF9333EA).copy(0.20f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Extension, null, tint = Color(0xFFD8B4FE), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(9.dp))
            Text(lora.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text("clip", color = Color(0xFFD8B4FE).copy(0.45f), fontSize = 9.sp)
                Text("%.2f".format(lora.weightClip), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("model", color = Color(0xFFD8B4FE).copy(0.45f), fontSize = 9.sp)
                Text("%.2f".format(lora.weightModel), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
