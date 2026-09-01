package eu.kanade.tachiyomi.ui.discover.presenter

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.ui.discover.data.DiscoverRepository
import eu.kanade.tachiyomi.ui.discover.domain.ContentType
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryFilters
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryRepo
import eu.kanade.tachiyomi.ui.discover.domain.ProviderCapabilities
import eu.kanade.tachiyomi.ui.discover.provider.ProviderRegistry
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class DiscoverViewModel(
    private val providerRegistry: ProviderRegistry,
    private val repository: DiscoverRepository,
) : ViewModel() {

    private val _events = Channel<Event>(Channel.CONFLATED)
    val events = _events.receiveAsFlow()

    private val searchQuery = MutableStateFlow<String?>(null)
    private val filters = MutableStateFlow(DirectoryFilters())
    private val sort = MutableStateFlow<DiscoverUiState.SortMode>(DiscoverUiState.SortMode.NAME)
    private val isRefreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val lastUpdatedAtByProvider = MutableStateFlow<Map<String, Instant?>>(emptyMap())

    /** Backing store of fully-fetched, deduplicated repos across all providers. */
    private val repos = MutableStateFlow<List<DirectoryRepo>>(emptyList())

    val providerCapabilities: Map<String, ProviderCapabilities> =
        providerRegistry.allProviders.associate { it.key to it.capabilities() }

    val state: StateFlow<DiscoverUiState> = combine(
        providerRegistry.enabledProvidersFlow,
        repos,
        searchQuery,
        filters,
        sort,
        isRefreshing,
        error,
        lastUpdatedAtByProvider,
    ) { providers, fetchedRepos, query, filter, sortMode, refreshing, err, lastUpdated ->
        val merged = DiscoverPresenter.mergeRepos(fetchedRepos)
        DiscoverUiState(
            isLoading = false,
            isRefreshing = refreshing,
            error = err,
            repos = DiscoverPresenter.deriveView(merged, query, filter, sortMode),
            searchQuery = query,
            filters = filter,
            sort = sortMode,
            hasProviders = providers.isNotEmpty(),
            enabledProviderKeys = providers.map { it.key }.toSet(),
            lastUpdatedAtByProvider = lastUpdated,
        )
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), DiscoverUiState(isLoading = true))

    init {
        // Initial load: hydrate from cache immediately, then attempt a remote refresh.
        viewModelScope.launchIO { refresh() }
    }

    fun setSearchQuery(query: String?) {
        searchQuery.update { query }
    }

    fun setAppFilter(apps: Set<String>) {
        filters.update { it.copy(compatibleApps = apps) }
    }

    fun setContentTypeFilter(types: Set<ContentType>) {
        filters.update { it.copy(contentTypes = types) }
    }

    fun setLanguageFilter(langs: Set<String>) {
        filters.update { it.copy(languages = langs) }
    }

    fun setNsfwFilter(nsfw: DirectoryFilters.NsfwFilter) {
        filters.update { it.copy(nsfw = nsfw) }
    }

    fun setSort(sort: DiscoverUiState.SortMode) {
        this.sort.update { sort }
    }

    fun setProviderEnabled(key: String, enabled: Boolean) {
        val provider = providerRegistry.getProvider(key) ?: return
        if (enabled) providerRegistry.enable(key) else providerRegistry.disable(key)
    }

    fun refresh(): Boolean {
        val providers = providerRegistry.enabledProviders
        if (providers.isEmpty()) return false
        launchRefresh(providers)
        return true
    }

    private fun launchRefresh(providers: List<eu.kanade.tachiyomi.ui.discover.domain.DirectoryProvider>) {
        isRefreshing.value = true
        error.value = null
        launchIO {
            val fetchedAt = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            val newLastUpdated = mutableMapOf<String, Instant?>()
            val all = mutableListOf<DirectoryRepo>()
            var success = true

            for (provider in providers) {
                // Hydrate cache first so offline mode has data immediately.
                val cached = repository.getCached(provider.key)
                if (cached.isNotEmpty()) {
                    newLastUpdated[provider.key] = repository.lastFetchedAt(provider.key)
                    all += cached
                }
            }

            for (provider in providers) {
                try {
                    val fresh = provider.list()
                    repository.cache(provider.key, fresh, fetchedAt)
                    newLastUpdated[provider.key] = fetchedAt
                    all.removeAll { it.providerKey == provider.key }
                    all += fresh
                } catch (e: Exception) {
                    success = false
                    newLastUpdated.putIfAbsent(provider.key, repository.lastFetchedAt(provider.key))
                }
            }

            lastUpdatedAtByProvider.update { newLastUpdated }
            repos.value = all.distinctBy { it.id }
            isRefreshing.value = false
            if (!success && all.isEmpty()) {
                error.value = "Failed to load directory entries"
            }
        }
    }

    @Immutable
    sealed interface Event {
        data object NoProvidersEnabled : Event
        data class ShowMessage(val message: String) : Event
    }
}
