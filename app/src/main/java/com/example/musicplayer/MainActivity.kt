package com.example.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var mainRootLayout: View
    private lateinit var headerLayout: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyViewContainer: LinearLayout
    private lateinit var emptyView: TextView
    private lateinit var grantPermissionButton: MaterialButton
    private lateinit var songCountText: TextView
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchButton: ImageView

    private lateinit var songAdapter: SongAdapter
    private val songsList = mutableListOf<Song>()
    private val filteredSongsList = mutableListOf<Song>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        initViews()
        setupEdgeToEdge()
        setupSearch()
        checkPermissions()
    }

    private fun initViews() {
        mainRootLayout = findViewById(R.id.mainRootLayout)
        headerLayout = findViewById(R.id.headerLayout)
        recyclerView = findViewById(R.id.recyclerView)
        emptyViewContainer = findViewById(R.id.emptyViewContainer)
        emptyView = findViewById(R.id.emptyView)
        grantPermissionButton = findViewById(R.id.grantPermissionButton)
        songCountText = findViewById(R.id.songCountText)
        searchEditText = findViewById(R.id.searchEditText)
        clearSearchButton = findViewById(R.id.clearSearchButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        songAdapter = SongAdapter(filteredSongsList) { song, position ->
            openPlayerActivity(song, position)
        }
        recyclerView.adapter = songAdapter

        grantPermissionButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(mainRootLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            headerLayout.setPadding(
                headerLayout.paddingLeft,
                systemBars.top + 16,
                headerLayout.paddingRight,
                headerLayout.paddingBottom
            )
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                systemBars.bottom + 16
            )
            insets
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSongs(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
        }
    }

    private fun filterSongs(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            clearSearchButton.visibility = View.GONE
            filteredSongsList.clear()
            filteredSongsList.addAll(songsList)
        } else {
            clearSearchButton.visibility = View.VISIBLE
            filteredSongsList.clear()
            for (song in songsList) {
                if (song.title.contains(trimmed, ignoreCase = true) ||
                    song.artist.contains(trimmed, ignoreCase = true) ||
                    song.album.contains(trimmed, ignoreCase = true)
                ) {
                    filteredSongsList.add(song)
                }
            }
        }
        updateUiState()
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        } else {
            loadSongs()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs()
            } else {
                showPermissionDeniedView()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If coming back from Settings, check permission again
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED && songsList.isEmpty()) {
            loadSongs()
        }
    }

    private fun loadSongs() {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        songsList.clear()
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: "Unknown Song"
                val artist = it.getString(artistColumn) ?: "Unknown Artist"
                val album = it.getString(albumColumn) ?: "Unknown Album"
                val albumId = it.getLong(albumIdColumn)
                val data = it.getString(dataColumn) ?: ""
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)

                songsList.add(Song(id, title, artist, album, albumId, data, duration, size))
            }
        }

        filterSongs(searchEditText.text.toString())
    }

    private fun updateUiState() {
        songCountText.text = getString(R.string.song_count_format, filteredSongsList.size)
        if (filteredSongsList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyViewContainer.visibility = View.VISIBLE
            emptyView.text = getString(R.string.no_songs_found)
            grantPermissionButton.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyViewContainer.visibility = View.GONE
        }
        songAdapter.updateSongs(filteredSongsList)
    }

    private fun showPermissionDeniedView() {
        recyclerView.visibility = View.GONE
        emptyViewContainer.visibility = View.VISIBLE
        emptyView.text = getString(R.string.permission_denied_msg)
        grantPermissionButton.visibility = View.VISIBLE
        songCountText.text = getString(R.string.song_count_format, 0)
    }

    private fun openPlayerActivity(song: Song, position: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("SONG", song)
            putExtra("POSITION", position)
            putParcelableArrayListExtra("PLAYLIST", ArrayList(filteredSongsList))
        }
        startActivity(intent)
    }
}
