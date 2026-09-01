package eu.kanade.tachiyomi.ui.discover.presenter

import eu.kanade.tachiyomi.ui.discover.domain.ContentType
import io.kotest.matchers.lists.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DiscoverPresenterTest {

    private fun repo(
        name: String,
        pkg: String,
        languages: List<String> = listOf("en"),
        contentTypes: List<ContentType> = listOf(ContentType.MANGA),
        appCompat: List<String> = listOf("mihon"),
        isNsfw: Boolean = false,
        extCount: Int? = null,
        siteUrl: String? = "https://example.com/$pkg",
    ) = DirectoryRepo(
        id = "$pkg/$name",
        providerKey = "mock",
        name = name,
        description = "desc for $name",
        compatibleApps = appCompat,
        contentTypes = contentTypes,
        languages = languages,
        isNsfw = isNsfw,
        extensionCount = extCount,
        repoUrl = "https://example.com/$pkg/index.min.json",
        siteUrl = siteUrl,
    )

    @Test
    fun `empty list returns as-is`() {
        val result = DiscoverPresenter.deriveView(
            repos = emptyList(),
            query = "",
            filters = DirectoryFilters(),
            sort = DiscoverUiState.SortMode.NAME,
        )
        result shouldHaveSize 0
    }

    @Test
    fun `search filters case-insensitive by name and pkg`() {
        val repos = listOf(
            repo("Keiyoushi", "keiyoushi"),
            repo("CloudStream MegaRepo", "cloudstream"),
            repo("Kavita", "kavita", languages = listOf("ja"), contentTypes = listOf(ContentType.ANIME)),
        )

        DiscoverPresenter.filterRepos(repos, "keiy", DirectoryFilters()) shouldBe listOf(repos[0])
        DiscoverPresenter.filterRepos(repos, "KAVITA", DirectoryFilters()) shouldBe listOf(repos[2])
        DiscoverPresenter.filterRepos(repos, "CLOUD", DirectoryFilters()) shouldBe listOf(repos[1])
    }

    @Test
    fun `app-compat filter keeps matching repos`() {
        val repos = listOf(
            repo("A", "a", appCompat = listOf("mihon", "aniyomi")),
            repo("B", "b", appCompat = listOf("cloudstream")),
            repo("C", "c", appCompat = listOf("mihon")),
        )

        val result = DiscoverPresenter.filterRepos(
            repos,
            null,
            DirectoryFilters(compatibleApps = setOf("mihon")),
        )
        result.map { it.name } shouldBe listOf("A", "C")
    }

    @Test
    fun `content-type filter keeps matching repos`() {
        val repos = listOf(
            repo("A", "a", contentTypes = listOf(ContentType.MANGA)),
            repo("B", "b", contentTypes = listOf(ContentType.ANIME)),
            repo("C", "c", contentTypes = listOf(ContentType.MANGA, ContentType.LIGHT_NOVEL)),
        )

        val result = DiscoverPresenter.filterRepos(
            repos,
            null,
            DirectoryFilters(contentTypes = setOf(ContentType.MANGA)),
        )
        result.map { it.name } shouldBe listOf("A", "C")
    }

    @Test
    fun `language filter keeps matching repos`() {
        val repos = listOf(
            repo("A", "a", languages = listOf("en")),
            repo("B", "b", languages = listOf("ja")),
            repo("C", "c", languages = listOf("en", "ja")),
        )

        val result = DiscoverPresenter.filterRepos(
            repos,
            null,
            DirectoryFilters(languages = setOf("ja")),
        )
        result.map { it.name } shouldBe listOf("B", "C")
    }

    @Test
    fun `nsfw filter EXCLUDE_NSFW removes nsfw repos`() {
        val repos = listOf(
            repo("A", "a", isNsfw = false),
            repo("B", "b", isNsfw = true),
        )
        DiscoverPresenter.filterRepos(
            repos,
            null,
            DirectoryFilters(nsfw = DirectoryFilters.NsfwFilter.EXCLUDE_NSFW),
        ) shouldBe listOf(repos[0])
    }

    @Test
    fun `nsfw filter NSFW_ONLY keeps only nsfw repos`() {
        val repos = listOf(
            repo("A", "a", isNsfw = false),
            repo("B", "b", isNsfw = true),
        )
        DiscoverPresenter.filterRepos(
            repos,
            null,
            DirectoryFilters(nsfw = DirectoryFilters.NsfwFilter.NSFW_ONLY),
        ) shouldBe listOf(repos[1])
    }

    @Test
    fun `sort by name is alphabetical case-insensitive`() {
        val repos = listOf(
            repo("zebra", "z"),
            repo("apple", "a"),
            repo("Banana", "b"),
        )
        val result = DiscoverPresenter.sortRepos(repos, DiscoverUiState.SortMode.NAME)
        result.map { it.name } shouldBe listOf("apple", "Banana", "zebra")
    }

    @Test
    fun `sort by count descending puts larger counts first`() {
        val repos = listOf(
            repo("A", "a", extCount = 5),
            repo("B", "b", extCount = 100),
            repo("C", "c", extCount = null),
            repo("D", "d", extCount = 50),
        )
        val result = DiscoverPresenter.sortRepos(repos, DiscoverUiState.SortMode.COUNT)
        result.map { it.name } shouldBe listOf("B", "D", "A", "C")
    }

    @Test
    fun `mergeRepos deduplicates by id`() {
        val a = repo("A", "a", extCount = 10)
        val b = repo("B", "b")
        val aDup = repo("A", "a", extCount = null)
        val merged = DiscoverPresenter.mergeRepos(a, b, aDup)
        merged.map { it.id } shouldBe listOf("a/A", "b/B")
    }

    @Test
    fun `available apps returns union of compat lists`() {
        val repos = listOf(
            repo("A", "a", appCompat = listOf("mihon", "aniyomi")),
            repo("B", "b", appCompat = listOf("cloudstream")),
        )
        DiscoverPresenter.availableApps(repos) shouldBe setOf("aniyomi", "cloudstream", "mihon")
    }

    @Test
    fun `available languages returns union of language lists`() {
        val repos = listOf(
            repo("A", "a", languages = listOf("en", "ja")),
            repo("B", "b", languages = listOf("de", "ja")),
        )
        DiscoverPresenter.availableLanguages(repos) shouldBe setOf("de", "en", "ja")
    }

    @Test
    fun `empty search query returns all repos`() {
        val repos = List(5) { repo("repo$it", "pkg$it") }
        val result = DiscoverPresenter.filterRepos(repos, "", DirectoryFilters())
        result shouldHaveSize 5
    }

    @Test
    fun `null search query returns all repos`() {
        val repos = List(5) { repo("repo$it", "pkg$it") }
        val result = DiscoverPresenter.filterRepos(repos, null, DirectoryFilters())
        result shouldHaveSize 5
    }
}
