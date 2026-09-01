package eu.kanade.tachiyomi.ui.discover.provider

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.discover.DiscoverPreferences
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Holds all available directory providers and resolves the subset the user has
 * explicitly enabled (empty on a fresh install — the fork ships no default
 * enabled provider).
 *
 * Providers are registered statically here. Adding provider #2 later is a
 * matter of appending an instance to [allProviders] (or, in Phase 5, adding a
 * dynamic manifest-backed provider) — no UI changes required.
 *
 * @param preferences Injected; owns the enabled-provider-keys preference so the
 *   key set is persisted across process death and survives app updates.
 */
@Inject
@SingleIn(AppScope::class)
class ProviderRegistry(
    private val preferences: DiscoverPreferences,
) {

    /** Every provider this build knows about (order = display order). */
    val allProviders: List<DirectoryProvider> = listOf(
        MockProvider(),
    )

    /** Enabled provider keys as a cold preference value. */
    val enabledProviderKeys: Set<String>
        get() = preferences.enabledProviderKeys.get()

    /** Hot flow of enabled provider keys. */
    val enabledProviderKeysFlow: Flow<Set<String>> = preferences.enabledProviderKeys.changes()

    /** The enabled providers, resolved by key. */
    val enabledProviders: List<DirectoryProvider>
        get() = allProviders.filter { it.key in preferences.enabledProviderKeys.get() }

    /** Flow of enabled providers. */
    val enabledProvidersFlow: Flow<List<DirectoryProvider>> =
        enabledProviderKeysFlow.map { keys -> allProviders.filter { it.key in keys } }

    fun isEnabled(key: String): Boolean = key in preferences.enabledProviderKeys.get()

    /** Explicit opt-in. Returns the resulting set. */
    fun enable(key: String): Set<String> {
        val current = preferences.enabledProviderKeys.get().toMutableSet()
        current += key
        preferences.enabledProviderKeys.set(current)
        return current
    }

    fun disable(key: String): Set<String> {
        val current = preferences.enabledProviderKeys.get().toMutableSet()
        current -= key
        preferences.enabledProviderKeys.set(current)
        return current
    }

    fun getProvider(key: String): DirectoryProvider? = allProviders.find { it.key == key }
}
