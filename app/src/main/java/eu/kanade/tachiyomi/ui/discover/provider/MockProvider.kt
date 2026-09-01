package eu.kanade.tachiyomi.ui.discover.provider

import eu.kanade.tachiyomi.ui.discover.domain.ContentType
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryFilters
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryProvider
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryRepo
import eu.kanade.tachiyomi.ui.discover.domain.HealthStatus
import eu.kanade.tachiyomi.ui.discover.domain.ProviderCapabilities
import eu.kanade.tachiyomi.ui.discover.domain.RepoHealth
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A static, offline provider returning realistic entries modeled on Miyomi's
 * public repository listing. Used as the default mock for Phase 1 (no network).
 *
 * Provider key: "mock".
 */
class MockProvider : DirectoryProvider {

    override val key = "mock"

    override val displayName = "Mock directory (sample)"

    override fun capabilities(): ProviderCapabilities = ProviderCapabilities(
        supportsSearch = false,
        supportsHealth = false,
        supportedFilters = emptySet(),
    )

    private val now: Instant = Clock.System.now()

    private val repos: List<DirectoryRepo> = listOf(
        // Namespaced ids are providerKey + "/" + slug.
        dirRepo(
            "keiyoushi",
            name = "Keiyoushi",
            description = "A large curated collection of extensions for Mihon and compatible apps.",
            compatibleApps = listOf("mihon", "aniyomi", "cloudstream"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf(
                "en", "ja", "zh", "vi", "th", "id", "ko",
                "es", "pt", "ru", "fr", "de", "it", "pl", "ar", "tr", "uk",
            ),
            isNsfw = true,
            extensionCount = 356,
            repoUrl = "https://raw.githubusercontent.com/KEI991/Keiyoushi/main/index.min.json",
            siteUrl = "https://github.com/KEI991/Keiyoushi",
        ),
        dirRepo(
            "kavita",
            name = "Kavita",
            description = "Kavita extension repository.",
            compatibleApps = listOf("mihon", "cloudstream"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf("en"),
            isNsfw = false,
            extensionCount = 113,
            repoUrl = "https://raw.githubusercontent.com/SilentVoid13/Kavita/master/index.min.json",
            siteUrl = "https://github.com/SilentVoid13/Kavita",
        ),
        dirRepo(
            "lnreader-plugins",
            name = "LNReader Plugins",
            description = "Light novel reader extensions.",
            compatibleApps = listOf("lnreader"),
            contentTypes = listOf(ContentType.LIGHT_NOVEL, ContentType.NOVEL),
            languages = listOf("en", "ja", "zh", "vi", "id"),
            isNsfw = true,
            extensionCount = 40,
            repoUrl = "https://raw.githubusercontent.com/LNReader/lnreader-extensions/main/index.min.json",
            siteUrl = "https://github.com/LNReader/lnreader-extensions",
        ),
        dirRepo(
            "cloudstream-megarepo",
            name = "CloudStream MegaRepo",
            description = "All CloudStream extensions in one repo.",
            compatibleApps = listOf("cloudstream"),
            contentTypes = listOf(ContentType.MANGA, ContentType.ANIME, ContentType.MOVIES, ContentType.TV_SHOWS),
            languages = listOf("en", "ja", "zh", "es", "fr", "ru", "de"),
            isNsfw = true,
            extensionCount = 34,
            repoUrl = "https://raw.githubusercontent.com/LibreSpark-Community/LibreSpark-Plugins/main/index.min.json",
            siteUrl = "https://github.com/LibreSpark-Community/LibreSpark-Plugins",
        ),
        dirRepo(
            "aniyomi-compat",
            name = "Aniyomi Compatibility",
            description = "Extensions ported for Aniyomi compatibility.",
            compatibleApps = listOf("aniyomi"),
            contentTypes = listOf(ContentType.ANIME, ContentType.MANGA),
            languages = listOf("en", "es", "pt", "fr", "it"),
            isNsfw = false,
            extensionCount = 128,
            repoUrl = "https://raw.githubusercontent.com/aniyomiorg/aniyomi-extensions/main/index.min.json",
            siteUrl = "https://github.com/aniyomiorg/aniyomi-extensions",
        ),
        dirRepo(
            "aidoku-community-sources",
            name = "Aidoku Community Sources",
            description = "Community-maintained sources for Aidoku.",
            compatibleApps = listOf("aidoku"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf("en", "ja", "zh", "ko", "vi", "id"),
            isNsfw = true,
            extensionCount = 80,
            repoUrl = null, // Aidoku uses a different packaging; one-tap add disabled
            siteUrl = "https://github.com/rai-card/aide-extensions",
        ),
        dirRepo(
            "50-50-modules",
            name = "50/50's Modules",
            description = "Module collection by 50/50 for Mihon-compatible apps.",
            compatibleApps = listOf("mihon", "aniyomi"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf("en"),
            isNsfw = false,
            extensionCount = 22,
            repoUrl = "https://raw.githubusercontent.com/5050-5050/5050-modules/main/index.min.json",
            siteUrl = "https://github.com/5050-5050/5050-modules",
        ),
        dirRepo(
            "kohi-den",
            name = "Kohi-den",
            description = "Manga extensions focused on French and international scanlators.",
            compatibleApps = listOf("mihon"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf("fr", "en", "es", "pt"),
            isNsfw = true,
            extensionCount = 45,
            repoUrl = "https://raw.githubusercontent.com/Edent/Kohi-den/main/index.min.json",
            siteUrl = "https://github.com/Edent/Kohi-den",
        ),
        dirRepo(
            "hayase",
            name = "Hayase",
            description = "Extensions for the Hayase fork.",
            compatibleApps = listOf("hayase"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf("en", "ja"),
            isNsfw = false,
            extensionCount = 38,
            repoUrl = "https://raw.githubusercontent.com/Hayase-Mochi/Hayase-extensions/main/index.min.json",
            siteUrl = "https://github.com/Hayase-Mochi/Hayase-extensions",
        ),
        dirRepo(
            "dantotsu-novel-extensions",
            name = "Dantotsu Novel Extensions",
            description = "Light novel and novel sources for Dantotsu.",
            compatibleApps = listOf("dantotsu"),
            contentTypes = listOf(ContentType.NOVEL, ContentType.LIGHT_NOVEL),
            languages = listOf("en", "ja", "zh", "vi"),
            isNsfw = true,
            extensionCount = 60,
            repoUrl = "https://raw.githubusercontent.com/dantotsu/dantotsu-extensions/main/index.min.json",
            siteUrl = "https://github.com/dantotsu/dantotsu-extensions",
        ),
        dirRepo(
            "tachiyomi-lib",
            name = "Tachiyomi Libre",
            description = "Community-run continuation extensions for Tachiyomi-based apps.",
            compatibleApps = listOf("mihon", "tachiyomi", "aniyomi"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf("en", "es", "pt", "fr", "ru"),
            isNsfw = true,
            extensionCount = 175,
            repoUrl = "https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/master/index.min.json",
            siteUrl = "https://github.com/tachiyomiorg/tachiyomi-extensions",
        ),
        dirRepo(
            "mangadex",
            name = "MangaDex",
            description = "The official MangaDex extension.",
            compatibleApps = listOf("mihon", "aniyomi", "tachiyomi"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf("en"),
            isNsfw = true,
            extensionCount = 1,
            repoUrl = "https://raw.githubusercontent.com/mangadexorg/mangadex-ext-devcenter/main/index.min.json",
            siteUrl = "https://github.com/mangadexorg/mangadex-ext-devcenter",
        ),
        dirRepo(
            "paperback-retur",
            name = "Paperback Retur",
            description = "Paperback-compatible sources re-packaged for Mihon.",
            compatibleApps = listOf("mihon", "paperback", "kotatsu"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf("en", "ja", "zh", "ko", "vi", "id", "th"),
            isNsfw = true,
            extensionCount = 90,
            repoUrl = null,
            siteUrl = "https://github.com/RetroMusicPlayer/RetroMusicExtensions",
        ),
        dirRepo(
            "kotatsu-extensions",
            name = "Kotatsu Sources",
            description = "Kotatsu-compatible extension repository.",
            compatibleApps = listOf("kotatsu", "cloudstream"),
            contentTypes = listOf(ContentType.MANGA, ContentType.ANIME),
            languages = listOf("en", "ja", "ru", "uk"),
            isNsfw = false,
            extensionCount = 52,
            repoUrl = "https://raw.githubusercontent.com/KotatsuApp/extensions/main/index.min.json",
            siteUrl = "https://github.com/KotatsuApp/extensions",
        ),
        dirRepo(
            "mangsx",
            name = "MangSX",
            description = "Extensions for adult-oriented manga sources.",
            compatibleApps = listOf("mihon"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf("en"),
            isNsfw = true,
            extensionCount = 64,
            repoUrl = "https://raw.githubusercontent.com/SuperPrefs/MangSX/main/index.min.json",
            siteUrl = "https://github.com/SuperPrefs/MangSX",
        ),
        dirRepo(
            "bilingual-hevc",
            name = "Bilingual HEVC",
            description = "Anime sources with bilingual/subtitled metadata.",
            compatibleApps = listOf("aniyomi"),
            contentTypes = listOf(ContentType.ANIME),
            languages = listOf("en", "ja"),
            isNsfw = true,
            extensionCount = 33,
            repoUrl = null,
            siteUrl = "https://github.com/rahul-mondal-25/bilingual-hevc",
        ),
        dirRepo(
            "manga-updates",
            name = "Manga Updates Info",
            description = "Extensions that track MangaUpdates metadata.",
            compatibleApps = listOf("mihon", "tachiyomi"),
            contentTypes = listOf(ContentType.MANGA),
            languages = listOf("en"),
            isNsfw = false,
            extensionCount = 18,
            repoUrl = "https://raw.githubusercontent.com/Manga-Updates-Extensions/mangadex-info/main/index.min.json",
            siteUrl = "https://github.com/Manga-Updates-Extensions",
        ),
        dirRepo(
            "comic-extra",
            name = "Comic Extra",
            description = "Western comic and movie sources.",
            compatibleApps = listOf("mihon", "cloudstream"),
            contentTypes = listOf(ContentType.MOVIES, ContentType.TV_SHOWS, ContentType.MANGA),
            languages = listOf("en"),
            isNsfw = false,
            extensionCount = 27,
            repoUrl = "https://raw.githubusercontent.com/Comet-Extensions/comic-extra/main/index.min.json",
            siteUrl = "https://github.com/Comet-Extensions/comic-extra",
        ),
        dirRepo(
            "novel-hawk",
            name = "Novel Hawk",
            description = "Light novel sources aggregator.",
            compatibleApps = listOf("lnreader", "dantotsu"),
            contentTypes = listOf(ContentType.NOVEL, ContentType.LIGHT_NOVEL),
            languages = listOf("en", "es", "pt"),
            isNsfw = true,
            extensionCount = 55,
            repoUrl = "https://raw.githubusercontent.com/Novela-Org/novel-hawk/main/index.min.json",
            siteUrl = "https://github.com/Novela-Org/novel-hawk",
        ),
        dirRepo(
            "hentai-ring",
            name = "Hentai Ring",
            description = "Adult-oriented manga and video sources.",
            compatibleApps = listOf("mihon"),
            contentTypes = listOf(ContentType.MANGA, ContentType.MOVIES),
            languages = listOf("en", "ja"),
            isNsfw = true,
            extensionCount = 77,
            repoUrl = "https://raw.githubusercontent.com/hentai-ring/hentai-extensions/main/index.min.json",
            siteUrl = "https://github.com/hentai-ring/hentai-extensions",
        ),
    )

    override suspend fun list(): List<DirectoryRepo> = repos

    override suspend fun search(query: String, filters: DirectoryFilters): List<DirectoryRepo> {
        // Mock ignores remote search; the UI filters client-side anyway.
        return repos.filter { matches(it, query, filters) }
    }

    override suspend fun detail(id: String): DirectoryRepo {
        return repos.find { it.id == id }
            ?: throw NoSuchElementException("Unknown repo id: $id")
    }

    override suspend fun health(id: String): RepoHealth? = null

    private fun matches(repo: DirectoryRepo, query: String, filters: DirectoryFilters): Boolean {
        val q = query.trim()
        if (q.isNotEmpty() &&
            !repo.name.contains(q, ignoreCase = true) &&
            !repo.description.contains(q, ignoreCase = true)
        ) {
            return false
        }
        if (filters.compatibleApps.isNotEmpty() &&
            filters.compatibleApps.none { it in repo.compatibleApps }
        ) {
            return false
        }
        if (filters.contentTypes.isNotEmpty() &&
            filters.contentTypes.none { it in repo.contentTypes }
        ) {
            return false
        }
        if (filters.languages.isNotEmpty() &&
            filters.languages.none { it in repo.languages }
        ) {
            return false
        }
        when (filters.nsfw) {
            DirectoryFilters.NsfwFilter.EXCLUDE_NSFW -> if (repo.isNsfw) return false
            DirectoryFilters.NsfwFilter.NSFW_ONLY -> if (!repo.isNsfw) return false
            else -> Unit
        }
        return true
    }

    private fun dirRepo(
        slug: String,
        name: String,
        description: String,
        compatibleApps: List<String>,
        contentTypes: List<ContentType>,
        languages: List<String>,
        isNsfw: Boolean,
        extensionCount: Int?,
        repoUrl: String?,
        siteUrl: String?,
    ): DirectoryRepo = DirectoryRepo(
        id = "$key/$slug",
        providerKey = key,
        name = name,
        description = description,
        compatibleApps = compatibleApps,
        contentTypes = contentTypes,
        languages = languages,
        isNsfw = isNsfw,
        extensionCount = extensionCount,
        repoUrl = repoUrl,
        siteUrl = siteUrl,
        raw = emptyMap(),
    )
}
