package com.coworkapp.loopplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * 트랙별 구간 정보를 DataStore에 영구 저장하는 저장소
 * 단순화를 위해 트랙 URI를 key, JSON 직렬화한 LoopSection 리스트를 value로 사용
 */
private val Context.dataStore by preferencesDataStore(name = "loop_player_prefs")

class SectionRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun keyFor(trackUri: String) = stringPreferencesKey("sections::$trackUri")

    /**
     * 특정 트랙의 구간들을 Flow로 관찰
     */
    fun observeSections(trackUri: String): Flow<List<LoopSection>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[keyFor(trackUri)] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<LoopSection>>(raw) }
                .getOrDefault(emptyList())
        }

    /**
     * 특정 트랙의 구간들을 한 번 읽어오기
     */
    suspend fun getSections(trackUri: String): List<LoopSection> =
        observeSections(trackUri).first()

    /**
     * 특정 트랙의 구간들 전체 저장 (덮어쓰기)
     */
    suspend fun saveSections(trackUri: String, sections: List<LoopSection>) {
        context.dataStore.edit { prefs ->
            prefs[keyFor(trackUri)] = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(LoopSection.serializer()),
                sections
            )
        }
    }
}
