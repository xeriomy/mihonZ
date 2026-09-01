package eu.kanade.tachiyomi.ui.discover.domain

import kotlin.time.Instant

/**
 * Content types that a directory repository may provide, as advertised by a
 * directory provider (e.g. Miyomi). This is *not* the app's internal source type
 * system; it is purely informational metadata surfaced in the Discover browser.
 */
enum class ContentType {
    MANGA,
    ANIME,
    NOVEL,
    LIGHT_NOVEL,
    MOVIES,
    TV_SHOWS,
    OTHER,
}

/**
 * Health status for an indexed extension repository.
 * - HEALTHY: updated within 45 days.
 * - STALE: updated between 45 and 180 days.
 * - BROKEN: updated more than 180 days ago.
 * - UNKNOWN: could not be determined.
 */
enum class HealthStatus {
    HEALTHY,
    STALE,
    BROKEN,
    UNKNOWN,
}

/**
 * Best-effort health snapshot for a directory repository, fetched in the
 * background and cached. Rendered as a colored dot + label.
 */
data class RepoHealth(
    val extensionCount: Int?,
    val lastUpdatedAt: Instant?, // best-effort, derived from GitHub Commits API
    val status: HealthStatus,
    val fetchedAt: Instant,
)

/**
 * A single extension repository as surfaced by a directory provider.
 *
 * [id] is stable and globally unique across providers: `providerKey + "/" + slug`.
 *
 * [repoUrl] is the URL of the extension repo index that Mihon consumes
 * (i.e. an `index.min.json` or `repo.json`). When null, one-tap add is disabled.
 *
 * [siteUrl] is the human-readable page for this repo entry on the provider site.
 *
 * [raw] carries provider-specific extras that the generic UI never parses.
 */
data class DirectoryRepo(
    val id: String,
    val providerKey: String,
    val name: String,
    val description: String,
    val compatibleApps: List<String>,
    val contentTypes: List<ContentType>,
    val languages: List<String>,
    val isNsfw: Boolean,
    val extensionCount: Int?,
    val repoUrl: String?,
    val siteUrl: String?,
    val raw: Map<String, String> = emptyMap(),
)

/**
 * Filter knobs surfaced by the Discover UI. A provider only needs to support
 * the filters it advertises via [ProviderCapabilities.supportedFilters].
 */
enum class FilterType {
    APP_COMPAT,
    CONTENT_TYPE,
    LANG,
    NSFW,
}

/**
 * Client-side active filters. Passed through to [DirectoryProvider.search]; providers
 * may ignore filters they don't support (see [FilterType]).
 */
data class DirectoryFilters(
    val compatibleApps: Set<String> = emptySet(),
    val contentTypes: Set<ContentType> = emptySet(),
    val languages: Set<String> = emptySet(),
    val nsfw: NsfwFilter = NsfwFilter.SHOW_ALL,
) {
    enum class NsfwFilter {
        SHOW_ALL,
        EXCLUDE_NSFW,
        NSFW_ONLY,
    }
}

/**
 * Capabilities advertised by a directory provider so the UI can adapt.
 */
data class ProviderCapabilities(
    val supportsSearch: Boolean,
    val supportsHealth: Boolean,
    val supportedFilters: Set<FilterType> = emptySet(),
)
