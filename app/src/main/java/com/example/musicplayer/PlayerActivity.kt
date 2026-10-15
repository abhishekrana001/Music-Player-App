package com.example.musicplayer

import android.content.Intent
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Random

class PlayerActivity : AppCompatActivity() {

    enum class RepeatMode {
        OFF, ALL, ONE
    }

    private lateinit var playerRootLayout: View
    private lateinit var playerToolbar: View
    private lateinit var playerControlsContainer: View
    private lateinit var playerAlbumArt: ImageView
    private lateinit var currentSongTitle: TextView
    private lateinit var currentSongArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var shuffleButton: ImageButton
    private lateinit var repeatButton: ImageButton

    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Song? = null
    private var currentPosition: Int = 0
    private var playlist: List<Song> = emptyList()
    private var isPlaying = false
    private var isShuffleOn = false
    private var repeatMode = RepeatMode.OFF

    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null
    private val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_player)

        initViews()
        setupEdgeToEdge()
        getIntentData()
        setupClickListeners()
        setupSeekBar()
    }

    private fun initViews() {
        playerRootLayout = findViewById(R.id.playerRootLayout)
        playerToolbar = findViewById(R.id.playerToolbar)
        playerControlsContainer = findViewById(R.id.playerControlsContainer)
        playerAlbumArt = findViewById(R.id.playerAlbumArt)
        currentSongTitle = findViewById(R.id.currentSongTitle)
        currentSongArtist = findViewById(R.id.currentSongArtist)
        seekBar = findViewById(R.id.seekBar)
        currentTime = findViewById(R.id.currentTime)
        totalTime = findViewById(R.id.totalTime)
        playPauseButton = findViewById(R.id.playPauseButton)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        backButton = findViewById(R.id.backButton)
        shuffleButton = findViewById(R.id.shuffleButton)
        repeatButton = findViewById(R.id.repeatButton)
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(playerRootLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            playerToolbar.setPadding(
                playerToolbar.paddingLeft,
                systemBars.top,
                playerToolbar.paddingRight,
                playerToolbar.paddingBottom
            )
            playerControlsContainer.setPadding(
                playerControlsContainer.paddingLeft,
                playerControlsContainer.paddingTop,
                playerControlsContainer.paddingRight,
                systemBars.bottom + 20
            )
            insets
        }
    }

    private fun getIntentData() {
        currentSong = intent.getParcelableExtraCompat("SONG", Song::class.java)
        currentPosition = intent.getIntExtra("POSITION", 0)
        playlist = intent.getParcelableArrayListExtraCompat("PLAYLIST", Song::class.java) ?: emptyList()

        currentSong?.let { song ->
            displaySongInfo(song)
            playSong(song)
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        playPauseButton.setOnClickListener {
            if (isPlaying) {
                pauseSong()
            } else {
                resumeSong()
            }
        }

        previousButton.setOnClickListener {
            playPreviousSong()
        }

        nextButton.setOnClickListener {
            playNextSong(auto = false)
        }

        shuffleButton.setOnClickListener {
            isShuffleOn = !isShuffleOn
            updateShuffleButton()
        }

        repeatButton.setOnClickListener {
            repeatMode = when (repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            updateRepeatButton()
        }
    }

    private fun updateShuffleButton() {
        val color = ContextCompat.getColor(
            this,
            if (isShuffleOn) R.color.icon_tint_active else R.color.icon_tint_inactive
        )
        shuffleButton.imageTintList = ColorStateList.valueOf(color)
    }

    private fun updateRepeatButton() {
        val colorActive = ContextCompat.getColor(this, R.color.icon_tint_active)
        val colorInactive = ContextCompat.getColor(this, R.color.icon_tint_inactive)

        when (repeatMode) {
            RepeatMode.OFF -> {
                repeatButton.setImageResource(R.drawable.ic_repeat)
                repeatButton.imageTintList = ColorStateList.valueOf(colorInactive)
            }
            RepeatMode.ALL -> {
                repeatButton.setImageResource(R.drawable.ic_repeat)
                repeatButton.imageTintList = ColorStateList.valueOf(colorActive)
            }
            RepeatMode.ONE -> {
                repeatButton.setImageResource(R.drawable.ic_repeat_one)
                repeatButton.imageTintList = ColorStateList.valueOf(colorActive)
            }
        }
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopUpdatingSeekBar()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    mediaPlayer?.seekTo(it.progress)
                    if (isPlaying) {
                        startUpdatingSeekBar()
                    }
                }
            }
        })
    }

    private fun displaySongInfo(song: Song) {
        currentSongTitle.text = song.title
        currentSongArtist.text = if (song.artist.isBlank() || song.artist == "<unknown>") {
            getString(R.string.unknown_artist)
        } else {
            song.artist
        }
        totalTime.text = formatTime(song.duration.toInt())

        // Reset art image
        playerAlbumArt.setImageResource(R.drawable.ic_music_note)
        playerAlbumArt.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.accent_color)
        )
        playerAlbumArt.setPadding(100, 100, 100, 100)

        // Load album artwork
        AlbumArtUtils.loadAlbumArtAsync(this, song) { bitmap ->
            if (bitmap != null) {
                playerAlbumArt.setImageBitmap(bitmap)
                playerAlbumArt.imageTintList = null
                playerAlbumArt.setPadding(0, 0, 0, 0)
            }
        }
    }

    private fun playSong(song: Song) {
        try {
            stopUpdatingSeekBar()
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, song.getUri())
                prepare()
                start()
            }

            isPlaying = true
            updatePlayPauseButton()
            setupMediaPlayerListeners()
            startUpdatingSeekBar()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to play audio file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMediaPlayerListeners() {
        mediaPlayer?.let { player ->
            seekBar.max = player.duration
            totalTime.text = formatTime(player.duration)

            player.setOnCompletionListener {
                when (repeatMode) {
                    RepeatMode.ONE -> {
                        currentSong?.let { playSong(it) }
                    }
                    RepeatMode.ALL -> {
                        playNextSong(auto = true)
                    }
                    RepeatMode.OFF -> {
                        if (currentPosition < playlist.size - 1 || isShuffleOn) {
                            playNextSong(auto = true)
                        } else {
                            isPlaying = false
                            updatePlayPauseButton()
                            stopUpdatingSeekBar()
                        }
                    }
                }
            }

            player.setOnErrorListener { _, _, _ ->
                Toast.makeText(this, "Playback error occurred", Toast.LENGTH_SHORT).show()
                playNextSong(auto = true)
                true
            }
        }
    }

    private fun pauseSong() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlayPauseButton()
        stopUpdatingSeekBar()
    }

    private fun resumeSong() {
        mediaPlayer?.let {
            it.start()
            isPlaying = true
            updatePlayPauseButton()
            startUpdatingSeekBar()
        } ?: run {
            currentSong?.let { playSong(it) }
        }
    }

    private fun playNextSong(auto: Boolean) {
        if (playlist.isEmpty()) return

        if (isShuffleOn && playlist.size > 1) {
            var nextPos = random.nextInt(playlist.size)
            if (nextPos == currentPosition) {
                nextPos = (currentPosition + 1) % playlist.size
            }
            currentPosition = nextPos
        } else {
            if (currentPosition < playlist.size - 1) {
                currentPosition++
            } else if (repeatMode == RepeatMode.ALL || !auto) {
                currentPosition = 0
            } else {
                return
            }
        }

        currentSong = playlist.getOrNull(currentPosition)
        currentSong?.let { song ->
            displaySongInfo(song)
            playSong(song)
        }
    }

    private fun playPreviousSong() {
        if (playlist.isEmpty()) return

        if (isShuffleOn && playlist.size > 1) {
            currentPosition = random.nextInt(playlist.size)
        } else {
            if (currentPosition > 0) {
                currentPosition--
            } else {
                currentPosition = playlist.size - 1
            }
        }

        currentSong = playlist.getOrNull(currentPosition)
        currentSong?.let { song ->
            displaySongInfo(song)
            playSong(song)
        }
    }

    private fun updatePlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause)
            playPauseButton.contentDescription = getString(R.string.pause)
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play_arrow)
            playPauseButton.contentDescription = getString(R.string.play)
        }
    }

    private fun startUpdatingSeekBar() {
        stopUpdatingSeekBar()
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        seekBar.progress = player.currentPosition
                        currentTime.text = formatTime(player.currentPosition)
                        handler.postDelayed(this, 500)
                    }
                }
            }
        }
        handler.post(updateSeekBarRunnable!!)
    }

    private fun stopUpdatingSeekBar() {
        updateSeekBarRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUpdatingSeekBar()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    @Suppress("DEPRECATION")
    private fun <T : Parcelable> Intent.getParcelableExtraCompat(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, clazz)
        } else {
            getParcelableExtra(key)
        }
    }

    @Suppress("DEPRECATION")
    private fun <T : Parcelable> Intent.getParcelableArrayListExtraCompat(key: String, clazz: Class<T>): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(key, clazz)
        } else {
            getParcelableArrayListExtra(key)
        }
    }
}
