package com.lucas.videoplayer.presentation.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.lucas.videoplayer.R
import com.lucas.videoplayer.data.repository.HistoryRepository
import com.lucas.videoplayer.data.repository.VideoRepository
import com.lucas.videoplayer.domain.model.Subtitle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VIDEO_PATH = "video_path"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_START_POSITION = "start_position"
    }

    @Inject lateinit var videoRepository: VideoRepository
    @Inject lateinit var historyRepository: HistoryRepository

    private var player: SimpleExoPlayer? = null
    private var playerView: PlayerView? = null
    private var videoPath: String = ""
    private var videoTitle: String = ""
    private var startPosition: Long = 0L
    private var isFullscreen = false
    private var controlsVisible = true
    private val controlsHideHandler = Handler(Looper.getMainLooper())
    private val controlsHideRunnable = { hideControls() }
    private val CONTROLS_HIDE_DELAY_MS = 3000L

    // Gesture detection
    private var gestureDetector: GestureDetector? = null
    private var initialVolume = 0f
    private var initialBrightness = 0f
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isVolumeControl = false
    private var isBrightnessControl = false
    private var isSeekControl = false

    // Subtitles
    private var availableSubtitles: List<Subtitle> = emptyList()
    private var selectedSubtitleIndex = -1

    // Views
    private lateinit var customControls: View
    private lateinit var playPauseBtn: ImageView
    private lateinit var fullscreenBtn: ImageView
    private lateinit var speedBtn: TextView
    private lateinit var subtitleBtn: TextView
    private lateinit var progressBar: AppCompatSeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var titleText: TextView
    private lateinit var lockBtn: ImageView
    private var isLocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Get extras
        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: return
        videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Video"
        startPosition = intent.getLongExtra(EXTRA_START_POSITION, 0L)

        initViews()
        initGestureDetector()
        initPlayer()
        loadSubtitles()
        restorePosition()
    }

    private fun initViews() {
        playerView = findViewById(R.id.player_view)
        customControls = findViewById(R.id.custom_controls)
        playPauseBtn = findViewById(R.id.play_pause_btn)
        fullscreenBtn = findViewById(R.id.fullscreen_btn)
        speedBtn = findViewById(R.id.speed_btn)
        subtitleBtn = findViewById(R.id.subtitle_btn)
        progressBar = findViewById(R.id.progress_bar)
        currentTimeText = findViewById(R.id.current_time)
        totalTimeText = findViewById(R.id.total_time)
        titleText = findViewById(R.id.video_title)
        lockBtn = findViewById(R.id.lock_btn)

        titleText.text = videoTitle

        // Click listeners
        playPauseBtn.setOnClickListener { togglePlayPause() }
        fullscreenBtn.setOnClickListener { toggleFullscreen() }
        speedBtn.setOnClickListener { showSpeedMenu() }
        subtitleBtn.setOnClickListener { showSubtitleMenu() }
        lockBtn.setOnClickListener { toggleLock() }

        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentTimeText.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                cancelControlsHide()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                player?.seekTo(seekBar.progress.toLong())
                scheduleControlsHide()
            }
        })

        playerView?.setOnTouchListener { _, event ->
            gestureDetector?.onTouchEvent(event) ?: false
        }

        customControls.setOnClickListener { } // Consume clicks
    }

    private fun initGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleControls()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                togglePlayPause()
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                touchStartX = e.x
                touchStartY = e.y
                cancelControlsHide()
                return true
            }

            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isLocked) return false

                val deltaX = e2.x - touchStartX
                val deltaY = touchStartY - e2.y // Invertido para que arriba = positivo

                // Determinar tipo de control basado en posición inicial
                if (!isVolumeControl && !isBrightnessControl && !isSeekControl) {
                    val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                    if (touchStartX > screenWidth * 0.7) {
                        isVolumeControl = true
                        initialVolume = getVolume()
                    } else if (touchStartX < screenWidth * 0.3) {
                        isBrightnessControl = true
                        initialBrightness = window.attributes.screenBrightness
                    } else {
                        isSeekControl = true
                    }
                }

                when {
                    isVolumeControl -> adjustVolume(deltaY)
                    isBrightnessControl -> adjustBrightness(deltaY)
                    isSeekControl -> adjustSeek(deltaX)
                }
                return true
            }

            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                return onScroll(e1, e2, distanceX, distanceY)
            }
        })

        gestureDetector?.isLongPressEnabled = false
    }

    private fun initPlayer() {
        val trackSelector = DefaultTrackSelector(this).apply {
            // Preferir pistas de audio en español
            parameters = parameters.copy(
                trackSelectionParameters = TrackSelectionParameters.DEFAULT
                    .copy(preferredAudioLanguage = "es")
            )
        }

        player = SimpleExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()

        playerView?.player = player

        val mediaItem = MediaItem.fromUri(videoPath)
        val mediaSource = ProgressiveMediaSource.Factory(
            DefaultDataSourceFactory(this)
        ).createMediaItem(mediaItem)

        player?.setMediaSource(mediaSource)
        player?.prepare()
        player?.playWhenReady = true

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlayPauseButton()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlayerActivity", "Error: ${error.message}")
                runOnUiThread {
                    Toast.makeText(this@PlayerActivity, "Error de reproducción: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseButton()
            }

            override fun onPositionDiscontinuity(reason: Int) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    savePosition()
                }
            }
        })

        // Update progress periodically
        lifecycleScope.launch {
            while (player != null && !isDestroyed) {
                kotlinx.coroutines.delay(500)
                runOnUiThread { updateProgress() }
            }
        }

        // Auto-save position every 5 seconds
        lifecycleScope.launch {
            while (player != null && !isDestroyed) {
                kotlinx.coroutines.delay(5000)
                savePosition()
            }
        }
    }

    private fun loadSubtitles() {
        val videoFile = File(videoPath)
        val parent = videoFile.parentFile ?: return
        val nameWithoutExt = videoFile.nameWithoutExtension
        val subtitleExtensions = listOf(".srt", ".vtt", ".ass", ".ssa")

        availableSubtitles = parent.listFiles()?.filter { file ->
            subtitleExtensions.any { file.name.lowercase().endsWith(it) } &&
                    file.nameWithoutExtension.startsWith(nameWithoutExt)
        }?.map { Subtitle.fromPath(it.absolutePath) } ?: emptyList()

        subtitleBtn.isVisible = availableSubtitles.isNotEmpty()
        if (availableSubtitles.isNotEmpty()) {
            // Seleccionar primer subtítulo por defecto si coincide con idioma del sistema
            val systemLang = resources.configuration.locales.get(0).language
            selectedSubtitleIndex = availableSubtitles.indexOfFirst { it.language == systemLang }
            if (selectedSubtitleIndex == -1) selectedSubtitleIndex = 0
            updateSubtitleButton()
            applySubtitle(selectedSubtitleIndex)
        }
    }

    private fun applySubtitle(index: Int) {
        if (index < 0 || index >= availableSubtitles.size) return
        val subtitle = availableSubtitles[index]
        // TODO: Implementar carga de subtítulos externos en ExoPlayer
        // Requiere crear MediaSource con subtítulos
        selectedSubtitleIndex = index
        updateSubtitleButton()
    }

    private fun updateSubtitleButton() {
        if (selectedSubtitleIndex >= 0 && selectedSubtitleIndex < availableSubtitles.size) {
            subtitleBtn.text = "CC: ${availableSubtitles[selectedSubtitleIndex].language.uppercase()}"
        } else {
            subtitleBtn.text = "CC: Off"
        }
    }

    private fun restorePosition() {
        lifecycleScope.launch(Dispatchers.IO) {
            val lastPos = historyRepository.getLastPosition(videoPath) ?: 0L
            if (lastPos > 1000 && startPosition == 0L) { // Solo restaurar si > 1s y no hay posición inicial
                runOnUiThread {
                    val resume = androidx.appcompat.app.AlertDialog.Builder(this@PlayerActivity)
                        .setTitle("Continuar viendo")
                        .setMessage("¿Reanudar desde ${formatTime(lastPos)}?")
                        .setPositiveButton("Sí") { _, _ ->
                            player?.seekTo(lastPos)
                        }
                        .setNegativeButton("No") { _, _ -> }
                        .show()
                }
            } else if (startPosition > 0) {
                player?.seekTo(startPosition)
            }
        }
    }

    private fun savePosition() {
        val position = player?.currentPosition ?: 0L
        val duration = player?.duration ?: 0L
        val completed = duration > 0 && position > duration * 0.9

        lifecycleScope.launch(Dispatchers.IO) {
            historyRepository.savePlayback(videoPath, position, duration, completed)
            videoRepository.updatePlayStats(videoPath, System.currentTimeMillis(), position)
        }
    }

    private fun updateProgress() {
        val position = player?.currentPosition ?: 0L
        val duration = player?.duration ?: 0L

        progressBar.max = duration.toInt()
        progressBar.progress = position.toInt()
        currentTimeText.text = formatTime(position)
        totalTimeText.text = formatTime(duration)
    }

    private fun updatePlayPauseButton() {
        val isPlaying = player?.playWhenReady == true && player?.playbackState == Player.STATE_READY
        playPauseBtn.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun togglePlayPause() {
        player?.playWhenReady = player?.playWhenReady != true
        updatePlayPauseButton()
        scheduleControlsHide()
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            supportActionBar?.hide()
            fullscreenBtn.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            supportActionBar?.show()
            fullscreenBtn.setImageResource(R.drawable.ic_fullscreen)
        }
    }

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    private fun showControls() {
        controlsVisible = true
        customControls.animate().alpha(1f).setDuration(200).start()
        scheduleControlsHide()
    }

    private fun hideControls() {
        if (isLocked) return
        controlsVisible = false
        customControls.animate().alpha(0f).setDuration(200).start()
    }

    private fun scheduleControlsHide() {
        controlsHideHandler.removeCallbacks(controlsHideRunnable)
        controlsHideHandler.postDelayed(controlsHideRunnable, CONTROLS_HIDE_DELAY_MS)
    }

    private fun cancelControlsHide() {
        controlsHideHandler.removeCallbacks(controlsHideRunnable)
    }

    private fun toggleLock() {
        isLocked = !isLocked
        lockBtn.setImageResource(if (isLocked) R.drawable.ic_lock else R.drawable.ic_lock_open)
        if (!isLocked) scheduleControlsHide()
        Toast.makeText(this, if (isLocked) "Controles bloqueados" else "Controles desbloqueados", Toast.LENGTH_SHORT).show()
    }

    private fun showSpeedMenu() {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        PopupMenu(this, speedBtn).apply {
            speeds.forEach { speed ->
                menu.add("Speed: ${speed}x").setOnMenuItemClickListener {
                    player?.playbackParameters = player?.playbackParameters?.withSpeed(speed)
                    speedBtn.text = "Speed: ${speed}x"
                    true
                }
            }
            show()
        }
    }

    private fun showSubtitleMenu() {
        PopupMenu(this, subtitleBtn).apply {
            menu.add("Off").setOnMenuItemClickListener {
                selectedSubtitleIndex = -1
                updateSubtitleButton()
                // TODO: Deshabilitar subtítulos en player
                true
            }
            availableSubtitles.forEachIndexed { index, sub ->
                menu.add("${sub.language.uppercase()} (${sub.format.name})").setOnMenuItemClickListener {
                    applySubtitle(index)
                    true
                }
            }
            show()
        }
    }

    // Gesture helpers
    private fun getVolume(): Float {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    private fun adjustVolume(deltaY: Float) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val delta = (-deltaY / resources.displayMetrics.heightPixels) * maxVolume
        val newVolume = (initialVolume * maxVolume + delta).coerceIn(0f, maxVolume.toFloat()).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    }

    private fun adjustBrightness(deltaY: Float) {
        val delta = (-deltaY / resources.displayMetrics.heightPixels)
        val newBrightness = (initialBrightness + delta).coerceIn(0.01f, 1f)
        val attrs = window.attributes
        attrs.screenBrightness = newBrightness
        window.attributes = attrs
    }

    private fun adjustSeek(deltaX: Float) {
        val player = this.player ?: return
        val duration = player.duration
        if (duration <= 0) return

        val deltaPercent = deltaX / resources.displayMetrics.widthPixels
        val deltaMs = (duration * deltaPercent).toLong()
        val newPosition = (player.currentPosition + deltaMs).coerceIn(0L, duration)
        player.seekTo(newPosition)
        currentTimeText.text = formatTime(newPosition)
        progressBar.progress = newPosition.toInt()
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Manejar rotación sin recrear activity
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            hideControls()
            supportActionBar?.hide()
        } else {
            showControls()
            supportActionBar?.show()
        }
    }

    override fun onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            // No hacer nada, dejar que el sistema maneje PiP
            super.onBackPressed()
        } else {
            savePosition()
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        savePosition()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInPictureInPictureMode) {
            player?.playWhenReady = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInPictureInPictureMode) {
            player?.playWhenReady = true
        }
    }

    override fun onDestroy() {
        savePosition()
        controlsHideHandler.removeCallbacks(controlsHideRunnable)
        player?.release()
        player = null
        super.onDestroy()
    }
}