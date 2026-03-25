package com.example.musicplayer

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class PlayerActivity : AppCompatActivity() {

    private lateinit var currentSongTitle: TextView
    private lateinit var currentSongArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var backButton: ImageButton

    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Song? = null
    private var currentPosition: Int = 0
    private var playlist: List<Song> = emptyList()
    private var isPlaying = false
    private var handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        enableEdgeToEdge()

        initViews()
        getIntentData()
        setupClickListeners()
        setupSeekBar()
    }

    private fun initViews() {
        currentSongTitle = findViewById(R.id.currentSongTitle)
        currentSongArtist = findViewById(R.id.currentSongArtist)
        seekBar = findViewById(R.id.seekBar)
        currentTime = findViewById(R.id.currentTime)
        totalTime = findViewById(R.id.totalTime)
        playPauseButton = findViewById(R.id.playPauseButton)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun getIntentData() {
        currentSong = intent.getParcelableExtra("SONG")
        currentPosition = intent.getIntExtra("POSITION", 0)
        playlist = intent.getParcelableArrayListExtra("PLAYLIST") ?: emptyList()

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
            playNextSong()
        }
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    currentTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun displaySongInfo(song: Song) {
        currentSongTitle.text = song.title
        currentSongArtist.text = song.artist.ifEmpty { getString(R.string.unknown_artist) }
        totalTime.text = formatTime(song.duration.toInt())
    }

    private fun playSong(song: Song) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(song.data)
                prepare()
                start()
            }

            isPlaying = true
            updatePlayPauseButton()
            setupMediaPlayerListeners()
            startUpdatingSeekBar()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun setupMediaPlayerListeners() {
        mediaPlayer?.setOnCompletionListener {
            playNextSong()
        }

        mediaPlayer?.let { player ->
            seekBar.max = player.duration
            totalTime.text = formatTime(player.duration)
        }
    }

    private fun pauseSong() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlayPauseButton()
        stopUpdatingSeekBar()
    }

    private fun resumeSong() {
        mediaPlayer?.start()
        isPlaying = true
        updatePlayPauseButton()
        startUpdatingSeekBar()
    }

    private fun playNextSong() {
        if (currentPosition < playlist.size - 1) {
            currentPosition++
            currentSong = playlist[currentPosition]
            currentSong?.let { song ->
                displaySongInfo(song)
                playSong(song)
            }
        }
    }

    private fun playPreviousSong() {
        if (currentPosition > 0) {
            currentPosition--
            currentSong = playlist[currentPosition]
            currentSong?.let { song ->
                displaySongInfo(song)
                playSong(song)
            }
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
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        seekBar.progress = player.currentPosition
                        currentTime.text = formatTime(player.currentPosition)
                        handler.postDelayed(this, 1000)
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
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        stopUpdatingSeekBar()
    }

    override fun onPause() {
        super.onPause()
        // Don't pause music when activity goes to background
    }
}