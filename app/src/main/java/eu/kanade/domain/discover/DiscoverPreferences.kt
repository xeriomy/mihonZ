package eu.kanade.domain.discover

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Preferences for the Discover (extension directory) feature.
 *
 * Key non-negotiable principle: the set of enabled provider keys is **empty
 * by default** — the fork ships no default enabled provider and never
 * auto-installs anything. Users explicitly enable a provider.
 */
@Inject
@SingleIn(AppScope::class)
class DiscoverPreferences(
    preferenceStore: PreferenceStore,
) {

    /**
     * Keys of directory providers the user has explicitly enabled.
     * Empty on a fresh install.
     */
    val enabledProviderKeys: Preference<Set<String>> = preferenceStore.getStringSet(
        "discover_enabled_providers",
        emptySet(),
    )
}
