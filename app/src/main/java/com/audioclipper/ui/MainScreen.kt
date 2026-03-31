package com.audioclipper.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.audioclipper.ui.theme.BrightPurple
import com.audioclipper.ui.theme.Charcoal700
import com.audioclipper.ui.theme.PrimaryViolet
import com.audioclipper.ui.theme.PurpleContainer
import com.audioclipper.viewmodel.ExportState
import com.audioclipper.viewmodel.MainViewModel
import kotlin.math.pow
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val audioUri by viewModel.audioUri.collectAsState()
    val audioFileName by viewModel.audioFileName.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val inPointMs by viewModel.inPointMs.collectAsState()
    val outPointMs by viewModel.outPointMs.collectAsState()
    val fadeInEnabled by viewModel.fadeInEnabled.collectAsState()
    val fadeOutEnabled by viewModel.fadeOutEnabled.collectAsState()
    val fadeInDuration by viewModel.fadeInDuration.collectAsState()
    val fadeOutDuration by viewModel.fadeOutDuration.collectAsState()
    val pitchSemitones by viewModel.pitchSemitones.collectAsState()
    val volumePercent by viewModel.volumePercent.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    val dragState = remember { floatArrayOf(0f, 0f) } // [0]=fraction, [1]=1.0 if dragging
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(audioUri) {
        audioUri?.let { uri ->
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        viewModel.setDuration(player.duration.coerceAtLeast(0))
                    }
                }
            })
        } ?: run {
            player.stop()
            player.clearMediaItems()
            currentPositionMs = 0L
            isPlaying = false
        }
    }

    LaunchedEffect(audioUri) {
        while (true) {
            delay(100)
            if (audioUri != null) {
                isPlaying = player.isPlaying
                if (dragState[1] == 0f) {
                    currentPositionMs = player.currentPosition.coerceAtLeast(0)
                    val frac = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
                    scrubFraction = frac
                    dragState[0] = frac
                } else {
                    scrubFraction = dragState[0]
                }
            }
        }
    }

    LaunchedEffect(pitchSemitones) {
        val factor = 2f.pow(pitchSemitones / 12f)
        player.playbackParameters = PlaybackParameters(player.playbackParameters.speed, factor)
    }

    LaunchedEffect(volumePercent) {
        player.volume = (volumePercent / 100f).coerceIn(0f, 1f)
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Done -> {
                snackbarHostState.showSnackbar(
                    message = "Exported to ${state.path}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetExportState()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val audioMimeTypes = arrayOf(
        "audio/mpeg", "audio/mp4", "audio/aac",
        "audio/ogg", "audio/wav", "audio/flac",
        "audio/x-wav", "audio/x-m4a", "audio/*"
    )

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val fileName = getFileName(context, it) ?: "audio"
            viewModel.setAudioUri(it, fileName)
        }
    }

    fun launchPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
        audioPickerLauncher.launch(audioMimeTypes)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.ContentCut,
                            contentDescription = null,
                            tint = BrightPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "AudioClipper",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (audioUri == null) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = BrightPurple,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "AudioClipper",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Trim, pitch-shift & process audio",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                FilledTonalButton(onClick = { launchPicker() }) {
                    Icon(Icons.Filled.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Audio File")
                }
            }
        } else {
            // Main editor
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // --- File Info Card ---
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = BrightPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = audioFileName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            player.stop()
                            player.clearMediaItems()
                            viewModel.clearAudio()
                        }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Playback Card ---
                SectionCard {
                    // Timeline scrubber
                    TimelineScrubber(
                        durationMs = durationMs,
                        scrubFraction = scrubFraction,
                        inPointMs = inPointMs,
                        outPointMs = outPointMs,
                        dragState = dragState,
                        player = player
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Time display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayPos = if (dragState[1] == 1f) (scrubFraction * durationMs).toLong() else currentPositionMs
                        Text(
                            text = formatTime(displayPos),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Playback controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = "-10s",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = {
                                if (player.isPlaying) player.pause() else player.play()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(PrimaryViolet, RoundedCornerShape(28.dp))
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = {
                                player.seekTo((player.currentPosition + 10000).coerceAtMost(durationMs))
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "+10s",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Trim Card ---
                SectionCard {
                    SectionHeader(emoji = "\u2702\uFE0F", label = "Trim")

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.setInPoint(currentPositionMs) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(
                                text = if (inPointMs != null) "IN: ${formatTime(inPointMs!!)}" else "SET IN",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        FilledTonalButton(
                            onClick = { viewModel.setOutPoint(currentPositionMs) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(
                                text = if (outPointMs != null) "OUT: ${formatTime(outPointMs!!)}" else "SET OUT",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (inPointMs != null || outPointMs != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val rangeStart = inPointMs ?: 0L
                        val rangeEnd = outPointMs ?: durationMs
                        Text(
                            text = "Selection: ${formatTime(rangeEnd - rangeStart)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrightPurple,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Pitch Card ---
                SectionCard {
                    SectionHeader(emoji = "\uD83C\uDFB5", label = "Pitch")

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.pitchDown() },
                            modifier = Modifier.size(48.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.Filled.ArrowDownward,
                                contentDescription = "Pitch down",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Text(
                            text = when {
                                pitchSemitones > 0 -> "Pitch: +$pitchSemitones st"
                                pitchSemitones < 0 -> "Pitch: $pitchSemitones st"
                                else -> "Pitch: 0 st"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (pitchSemitones != 0) BrightPurple else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(100.dp),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        OutlinedButton(
                            onClick = { viewModel.pitchUp() },
                            modifier = Modifier.size(48.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = "Pitch up",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (pitchSemitones != 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.resetPitch() },
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("Reset Pitch", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Volume Card ---
                SectionCard {
                    SectionHeader(emoji = "\uD83D\uDD0A", label = "Volume")

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = volumePercent.toFloat(),
                            onValueChange = { viewModel.setVolumePercent(it.toInt()) },
                            valueRange = 0f..200f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryViolet,
                                activeTrackColor = PrimaryViolet,
                                inactiveTrackColor = Charcoal700
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$volumePercent%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (volumePercent != 100) BrightPurple else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(48.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Fade Card ---
                SectionCard {
                    SectionHeader(emoji = "\uD83C\uDF0A", label = "Fade")

                    Spacer(modifier = Modifier.height(12.dp))

                    FadeControl(
                        label = "Fade In",
                        enabled = fadeInEnabled,
                        onEnabledChange = { viewModel.setFadeInEnabled(it) },
                        duration = fadeInDuration,
                        onDurationChange = { viewModel.setFadeInDuration(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FadeControl(
                        label = "Fade Out",
                        enabled = fadeOutEnabled,
                        onEnabledChange = { viewModel.setFadeOutEnabled(it) },
                        duration = fadeOutDuration,
                        onDurationChange = { viewModel.setFadeOutDuration(it) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Export Card ---
                SectionCard {
                    SectionHeader(emoji = "\uD83D\uDCE4", label = "Export")

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = { viewModel.export(context) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = exportState !is ExportState.Exporting
                    ) {
                        if (exportState is ExportState.Exporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BrightPurple,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exporting...")
                        } else {
                            Icon(Icons.Filled.ContentCut, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export as MP3")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { launchPicker() },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Another File")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionHeader(emoji: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TimelineScrubber(
    durationMs: Long,
    scrubFraction: Float,
    inPointMs: Long?,
    outPointMs: Long?,
    dragState: FloatArray,
    player: ExoPlayer
) {
    val accentColor = PrimaryViolet
    val accentLight = BrightPurple
    val selectionColor = PurpleContainer
    val trackColor = Charcoal700

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0) {
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        player.seekTo((frac * durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (durationMs > 0) {
                            dragState[0] = offset.x / size.width.toFloat()
                            dragState[1] = 1f
                        }
                    },
                    onDragEnd = {
                        player.seekTo((dragState[0] * durationMs).toLong())
                        dragState[1] = 0f
                    },
                    onDragCancel = {
                        dragState[1] = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (durationMs > 0) {
                            val newFrac = (dragState[0] + dragAmount / size.width.toFloat()).coerceIn(0f, 1f)
                            dragState[0] = newFrac
                            player.seekTo((newFrac * durationMs).toLong())
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val trackY = h / 2
            val trackHeight = 6.dp.toPx()

            drawRect(
                color = trackColor,
                topLeft = Offset(0f, trackY - trackHeight / 2),
                size = Size(w, trackHeight)
            )

            if (durationMs > 0 && (inPointMs != null || outPointMs != null)) {
                val startFrac = (inPointMs ?: 0L).toFloat() / durationMs
                val endFrac = (outPointMs ?: durationMs).toFloat() / durationMs
                drawRect(
                    color = selectionColor,
                    topLeft = Offset(startFrac * w, 0f),
                    size = Size((endFrac - startFrac) * w, h)
                )
            }

            if (durationMs > 0 && inPointMs != null) {
                val x = (inPointMs.toFloat() / durationMs) * w
                drawLine(
                    color = accentLight,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 3.dp.toPx()
                )
            }

            if (durationMs > 0 && outPointMs != null) {
                val x = (outPointMs.toFloat() / durationMs) * w
                drawLine(
                    color = accentLight,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 3.dp.toPx()
                )
            }

            if (durationMs > 0) {
                val px = scrubFraction.coerceIn(0f, 1f) * w

                drawLine(
                    color = accentColor,
                    start = Offset(px, 0f),
                    end = Offset(px, h),
                    strokeWidth = 2.dp.toPx()
                )

                drawCircle(
                    color = accentColor,
                    radius = 8.dp.toPx(),
                    center = Offset(px, trackY)
                )
            }
        }
    }
}

@Composable
private fun FadeControl(
    label: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    duration: Float,
    onDurationChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryViolet,
                    checkedTrackColor = PurpleContainer
                )
            )
        }
        if (enabled) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%.1fs".format(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = BrightPurple,
                    modifier = Modifier.width(36.dp)
                )
                Slider(
                    value = duration,
                    onValueChange = onDurationChange,
                    valueRange = 0f..5f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryViolet,
                        activeTrackColor = PrimaryViolet,
                        inactiveTrackColor = Charcoal700
                    )
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 10
    return "%d:%02d.%02d".format(minutes, seconds, millis)
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex)
        }
    }
    return uri.lastPathSegment
}
