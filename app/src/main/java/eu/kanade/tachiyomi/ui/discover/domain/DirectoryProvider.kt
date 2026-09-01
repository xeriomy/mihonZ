package eu.kanade.tachiyomi.ui.discover.domain

/**
 * Pluggable directory provider abstraction.
 *
 * A `DirectoryProvider` discovers *extension repositories* (not individual
 * extensions) and reports best-effort health for them. The Discover feature
 * ships **zero** enabled providers by default — users explicitly enable one
 * (or more), and explicitly confirm every repository add.
 *
 * Implementations must be pure (no Android UI types) so they can be unit-tested
 * in isolation. Network access, when present, must reuse the app's OkHttp setup
 * and degrade gracefully to cached data on failure.
 *
 * Concurrency note: [list]/[search]/[detail]/[health] may be called concurrently;
 * implementations should be safe to call from any coroutine context.
 */
interface DirectoryProvider {
    /** Stable, namespaced id (e.g. "miyomi", "manifest"). Never changes. */
    val key: String

    /** Human-readable name shown to the user. */
    val displayName: String

    /** What can this provider do? Drives UI affordances. */
    fun capabilities(): ProviderCapabilities

    /**
     * Full listing of every repository this provider exposes.
     * Returns an empty list (not throws) if the source is unreachable and
     * no cached data is available.
     */
    suspend fun list(): List<DirectoryRepo>

    /**
     * Filtered search against the provider's remote source (or cached data when
     * offline). The UI additionally filters client-side, so a provider may
     * return a superset and let the UI narrow it further.
     */
    suspend fun search(
        query: String,
        filters: DirectoryFilters,
    ): List<DirectoryRepo>

    /** Detail view for a single repository by its [DirectoryRepo.id]. */
    suspend fun detail(id: String): DirectoryRepo

    /**
     * Best-effort health snapshot for [id]. Returns null if the provider cannot
     * determine health (the UI renders UNKNOWN).
     */
    suspend fun health(id: String): RepoHealth?
}
