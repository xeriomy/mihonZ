package eu.kanade.tachiyomi.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleCrop
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.discover.domain.ContentType
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryFilters
import eu.kanade.tachiyomi.ui.discover.domain.DirectoryRepo
import eu.kanade.tachiyomi.ui.discover.presenter.DiscoverUiState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.Sort
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

/**
 * The Discover tab content.
 *
 * Rendered by [discoverTab]. Handles three modes:
 * 1. **First-open consent** — no provider enabled: neutral explainer + explicit
 *    "Enable sample directory" button + disabled "add later" note.
 * 2. **Loading / error / empty** states.
 * 3. **Ready** — LazyVerticalGrid of repo cards with client-side search,
 *    filter chips, sort menu, and per-card Add / open-site actions.
 *
 * Nothing here auto-adds or auto-enables anything; every action is a deliberate
 * user tap handled by the Presenter layer.
 */
@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onEnableMock: () -> Unit,
    onSearchQueryChange: (String?) -> Unit,
    onSort: (DiscoverUiState.SortMode) -> Unit,
    onAppFilter: (Set<String>) -> Unit,
    onContentTypeFilter: (Set<ContentType>) -> Unit,
    onLanguageFilter: (Set<String>) -> Unit,
    onNsfwFilter: (DirectoryFilters.NsfwFilter) -> Unit,
    onRetry: () -> Unit,
    onAddRepo: (DirectoryRepo) -> Unit,
    onOpenSite: (String) -> Unit,
) {
    // First-open flow: no provider enabled yet.
    if (!state.hasProviders) {
        DiscoverConsentScreen(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = MaterialTheme.padding.medium),
            onEnableMock = onEnableMock,
        )
        return
    }

    val shownRepos = state.repos
    val isLoading = state.isLoading && shownRepos.isEmpty()

    if (isLoading) {
        LoadingState(Modifier.padding(contentPadding))
        return
    }

    if (state.error != null && shownRepos.isEmpty()) {
        ErrorState(
            modifier = Modifier.padding(contentPadding),
            message = state.error!!,
            onRetry = onRetry,
        )
        return
    }

    if (shownRepos.isEmpty()) {
        EmptyState(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = MaterialTheme.padding.medium),
            onRetry = onRetry,
        )
        return
    }

    // Ready: embedded search + filters + grid
    var searchQuery by rememberSaveable { mutableStateOf(state.searchQuery) }
    LaunchedEffect(state.searchQuery) { searchQuery = state.searchQuery }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery.orEmpty(),
            onValueChange = {
                searchQuery = it.ifEmpty { null }
                onSearchQueryChange(searchQuery)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding + topSmallPaddingValues)
                .padding(horizontal = MaterialTheme.padding.medium),
            placeholder = { Text(stringResource(MR.strings.discover_search_hint)) },
            singleLine = true,
            leadingIcon = {
                Icon(MaterialSymbols.Rounded.Search, contentDescription = null)
            },
        )

        FilterChipsRow(
            state = state,
            onAppFilter = onAppFilter,
            onNsfwFilter = onNsfwFilter,
            onSort = onSort,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = contentPadding + topSmallPaddingValues +
                PaddingValues(horizontal = MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            modifier = Modifier
                .fillMaxSize()
                .padding(topSmallPaddingValues),
        ) {
            items(shownRepos, key = { it.id }) { repo ->
                RepoCard(
                    repo = repo,
                    onClick = { onAddRepo(repo) },
                    onOpenSite = { onOpenSite(it) },
                )
            }
        }
    }
}

@Composable
private fun DiscoverConsentScreen(
    modifier: Modifier = Modifier,
    onEnableMock: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        Text(
            text = stringResource(MR.strings.discover_consent_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(MR.strings.discover_consent_body),
            style = MaterialTheme.typography.bodyMedium,
        )

        // Explicit opt-in: a real tap, not a default-OK dialog.
        TextButton(
            onClick = onEnableMock,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(MR.strings.discover_consent_enable_mock))
        }

        // "Add a custom directory later" — disabled in Phase 1 (Phase 5).
        TextButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(MR.strings.discover_consent_add_later))
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(MR.strings.discover_consent_add_later_summary),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        repeat(6) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {}
        }
    }
}

@Composable
private fun ErrorState(
    modifier: Modifier = Modifier,
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.padding.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Refresh,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(message, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) {
            Text(stringResource(MR.strings.discover_action_retry))
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        Text(
            text = stringResource(MR.strings.discover_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(MR.strings.discover_empty_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(MR.strings.discover_action_retry))
        }
    }
}

@Composable
private fun RepoCard(
    repo: DirectoryRepo,
    onClick: () -> Unit,
    onOpenSite: (String) -> Unit,
) {
    val isAddable = repo.repoUrl != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp)),
        onClick = if (isAddable) {
            onClick
        } else {
            {}
        },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            // Header: avatar + name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                RepoAvatar(repo.name)
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (repo.isNsfw) {
                    BadgeBox(stringResource(MR.strings.ext_nsfw_short), MaterialTheme.colorScheme.error)
                }
                repo.extensionCount?.let {
                    BadgeBox(
                        stringResource(MR.strings.discover_extensions_count, it),
                        MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Text(
                text = repo.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (repo.compatibleApps.isNotEmpty()) {
                Text(
                    text = stringResource(MR.strings.discover_compatible_with, repo.compatibleApps.joinToString()),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Spacer(Modifier.weight(1f))

            // Content types chips
            if (repo.contentTypes.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    repo.contentTypes.take(3).forEach { type ->
                        AssistChip(
                            onClick = {},
                            label = { Text(typeLabel(type)) },
                            modifier = Modifier.height(20.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!repo.siteUrl.isNullOrEmpty()) {
                    IconButton(onClick = { onOpenSite(repo.siteUrl) }) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Public,
                            contentDescription = stringResource(MR.strings.discover_open_site),
                        )
                    }
                }
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Download,
                        contentDescription = stringResource(
                            if (isAddable) MR.strings.discover_add_to_mihon else MR.strings.discover_copy_url,
                        ),
                        tint = if (isAddable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeBox(text: String, color: Color) {
    Box(
        Modifier
            .clip(CircleCrop)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun RepoAvatar(name: String) {
    val initials = name.split(" ").take(2).joinToString("") { it.firstOrNull()?.toString().orEmpty() }
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
    )
    val bg = palette[(name.hashCode().absoluteValue) % palette.size]
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleCrop)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.ifEmpty { "•" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun typeLabel(type: ContentType): String = when (type) {
    ContentType.MANGA -> stringResource(MR.strings.discover_content_type_manga)
    ContentType.ANIME -> stringResource(MR.strings.discover_content_type_anime)
    ContentType.NOVEL -> stringResource(MR.strings.discover_content_type_novel)
    ContentType.LIGHT_NOVEL -> stringResource(MR.strings.discover_content_type_light_novel)
    ContentType.MOVIES -> stringResource(MR.strings.discover_content_type_movies)
    ContentType.TV_SHOWS -> stringResource(MR.strings.discover_content_type_tv_shows)
    ContentType.OTHER -> stringResource(MR.strings.discover_content_type_other)
}

@Composable
private fun FilterChipsRow(
    state: DiscoverUiState,
    onAppFilter: (Set<String>) -> Unit,
    onNsfwFilter: (DirectoryFilters.NsfwFilter) -> Unit,
    onSort: (DiscoverUiState.SortMode) -> Unit,
) {
    val (expanded, setExpanded) = remember { mutableStateOf(false) }

    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.padding.medium,
                    vertical = MaterialTheme.padding.extraSmall,
                ),
        ) {
            if (state.filters.compatibleApps.isNotEmpty()) {
                AssistChip(
                    onClick = { onAppFilter(emptySet()) },
                    label = {
                        Text(
                            text = state.filters.compatibleApps.joinToString(),
                            maxLines = 1,
                        )
                    },
                    trailingIcon = { Icon(MaterialSymbols.Rounded.Close, contentDescription = null) },
                )
            }
            AssistChip(
                onClick = { onNsfwFilter(state.filters.nsfw.next()) },
                label = { Text(stringResource(MR.strings.discover_filter_nsfw)) },
            )
            AssistChip(
                onClick = { setExpanded(true) },
                label = { Text(stringResource(MR.strings.discover_action_sort)) },
                trailingIcon = { Icon(MaterialSymbols.Rounded.Sort, contentDescription = null) },
            )

            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(MR.strings.discover_extensions_count, state.repos.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                TextButton(onClick = {
                    onSort(DiscoverUiState.SortMode.NAME)
                    setExpanded(false)
                }) {
                    Text(stringResource(MR.strings.discover_sort_name))
                }
                TextButton(onClick = {
                    onSort(DiscoverUiState.SortMode.COUNT)
                    setExpanded(false)
                }) {
                    Text(stringResource(MR.strings.discover_sort_count))
                }
            }
        }
    }
}
