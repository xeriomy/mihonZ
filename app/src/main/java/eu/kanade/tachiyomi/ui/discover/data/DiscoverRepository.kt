package eu.kanade.tachiyomi.ui.discover.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.discover.domain.ContentType
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryRepo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Cache layer for directory-provider listing results.
 *
 * Persists raw [DirectoryRepo] payloads per provider in the `discover_cache`
 * SQLDelight table, keyed by (providerKey, repoId). Implements a 12-hour TTL
 * and exposes both cold reads and a hot Flow so the UI survives offline.
 *
 * This is the Discover layer's cache only; it does NOT touch upstream
 * extension-repo state (the `extension_store` table) — that stays under the
 * existing AddExtensionStore flow.
 */
@Inject
@SingleIn(AppScope::class)
class DiscoverRepository(
    private val database: Database,
    private val json: Json,
) {

    /** Preferred TTL for a cached listing. */
    internal val cacheTtl = 12.hours

    /**
     * Persists the given [repos] for [providerKey], stamped with the fetch time.
     * Clears prior rows for that provider first so stale entries cannot linger
     * after a structural schema change.
     */
    suspend fun cache(
        providerKey: String,
        repos: List<DirectoryRepo>,
        fetchedAt: Instant,
    ) {
        database.transaction {
            database.discover_cacheQueries.clearByProvider(providerKey)
            repos.forEach { repo ->
                val payload = json.encodeToString(DirectoryRepoSurrogate.serializer(), repo.toSurrogate())
                database.discover_cacheQueries.upsert(
                    providerKey = providerKey,
                    repoId = repo.id,
                    payload = payload,
                    fetchedAt = fetchedAt.toEpochMilliseconds(),
                )
            }
        }
    }

    /** Cold read of cached repos for [providerKey]. Empty on miss. */
    suspend fun getCached(providerKey: String): List<DirectoryRepo> {
        return database.discover_cacheQueries
            .getAll(providerKey)
            .executeAsList()
            .mapNotNull { row ->
                row.payload?.let { json.decodeFromString(DirectoryRepoSurrogate.serializer(), it).toDomain() }
            }
    }

    /** When the cache was last written for [providerKey]. */
    suspend fun lastFetchedAt(providerKey: String): Instant? {
        val rows = database.discover_cacheQueries.getAll(providerKey).executeAsList()
        if (rows.isEmpty()) return null
        return Instant.fromEpochMilliseconds(rows.maxOf { it.fetched_at })
    }

    /** Hot flow of cached repos for [providerKey] (replayed on subscribe). */
    fun getCachedAsFlow(providerKey: String) =
        database.discover_cacheQueries.getAll(providerKey).subscribeToList()
            .map { rows ->
                rows.mapNotNull { row ->
                    row.payload?.let {
                        json.decodeFromString(DirectoryRepoSurrogate.serializer(), it).toDomain()
                    }
                }
            }

    /** Whether the cached data for [providerKey] is within the TTL. */
    suspend fun isFresh(providerKey: String, now: Instant = Clock.System.now()): Boolean {
        val last = lastFetchedAt(providerKey) ?: return false
        return (now - last) < cacheTtl
    }

    /** Wipes everything (debug/testing only). */
    suspend fun clearAll() {
        database.discover_cacheQueries.clearAll()
    }

    /**
     * Stable serialized form of [DirectoryRepo], kept in the `payload` JSON column.
     * Kept as a separate type so the cache is resilient to future model changes.
     */
    @Serializable
    internal data class DirectoryRepoSurrogate(
        @SerialName("id") val id: String,
        @SerialName("providerKey") val providerKey: String,
        @SerialName("name") val name: String,
        @SerialName("description") val description: String,
        @SerialName("compatibleApps") val compatibleApps: List<String>,
        @SerialName("contentTypes") val contentTypes: List<String>,
        @SerialName("languages") val languages: List<String>,
        @SerialName("isNsfw") val isNsfw: Boolean,
        @SerialName("extensionCount") val extensionCount: Int? = null,
        @SerialName("repoUrl") val repoUrl: String? = null,
        @SerialName("siteUrl") val siteUrl: String? = null,
        @SerialName("raw") val raw: Map<String, String> = emptyMap(),
    ) {
        fun toDomain(): DirectoryRepo = DirectoryRepo(
            id = id,
            providerKey = providerKey,
            name = name,
            description = description,
            compatibleApps = compatibleApps,
            contentTypes = contentTypes.mapNotNull { runCatching { ContentType.valueOf(it) }.getOrNull() },
            languages = languages,
            isNsfw = isNsfw,
            extensionCount = extensionCount,
            repoUrl = repoUrl,
            siteUrl = siteUrl,
            raw = raw,
        )
    }
}

private fun DirectoryRepo.toSurrogate() = DiscoverRepository.DirectoryRepoSurrogate(
    id = id,
    providerKey = providerKey,
    name = name,
    description = description,
    compatibleApps = compatibleApps,
    contentTypes = contentTypes.map { it.name },
    languages = languages,
    isNsfw = isNsfw,
    extensionCount = extensionCount,
    repoUrl = repoUrl,
    siteUrl = siteUrl,
    raw = raw,
)
