package com.example.musicplayer

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.util.Size
import java.io.InputStream
import java.util.concurrent.Executors

object AlbumArtUtils {

    private val cacheSize = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    private val memoryCache = object : LruCache<Long, Bitmap>(if (cacheSize > 0) cacheSize else 1024) {
        override fun sizeOf(key: Long, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun loadAlbumArtAsync(
        context: Context,
        song: Song,
        onLoaded: (Bitmap?) -> Unit
    ) {
        if (song.albumId != -1L) {
            val cached = memoryCache.get(song.albumId)
            if (cached != null) {
                onLoaded(cached)
                return
            }
        }

        executor.execute {
            val bitmap = getAlbumArt(context, song.albumId, song.getUri())
            if (bitmap != null && song.albumId != -1L) {
                memoryCache.put(song.albumId, bitmap)
            }
            mainHandler.post {
                onLoaded(bitmap)
            }
        }
    }

    private fun getAlbumArt(context: Context, albumId: Long, songUri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(songUri, Size(300, 300), null)
            } else {
                val artUri = Uri.parse("content://media/external/audio/albumart")
                val uri = ContentUris.withAppendedId(artUri, albumId)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }
}
