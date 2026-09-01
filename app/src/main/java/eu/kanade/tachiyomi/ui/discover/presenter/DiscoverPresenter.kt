@file:Suppress("INVISIBLE_MEMBER") // androidx.compose.runtime.Immutable

package eu.kanade.tachiyomi.ui.discover.presenter

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.ui.discover.domain.ContentType
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryFilters
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryRepo

/**
 * UI-agnostic state model for the Discover screen.
 *
 * Built by the presenter by combining merged provider results with the active
 * filters/sort/search. Mirrors the immutable-state-in-presenter pattern used by
 * SourcesViewModel / ExtensionsViewModel: the presenter owns the state, the
 * Compose layer only renders it.
 */
data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val repos: List<DirectoryRepo> = emptyList(),
    val searchQuery: String? = null,
    val filters: DirectoryFilters = DirectoryFilters(),
    val sort: SortMode = SortMode.NAME,
    val hasProviders: Boolean = false,
    val enabledProviderKeys: Set<String> = emptySet(),
    val lastUpdatedAtByProvider: Map<String, kotlin.time.Instant?> = emptyMap(),
) {
    @Immutable
    sealed interface SortMode {
        data object NAME : SortMode
        data object COUNT : SortMode
    }
}

/**
 * Pure transformation helpers (unit-testable without Android).
 *
 * These are intentionally free functions so the ViewModel can derive its UI
 * state from the raw fetched lists without depending on Android types.
 */
object DiscoverPresenter {

    fun mergeRepos(vararg lists: List<DirectoryRepo>): List<DirectoryRepo> =
        lists.flatMap { it }.distinctBy { it.id }

    fun filterRepos(
        repos: List<DirectoryRepo>,
        query: String?,
        filters: DirectoryFilters,
    ): List<DirectoryRepo> {
        val q = query?.trim().orEmpty()
        return repos.filter { repo ->
            val queryMatch = if (q.isNotEmpty()) {
                repo.name.contains(q, ignoreCase = true) ||
                    repo.description.contains(q, ignoreCase = true) ||
                    repo.compatibleApps.any { it.contains(q, ignoreCase = true) } ||
                    repo.languages.any { it.contains(q, ignoreCase = true) }
            } else {
                true
            }
            val appMatch = filters.compatibleApps.isEmpty() ||
                filters.compatibleApps.any { it in repo.compatibleApps }
            val typeMatch = filters.contentTypes.isEmpty() ||
                filters.contentTypes.any { it in repo.contentTypes }
            val langMatch = filters.languages.isEmpty() ||
                filters.languages.any { it in repo.languages }
            val nsfwMatch = when (filters.nsfw) {
                DirectoryFilters.NsfwFilter.EXCLUDE_NSFW -> !repo.isNsfw
                DirectoryFilters.NsfwFilter.NSFW_ONLY -> repo.isNsfw
                DirectoryFilters.NsfwFilter.SHOW_ALL -> true
            }
            queryMatch && appMatch && typeMatch && langMatch && nsfwMatch
        }
    }

    fun sortRepos(
        repos: List<DirectoryRepo>,
        sort: DiscoverUiState.SortMode,
    ): List<DirectoryRepo> {
        val comparator = when (sort) {
            is DiscoverUiState.SortMode.NAME -> compareBy({ it.name })
            is DiscoverUiState.SortMode.COUNT -> compareBy({ -(it.extensionCount ?: Int.MIN_VALUE) }, { it.name })
        }
        return repos.sortedWith(comparator)
    }

    /** Apply search + filters + sort to produce the final visible list. */
    fun deriveView(
        repos: List<DirectoryRepo>,
        query: String?,
        filters: DirectoryFilters,
        sort: DiscoverUiState.SortMode,
    ): List<DirectoryRepo> =
        sortRepos(filterRepos(repos, query, filters), sort)

    /** Available app-compat filter chips (union across visible repos). */
    fun availableApps(repos: List<DirectoryRepo>): Set<String> =
        repos.flatMap { it.compatibleApps }.toSortedSet()

    /** Available language filter chips (union across visible repos). */
    fun availableLanguages(repos: List<DirectoryRepo>): Set<String> =
        repos.flatMap { it.languages }.toSortedSet()

    /** Available content-type filter chips (union across visible repos). */
    fun availableContentTypes(repos: List<DirectoryRepo>): Set<ContentType> =
        repos.flatMap { it.contentTypes }.toSortedSet()
}
