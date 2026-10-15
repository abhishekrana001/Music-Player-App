package com.example.musicplayer

import android.content.ContentUris
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    val albumId: Long = -1L,
    val data: String = "",
    val duration: Long = 0L,
    val size: Long = 0L
) : Parcelable {
    fun getUri(): Uri {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }
}
