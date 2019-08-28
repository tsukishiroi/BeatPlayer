package com.crrl.beatplayer.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.crrl.beatplayer.extensions.toList
import com.crrl.beatplayer.models.Album
import com.crrl.beatplayer.models.Song
import com.crrl.beatplayer.utils.PlayerConstants
import com.crrl.beatplayer.utils.SettingsUtility
import com.crrl.beatplayer.utils.SortModes


class AlbumRepository() {

    private lateinit var contentResolver: ContentResolver
    private lateinit var settingsUtility: SettingsUtility

    val currentAlbumList: List<Album>
        get() = getAlbums()

    companion object {
        private var instance: AlbumRepository? = null

        fun getInstance(context: Context?): AlbumRepository? {
            if (instance == null) instance = AlbumRepository(context)
            return instance
        }
    }

    constructor(context: Context? = null) : this() {
        contentResolver = context!!.contentResolver
        settingsUtility = SettingsUtility.getInstance(context)
    }

    fun getAlbum(id: Long): Album {
        return getAlbum(makeAlbumCursor("_id=?", arrayOf(id.toString())))
    }

    private fun getAlbum(cursor: Cursor?): Album {
        return cursor?.use {
            if (cursor.moveToFirst()) {
                Album.createFromCursor(cursor)
            } else {
                null
            }
        } ?: Album()
    }

    fun getSongsForAlbum(albumId: Long): List<Song> {
        val list = makeAlbumSongCursor(albumId)
            .toList(true) { Song.createFromCursor(this, albumId) }
        SortModes.sortAlbumSongList(list)
        return list
    }

    private fun getAlbums(): List<Album> {
        val sl = makeAlbumCursor(null, null)
            .toList(true) { Album.createFromCursor(this) }
        SortModes.sortAlbumList(sl, settingsUtility.albumSortOrder)
        return sl
    }

    fun search(paramString: String, limit: Int): List<Album> {
        val result = makeAlbumCursor("album LIKE ?", arrayOf("$paramString%"))
            .toList(true) { Album.createFromCursor(this) }
        if (result.size < limit) {
            val moreResults = makeAlbumCursor("album LIKE ?", arrayOf("%_$paramString%"))
                .toList(true) { Album.createFromCursor(this) }
            result += moreResults
        }
        return if (result.size < limit) {
            result
        } else {
            result.subList(0, limit)
        }
    }

    private fun makeAlbumCursor(selection: String?, paramArrayOfString: Array<String>?): Cursor? {
        return contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            arrayOf("_id", "album", "artist", "artist_id", "numsongs", "minyear"),
            selection,
            paramArrayOfString,
            settingsUtility.albumSortOrder
        )
    }

    private fun makeAlbumSongCursor(albumID: Long): Cursor? {
        val selection = "is_music=1 AND title != '' AND album_id=$albumID"
        return contentResolver.query(
            PlayerConstants.SONG_URI,
            arrayOf("_id", "title", "artist", "album", "duration", "track", "artist_id", "_data"),
            selection,
            null,
            ""
        )
    }
}