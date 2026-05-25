package com.coworkapp.loopplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 트랙별 사용자/재생 메타데이터.
 *
 * @property favorite        즐겨찾기 여부
 * @property lastPracticedMs 마지막으로 재생을 시작한 시각 (epoch ms). null = 미연습
 * @property loops           누적 반복 횟수 (구간반복 1바퀴당 +1)
 */
@Serializable
data class TrackMetadata(
    val favorite: Boolean = false,
    val lastPracticedMs: Long? = null,
    val loops: Int = 0,
)

/**
 * SectionRepository 와 같은 DataStore 를 공유하고 "meta::<uri>" 키로 트랙별 메타 저장.
 *
 * Player·Library 양쪽이 같은 인스턴스를 안 써도 됨 (DataStore 가 프로세스-와이드).
 * 단, 같은 트랙에 동시 write 가 발생하면 read-modify-write 경합 가능 — 현재는 write
 * 지점이 분리돼있고(openTrack: touchPracticed / loopJob: incrementLoops / UI: setFavorite)
 * 같은 필드에 동시 쓰지 않으므로 안전.
 */
class TrackMetadataRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun keyFor(trackUri: String) = stringPreferencesKey("meta::$trackUri")

    fun observeAll(): Flow<Map<String, TrackMetadata>> =
        context.dataStore.data.map { prefs ->
            val out = mutableMapOf<String, TrackMetadata>()
            prefs.asMap().forEach { (key, value) ->
                val name = key.name
                if (name.startsWith("meta::") && value is String) {
                    val uri = name.removePrefix("meta::")
                    runCatching { json.decodeFromString<TrackMetadata>(value) }
                        .getOrNull()?.let { out[uri] = it }
                }
            }
            out
        }

    suspend fun getAll(): Map<String, TrackMetadata> = observeAll().first()

    private suspend fun get(uri: String): TrackMetadata {
        val raw = context.dataStore.data.first()[keyFor(uri)] ?: return TrackMetadata()
        return runCatching { json.decodeFromString<TrackMetadata>(raw) }
            .getOrDefault(TrackMetadata())
    }

    private suspend fun put(uri: String, meta: TrackMetadata) {
        context.dataStore.edit { prefs ->
            prefs[keyFor(uri)] = json.encodeToString(TrackMetadata.serializer(), meta)
        }
    }

    suspend fun setFavorite(uri: String, fav: Boolean) {
        put(uri, get(uri).copy(favorite = fav))
    }

    suspend fun touchPracticed(uri: String, atMs: Long = System.currentTimeMillis()) {
        put(uri, get(uri).copy(lastPracticedMs = atMs))
    }

    suspend fun incrementLoops(uri: String, by: Int = 1) {
        val cur = get(uri)
        put(uri, cur.copy(loops = cur.loops + by))
    }
}
