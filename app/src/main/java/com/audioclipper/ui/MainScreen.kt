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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.audioclipper.ui.theme.PurpleAccent
import com.audioclipper.ui.theme.PurpleAccentDark
import com.audioclipper.ui.theme.PurpleAccentLight
import com.audioclipper.ui.theme.PurpleContainer
import com.audioclipper.ui.theme.Charcoal600
import com.audioclipper.ui.theme.Charcoal700
import com.audioclipper.viewmodel.ExportState
import com.audioclipper.viewmodel.MainViewModel
import kotlinx.coroutines.delay

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
    val exportState by viewModel.exportState.collectAsState()

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    // Load audio when URI changes
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

    // Poll position
    LaunchedEffect(audioUri) {
        while (true) {
            delay(100)
            if (!isDragging && audioUri != null) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0)
                isPlaying = player.isPlaying
            }
        }
    }

    // Export state feedback
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

    Scaffold(
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
                    tint = PurpleAccent,
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
                    text = "Trim audio files with precision",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        permissionLauncher.launch(permission)
                        audioPickerLauncher.launch(
                            arrayOf(
                                "audio/mpeg", "audio/mp4", "audio/aac",
                                "audio/ogg", "audio/wav", "audio/flac",
                                "audio/x-wav", "audio/x-m4a", "audio/*"
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
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
                    .padding(16.dp)
            ) {
                // File info header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = audioFileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
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

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline scrubber
                TimelineScrubber(
                    durationMs = durationMs,
                    currentPositionMs = currentPositionMs,
                    inPointMs = inPointMs,
                    outPointMs = outPointMs,
                    isDragging = isDragging,
                    dragPosition = dragPosition,
                    onDragStart = { fraction ->
                        isDragging = true
                        dragPosition = fraction
                    },
                    onDrag = { fraction ->
                        dragPosition = fraction.coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        val seekTo = (dragPosition * durationMs).toLong()
                        player.seekTo(seekTo)
                        currentPositionMs = seekTo
                        isDragging = false
                    },
                    onTap = { fraction ->
                        val seekTo = (fraction * durationMs).toLong()
                        player.seekTo(seekTo)
                        currentPositionMs = seekTo
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayPos = if (isDragging) (dragPosition * durationMs).toLong() else currentPositionMs
                    Text(
                        text = formatTime(displayPos),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(durationMs),
                        style = MaterialTheme.typography.bodySmall,
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
                    IconButton(onClick = {
                        player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                    }) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = "-10s",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            if (player.isPlaying) player.pause() else player.play()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(PurpleAccent, RoundedCornerShape(28.dp))
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = {
                        player.seekTo((player.currentPosition + 10000).coerceAtMost(durationMs))
                    }) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "+10s",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Trim controls
                Text(
                    text = "TRIM POINTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setInPoint(currentPositionMs) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inPointMs != null) PurpleAccentDark else Charcoal600
                        )
                    ) {
                        Text(
                            text = if (inPointMs != null) "IN: ${formatTime(inPointMs!!)}" else "SET IN",
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = { viewModel.setOutPoint(currentPositionMs) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (outPointMs != null) PurpleAccentDark else Charcoal600
                        )
                    ) {
                        Text(
                            text = if (outPointMs != null) "OUT: ${formatTime(outPointMs!!)}" else "SET OUT",
                            maxLines = 1
                        )
                    }
                }

                // Trim range info
                if (inPointMs != null || outPointMs != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val rangeStart = inPointMs ?: 0L
                    val rangeEnd = outPointMs ?: durationMs
                    Text(
                        text = "Selection: ${formatTime(rangeEnd - rangeStart)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PurpleAccentLight,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Fade controls
                Text(
                    text = "FADE EFFECTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Fade In
                FadeControl(
                    label = "Fade In",
                    enabled = fadeInEnabled,
                    onEnabledChange = { viewModel.setFadeInEnabled(it) },
                    duration = fadeInDuration,
                    onDurationChange = { viewModel.setFadeInDuration(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fade Out
                FadeControl(
                    label = "Fade Out",
                    enabled = fadeOutEnabled,
                    onEnabledChange = { viewModel.setFadeOutEnabled(it) },
                    duration = fadeOutDuration,
                    onDurationChange = { viewModel.setFadeOutDuration(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Export button
                Button(
                    onClick = { viewModel.export(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = exportState !is ExportState.Exporting,
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    if (exportState is ExportState.Exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
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

                Spacer(modifier = Modifier.height(16.dp))

                // Open another file
                OutlinedButton(
                    onClick = {
                        audioPickerLauncher.launch(
                            arrayOf(
                                "audio/mpeg", "audio/mp4", "audio/aac",
                                "audio/ogg", "audio/wav", "audio/flac",
                                "audio/x-wav", "audio/x-m4a", "audio/*"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Another File")
                }
            }
        }
    }
}

@Composable
private fun TimelineScrubber(
    durationMs: Long,
    currentPositionMs: Long,
    inPointMs: Long?,
    outPointMs: Long?,
    isDragging: Boolean,
    dragPosition: Float,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onTap: (Float) -> Unit
) {
    val accentColor = PurpleAccent
    val accentLight = PurpleAccentLight
    val selectionColor = PurpleContainer
    val trackColor = Charcoal700

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0) {
                        onTap(offset.x / size.width.toFloat())
                    }
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (durationMs > 0) {
                            onDragStart(offset.x / size.width.toFloat())
                        }
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onHorizontalDrag = { _, dragAmount ->
                        if (durationMs > 0) {
                            onDrag(dragPosition + dragAmount / size.width.toFloat())
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

            // Track background
            drawRect(
                color = trackColor,
                topLeft = Offset(0f, trackY - trackHeight / 2),
                size = Size(w, trackHeight)
            )

            // Selection range
            if (durationMs > 0 && (inPointMs != null || outPointMs != null)) {
                val startFrac = (inPointMs ?: 0L).toFloat() / durationMs
                val endFrac = (outPointMs ?: durationMs).toFloat() / durationMs
                drawRect(
                    color = selectionColor,
                    topLeft = Offset(startFrac * w, 0f),
                    size = Size((endFrac - startFrac) * w, h)
                )
            }

            // In point marker
            if (durationMs > 0 && inPointMs != null) {
                val x = (inPointMs.toFloat() / durationMs) * w
                drawLine(
                    color = accentLight,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Out point marker
            if (durationMs > 0 && outPointMs != null) {
                val x = (outPointMs.toFloat() / durationMs) * w
                drawLine(
                    color = accentLight,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Playhead
            if (durationMs > 0) {
                val posFrac = if (isDragging) dragPosition else currentPositionMs.toFloat() / durationMs
                val px = posFrac.coerceIn(0f, 1f) * w

                // Playhead line
                drawLine(
                    color = accentColor,
                    start = Offset(px, 0f),
                    end = Offset(px, h),
                    strokeWidth = 2.dp.toPx()
                )

                // Playhead circle
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
                    checkedThumbColor = PurpleAccent,
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
                    style = MaterialTheme.typography.bodySmall,
                    color = PurpleAccentLight,
                    modifier = Modifier.width(36.dp)
                )
                Slider(
                    value = duration,
                    onValueChange = onDurationChange,
                    valueRange = 0f..5f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = PurpleAccent,
                        activeTrackColor = PurpleAccent,
                        inactiveTrackColor = Charcoal600
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
