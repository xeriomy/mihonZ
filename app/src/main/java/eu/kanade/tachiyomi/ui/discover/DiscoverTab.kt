package eu.kanade.tachiyomi.ui.discover

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.ui.discover.presenter.DiscoverViewModel
import eu.kanade.tachiyomi.util.system.openInBrowser
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Public
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * TabContent entry point for the Discover tab inside Browse.
 *
 * Follows the same pattern as [eu.kanade.tachiyomi.ui.browse.source.sourcesTab].
 * The tab is always visible, but its content defers to the first-open consent
 * flow when no provider is enabled.
 */
@Composable
fun discoverTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow

    return TabContent(
        titleRes = MR.strings.label_discover,
        searchEnabled = true,
        actions = listOf(
            AppBar.Action(
                title = stringResource(MR.strings.discover_settings_title),
                icon = MaterialSymbols.Rounded.Public,
                onClick = {
                    // DISCOVER (Phase 5): open Discover directories settings.
                    // Placeholder no-op in Phase 1; wired in Phase 5.
                },
            ),
        ),
        content = { contentPadding, snackbarHostState ->
            val viewModel = metroViewModel<DiscoverViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val context = LocalContext.current

            DiscoverScreen(
                state = state,
                contentPadding = contentPadding,
                snackbarHostState = snackbarHostState,
                onEnableMock = { viewModel.setProviderEnabled(MOCK_PROVIDER_KEY, true) },
                onSearchQueryChange = viewModel::setSearchQuery,
                onSort = viewModel::setSort,
                onAppFilter = viewModel::setAppFilter,
                onContentTypeFilter = viewModel::setContentTypeFilter,
                onLanguageFilter = viewModel::setLanguageFilter,
                onNsfwFilter = viewModel::setNsfwFilter,
                onRetry = { viewModel.refresh() },
                onAddRepo = { repo ->
                    // DISCOVER (Phase 3): invoke upstream AddExtensionStore here.
                },
                onOpenSite = context::openInBrowser,
            )
        },
    )
}

// Key of the MockProvider, referenced by the consent explainer button.
internal const val MOCK_PROVIDER_KEY = "mock"
