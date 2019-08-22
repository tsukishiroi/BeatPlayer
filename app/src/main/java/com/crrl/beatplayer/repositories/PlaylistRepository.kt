package com.crrl.beatplayer.repositories

import android.content.*
import android.database.Cursor
import android.os.RemoteException
import android.provider.BaseColumns._ID
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
import android.provider.MediaStore.Audio.Playlists.Members.AUDIO_ID
import android.provider.MediaStore.Audio.Playlists.Members.PLAY_ORDER
import android.provider.MediaStore.Audio.PlaylistsColumns.NAME
import com.crrl.beatplayer.extensions.toList
import com.crrl.beatplayer.models.Playlist
import com.crrl.beatplayer.models.Song
import com.crrl.beatplayer.utils.SettingsUtility

class PlaylistRepository() {


    private lateinit var contentResolver: ContentResolver
    private lateinit var settingsUtility: SettingsUtility

    companion object {
        private var instance: PlaylistRepository? = null

        fun getInstance(context: Context?): PlaylistRepository? {
            if (instance == null) {
                instance = PlaylistRepository(context)
            }
            return instance
        }
    }

    constructor(context: Context?) : this() {
        contentResolver = context!!.contentResolver
        settingsUtility = SettingsUtility.getInstance(context)
    }


    @Throws(IllegalStateException::class)
    fun createPlaylist(name: String?): Long {
        if (name.isNullOrEmpty()) {
            return -1
        }
        val projection = arrayOf(NAME)
        val selection = "$NAME = ?"
        val selectionArgs = arrayOf(name)

        return contentResolver.query(
            EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use {
            return if (it.count <= 0) {
                val values = ContentValues(1).apply {
                    put(NAME, name)
                }
                contentResolver.insert(EXTERNAL_CONTENT_URI, values)?.lastPathSegment?.toLong()
                    ?: -1
            } else {
                -1
            }
        }
            ?: throw IllegalStateException("Unable to query $EXTERNAL_CONTENT_URI, because system returned null.")
    }

    fun getPlaylists(): List<Playlist> {
        return makePlaylistCursor().toList(true) {
            val id: Long = getLong(0)
            val songCount = getSongCountForPlaylist(id)
            Playlist.fromCursor(this, songCount)
        }.filter { it.name.isNotEmpty() }
    }

    fun addToPlaylist(playlistId: Long, ids: LongArray): Int {
        val projection = arrayOf("max($PLAY_ORDER)")
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)

        val base: Int = contentResolver.query(uri, projection, null, null, null)?.use {
            if (it.moveToFirst()) {
                it.getInt(0) + 1
            } else {
                0
            }
        } ?: throw IllegalStateException("Unable to query $uri, system returned null.")

        var numInserted = 0
        var offset = 0
        while (offset < ids.size) {
            val bulkValues = makeInsertItems(ids, offset, 1000, base)
            numInserted += contentResolver.bulkInsert(uri, bulkValues)
            offset += 1000
        }

        return numInserted
    }

    fun getSongsInPlaylist(playlistId: Long): List<Song> {
        val playlistCount = countPlaylist(playlistId)

        makePlaylistSongCursor(playlistId)?.use {
            var runCleanup = false
            if (it.count != playlistCount) {
                runCleanup = true
            }

            if (!runCleanup && it.moveToFirst()) {
                val playOrderCol =
                    it.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.PLAY_ORDER)
                var lastPlayOrder = -1
                do {
                    val playOrder = it.getInt(playOrderCol)
                    if (playOrder == lastPlayOrder) {
                        runCleanup = true
                        break
                    }
                    lastPlayOrder = playOrder
                } while (it.moveToNext())
            }

            if (runCleanup) {
                cleanupPlaylist(playlistId, it, true)
            }
        }

        return makePlaylistSongCursor(playlistId)
            .toList(true, Song.Companion::createFromPlaylistCursor)
    }

    fun removeFromPlaylist(playlistId: Long, id: Long) {
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        contentResolver.delete(
            uri,
            "$AUDIO_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun deletePlaylist(playlistId: Long): Int {
        val localUri = EXTERNAL_CONTENT_URI
        val localStringBuilder = StringBuilder().apply {
            append("_id IN (")
            append(playlistId)
            append(")")
        }
        return contentResolver.delete(localUri, localStringBuilder.toString(), null)
    }

    private fun makePlaylistCursor(): Cursor? {
        return contentResolver.query(
            EXTERNAL_CONTENT_URI,
            arrayOf(_ID, NAME), null, null, MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER
        )
    }

    private fun getSongCountForPlaylist(playlistId: Long): Int {
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        return contentResolver.query(
            uri,
            arrayOf(_ID),
            "${MediaStore.Audio.AudioColumns.IS_MUSIC}=1 AND ${MediaStore.Audio.AudioColumns.TITLE} != ''",
            null,
            null
        )?.use {
            if (it.moveToFirst()) {
                it.count
            } else {
                0
            }
        } ?: 0
    }

    private fun cleanupPlaylist(
        playlistId: Long,
        cursor: Cursor,
        closeCursorAfter: Boolean
    ) {
        val idCol = cursor.getColumnIndexOrThrow(AUDIO_ID)
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        val ops = arrayListOf<ContentProviderOperation>().apply {
            add(ContentProviderOperation.newDelete(uri).build())
        }

        if (cursor.moveToFirst() && cursor.count > 0) {
            do {
                val builder = ContentProviderOperation.newInsert(uri)
                    .withValue(PLAY_ORDER, cursor.position)
                    .withValue(AUDIO_ID, cursor.getLong(idCol))
                if ((cursor.position + 1) % 100 == 0) {
                    builder.withYieldAllowed(true)
                }
                ops.add(builder.build())
            } while (cursor.moveToNext())
        }

        try {
            contentResolver.applyBatch(MediaStore.AUTHORITY, ops)
        } catch (e: RemoteException) {
        } catch (e: OperationApplicationException) {
        }

        if (closeCursorAfter) {
            cursor.close()
        }
    }

    private fun countPlaylist(playlistId: Long): Int {
        return contentResolver.query(
            MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
            arrayOf(AUDIO_ID),
            null,
            null,
            MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER
        )?.use {
            if (it.moveToFirst()) {
                it.count
            } else {
                0
            }
        } ?: 0
    }

    private fun makePlaylistSongCursor(playlistID: Long?): Cursor? {
        val selection = StringBuilder().apply {
            append("${MediaStore.Audio.AudioColumns.IS_MUSIC}=1")
            append(" AND ${MediaStore.Audio.AudioColumns.TITLE} != ''")
        }
        val projection = arrayOf(
            "play_order",
            "_id",
            "title",
            "artist",
            "album",
            "duration",
            "track",
            "artist_id",
            "album_id",
            "_data"
        )
        return contentResolver.query(
            MediaStore.Audio.Playlists.Members.getContentUri("external", playlistID!!),
            projection,
            selection.toString(),
            null,
            MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER
        )
    }

    private fun makeInsertItems(
        ids: LongArray,
        offset: Int,
        len: Int,
        base: Int
    ): Array<ContentValues> {
        var actualLen = len
        if (offset + actualLen > ids.size) {
            actualLen = ids.size - offset
        }
        val contentValuesList = mutableListOf<ContentValues>()
        for (i in 0 until actualLen) {
            val values = ContentValues().apply {
                put(PLAY_ORDER, base + offset + i)
                put(AUDIO_ID, ids[offset + i])
            }
            contentValuesList.add(values)
        }
        return contentValuesList.toTypedArray()
    }
}