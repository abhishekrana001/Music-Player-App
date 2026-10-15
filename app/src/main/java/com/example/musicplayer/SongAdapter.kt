package com.example.musicplayer

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class SongAdapter(
    private var songs: List<Song>,
    private val onSongClick: (Song, Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songAlbumArt: ImageView = view.findViewById(R.id.songAlbumArt)
        val songTitle: TextView = view.findViewById(R.id.songTitle)
        val songArtist: TextView = view.findViewById(R.id.songArtist)
        val songDuration: TextView = view.findViewById(R.id.songDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.songTitle.text = song.title
        holder.songArtist.text = if (song.artist.isBlank() || song.artist == "<unknown>") {
            holder.itemView.context.getString(R.string.unknown_artist)
        } else {
            song.artist
        }
        holder.songDuration.text = formatDuration(song.duration)

        // Reset image default state
        holder.songAlbumArt.setImageResource(R.drawable.ic_music_note)
        holder.songAlbumArt.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(holder.itemView.context, R.color.accent_color)
        )
        holder.songAlbumArt.setPadding(30, 30, 30, 30)

        // Asynchronously load album art
        AlbumArtUtils.loadAlbumArtAsync(holder.itemView.context, song) { bitmap ->
            if (bitmap != null) {
                holder.songAlbumArt.setImageBitmap(bitmap)
                holder.songAlbumArt.imageTintList = null
                holder.songAlbumArt.setPadding(0, 0, 0, 0)
            }
        }

        holder.itemView.setOnClickListener {
            onSongClick(song, position)
        }
    }

    override fun getItemCount() = songs.size

    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
