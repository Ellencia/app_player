package com.coworkapp.loopplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 사용자 플레이리스트를 DataStore 에 영구 저장.
 *
 * SectionRepository 와는 별도의 DataStore 인스턴스 사용 (별도 prefs 파일).
 */
private val Context.playlistsDataStore by preferencesDataStore(name = "playlists_prefs")

class PlaylistRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("playlists::all")

    fun observe(): Flow<List<Playlist>> =
        context.playlistsDataStore.data.map { prefs ->
            val raw = prefs[key] ?: return@map emptyList()
            runCatching {
                json.decodeFromString(ListSerializer(Playlist.serializer()), raw)
            }.getOrDefault(emptyList())
        }

    suspend fun getAll(): List<Playlist> = observe().first()

    suspend fun saveAll(playlists: List<Playlist>) {
        context.playlistsDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(
                ListSerializer(Playlist.serializer()),
                playlists,
            )
        }
    }
}
