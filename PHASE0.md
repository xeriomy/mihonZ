# Phase 0 — Orientation and Verification

## Summary

This fork is **`app.mihon`** (namespace `eu.kanade.tachiyomi`, versionName `0.20.4`, versionCode `29`).
Source tree is a multi-module Gradle project. Persistence is **SQLDelight** (not Room).
DI is **Metro** (`@Inject`, `@SingleIn(AppScope::class)`, `@ContributesBinding`).
DI for ViewModels uses `metrox` (`@ViewModelKey` + `@ContributesIntoMap(AppScope::class)`) resolved via `metroViewModel<T>()`.
Navigation is **Voyager** (`Screen`, `Navigator`, `LocalNavigator.currentOrThrow`, tabs via `TabContent`/`TabbedScreen`).
Strings are **moko-resources** (`tachiyomi.i18n.MR` / `MR.strings.<name>`), source-of-truth `base/strings.xml`.
Jsoup, OkHttp, kotlinx.serialization, SQLDelight, Metro are **all already dependencies** (no new heavy deps needed).

## Module structure (`settings.gradle.kts`)

```
:app                      # application; UI + view models + presenters live here
:baseline-profile
:core-metadata
:core:archive
:core:common            # NetworkHelper, OkHttp client, network requests
:core:metro             # GraphProvider, metroGraph(), appGraph accessor
:data                   # SQLDelight DB, data-layer repositories (tachiyomi.data.*)
:domain                 # interactors / use-cases / model / repository interfaces
  (note: some extension-store interactors live under mihon.domain.extension.*)
:i18n                   # moko-resources string catalogs (commonMain/moko-resources/base/strings.xml)
:icons:material-symbols
:icons:simple-icons
:presentation-core     # shared compose primitives (EmptyScreen, LoadingScreen, Scaffold, theme)
:presentation-widget
:source-api
:source-local
:telemetry
```

## Verified paths

### Browse UI (Compose screens + presenters)

- **Browse tab root (tab list)**: `/root/mihonZ/app/src/main/java/eu/kanade/tachiyomi/ui/browse/BrowseTab.kt`
  - `BrowseTab.Content()` builds the tab list: `listOf(sourcesTab(), extensionsTab(...), migrateSourceTab())`
  - Wired into `TabbedScreen` (titleRes, tabs, pager state, shared search bar). No preference controls tab order; the list is hardcoded.
  - **Integration todo**: add `discoverTab()` to this list (marked `// DISCOVER:`).
- **Tab container**: `/root/mihonZ/app/src/main/java/eu/kanade/presentation/components/TabbedScreen.kt`
  - `TabContent(titleRes, badgeNumber, searchEnabled, actions, content)`
- **Sources tab**: `/root/mihonZ/app/src/main/java/eu/kanade/tachiyomi/ui/browse/source/SourcesTab.kt`
  - Pattern to follow: `Screen.sourcesTab(): TabContent` using `metroViewModel<SourcesViewModel>()`.
- **Sources presenter/screen**:
  - `/root/mihonZ/app/src/main/java/eu/kanade/tachiyomi/ui/browse/source/SourcesViewModel.kt`
  - `/root/mihonZ/app/src/main/java/eu/kanade/presentation/browse/SourcesScreen.kt`
- **Extensions tab**: `/root/mihonZ/app/src/main/java/eu/kanade/tachiyomi/ui/browse/extension/ExtensionsTab.kt`
- **Extensions presenter**: `/root/mihonZ/app/src/main/java/eu/kanade/tachiyomi/ui/browse/extension/ExtensionsViewModel.kt`
- **Extensions screen**: `/root/mihonZ/app/src/main/java/eu/kanade/presentation/browse/ExtensionsScreen.kt`
- **Migrate tab**: `eu.kanade.tachiyomi.ui.browse.migration.sources.migrateSourceTab` (under `app/src/main/java/eu/kanade/tachiyomi/ui/browse/migration/sources/`)

### Extension-repo machinery

- **Repository interface** (domain): `/root/mihonZ/domain/src/main/java/mihon/domain/extension/repository/ExtensionStoreRepository.kt`
  - `insert(indexUrl)`, `insertFromPreference(indexUrl, name)`, `refreshAll()`, `fetchExtensions()`, `getAll()`, `getAllAsFlow()`, `getCountAsFlow()`, `remove(indexUrl)`.
- **Repository impl** (data): `/root/mihonZ/data/src/main/java/mihon/data/extension/repository/ExtensionStoreRepositoryImpl.kt`
  - Implements `ExtensionStoreRepository`; `@ContributesBinding(AppScope::class)`.
  - `insert(indexUrl)` → `service.fetch(indexUrl)` validates `index.min.json` then upserts into DB.
- **Network service**: `/root/mihonZ/data/src/main/java/mihon/data/extension/service/ExtensionStoreService.kt`
  - `fetch(indexUrl)`: fetches index, auto-detects legacy v1 `[` array vs v2 `{` object vs protobuf; resolves legacy `index_v2` redirect; validates signing key. **This is the validation gate we reuse.**
- **Network model**: `/root/mihonZ/data/src/main/java/mihon/data/extension/model/NetworkLegacyExtensionRepo.kt` and `NetworkExtensionStore.kt` (same folder).
- **Domain model**: `/root/mihonZ/domain/src/main/java/mihon/domain/extension/model/ExtensionStore.kt`
- **SQLDelight table**: `/root/mihonZ/data/src/main/sqldelight/tachiyomi/data/extension_store.sq`
  - Table `extension_store(index_url PK, name, badge_label, signing_key, contact_website, contact_discord, is_legacy, extension_list_url)`.
- **Interact** (domain use-cases, all `@Inject`):
  - `/root/mihonZ/domain/src/main/java/mihon/domain/extension/interactor/AddExtensionStore.kt` — `invoke(indexUrl): Result<Unit>` → `repository.insert(indexUrl)`. **Use this for one-tap add.**
  - `/root/mihonZ/domain/src/main/java/mihon/domain/extension/interactor/GetExtensionStores.kt` — `get(): List<ExtensionStore>`, `subscribe(): Flow<List<ExtensionStore>>`.
  - `/root/mihonZ/domain/src/main/java/mihon/domain/extension/interactor/GetExtensionStoreCountAsFlow.kt`
  - `/root/mihonZ/domain/src/main/java/mihon/domain/extension/interactor/RemoveExtensionStore.kt`
  - `/root/mihonZ/domain/src/main/java/mihon/domain/extension/interactor/UpdateExtensionStores.kt`
  - `/root/mihonZ/domain/src/main/java/mihon/domain/extension/interactor/GetExtensionStoreCountAsFlow.kt`

### Trust / signature verification (REUSE, never bypass)

- **Trust interactor**: `/root/mihonZ/app/src/main/java/eu/kanade/domain/extension/interactor/TrustExtension.kt`
  - `TrustExtension` (domain, `eu.kanade.domain.extension...`).
- **Signature check**: `/root/mihonZ/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt` (extension loading + trust verification).
- **Migration path** (historical trust migration): `/root/mihonZ/app/src/main/java/mihon/core/migration/migrations/TrustExtensionRepositoryMigration.kt`
- The repo-add flow via `AddExtensionStore` → `ExtensionStoreService.fetch()` already enforces the signing-key / repo.json validation. Post-add: call `extensionManager.findAvailableExtensions()` to trigger the refresh.

### Deep-link / intent handling

- **Manifest intent filters** (`/root/mihonZ/app/src/main/AndroidManifest.xml`):
  - `tachiyomi://add-repo` (legacy) and `mihon://extension-store` — both handled in `MainActivity`.
  - **Integration todo**: add `${appId}://add-repo` filter for Discover (namespaced to `app.mihon`).
- **Intent dispatch**: `/root/mihonZ/app/src/main/java/eui/kanade/tachiyomi/ui/main/MainActivity.kt`
  - `handleIntentAction(intent, navigator)` (line ~524): `Intent.ACTION_VIEW` → `intent.isAddExtensionStoreIntent()` → reads `url` query param → `navigator.push(ExtensionStoresScreen(repoUrl))`.
  - `isAddExtensionStoreIntent()`: scheme `tachiyomi`+host `add-repo` OR scheme `mihon`+host `extension-store`.
  - `ExtensionStoresScreen(url: String?)` deep-link constructor: `/root/mihonZ/app/src/main/java/eu/kanade/presentation/more/settings/screen/browse/ExtensionStoresScreen.kt` — calls `viewModel.addFromDeeplink(url)`.
  - `addFromDeeplink` (in `ExtensionStoresViewModel.kt`) shows a `Confirm` dialog before adding (explicit user confirmation). **Reuse this exact UX.**

### Settings entry for repos

- `/root/mihonZ/app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsBrowseScreen.kt`
  - `SettingsBrowseScreen : SearchableSettings` builds `getPreferences()`. The "Extension stores" item navigates to `ExtensionStoresScreen()`.
  - Accesses `context.appGraph.getExtensionStoreCountAsFlow`.
  - **Integration todo**: add a "Discover directories" text preference here (marked `// DISCOVER:`).

### DI / AppGraph

- `/root/mihonZ/app/src/main/java/mihon/app/di/AppGraph.kt`
  - `@DependencyGraph(scope = AppScope::class)`, exposes `context`, `json`, `networkHelper`, `extensionManager`, `trustExtension`, `sourcePreferences`, `getExtensionStoreCountAsFlow`, etc.
  - `ViewModelGraph` — interactors are auto-injected into Metro view-models.
  - **Integration todo**: add `discoverPreference` / `discoverRepository` / providers here OR via `@ContributesBinding` in the data layer (preferred, mirrors `ExtensionStoreRepositoryImpl`).
- `/root/mihonZ/app/src/main/java/mihon/app/di/AppBindings.kt`
  - Provides `SqlDriver`, `Database`, `Json` (ignoreUnknownKeys, explicitNulls=false), `XML`, `ProtoBuf`.

### Persistence / DataStore

- SQLDelight DB: `tachiyomi.data.Database` (generated), driver provided in `AppBindings`.
- Preferences use Mihon's own `PreferenceStore` (`tachiyomi.core.common.preference`), NOT Android DataStore — though the task brief says "DataStore for key-value persistence". The repo's actual convention is `PreferenceStore` via injected `PreferenceStore` into `*Preferences` classes (e.g. `SourcePreferences` reads `extensionRepos: Preference<Set<String>>`). **We will follow the repo convention** (`PreferenceStore`) to match upstream, since DataStore is not used anywhere in this fork.
- `SourcePreferences.extensionRepos: Preference<Set<String>>` lives at `/root/mihonZ/app/src/main/java/eu/kanade/domain/source/service/SourcePreferences.kt` (line ~end). This is the existing "added repos" set — useful for duplicate detection.

### Network / serialization helpers (reuse)

- `NetworkHelper.client` (OkHttpClient with cache + Cloudflare interceptor + logging): `/root/mihonZ/core/common/src/main/kotlin/eu/kanade/tachiyomi/network/NetworkHelper.kt`
- `GET(url)` request builders: `/root/mihonZ/core/common/src/main/kotlin/eu/kanade/tachiyomi/network/Requests.kt`
- `awaitSuccess()` extension: used across data layer (e.g. `ExtensionStoreService`).
- `Json` instance from `AppBindings` (ignoreUnknownKeys=true, explicitNulls=false) — reuse for any JSON parsing.

### i18n

- Source catalog: `/root/mihonZ/i18n/src/commonMain/moko-resources/base/strings.xml` (1083 lines; the `base` = English fallback).
- Access: `import tachiyomi.i18n.MR; MR.strings.<name>` (or `tachiyomi.i18n.MR`).
- Plural resources live in `base/plurals.xml`.
- Adding a string = add `<string name="...">` to `base/strings.xml` (and optionally other locales, but English base is required). `MR` is generated from `commonMain/moko-resources`.

## Application ID

- `applicationId = "app.mihon"` (build variant suffixes: `.dev`, `.foss`, `.debug`, `.benchmark`).
- Namespace: `eu.kanade.tachiyomi` (app module), but the fork identity is `app.mihon`.
- **Deep-link scheme must be namespaced to `app.mihon`** per the task: `app.mihon://add-repo`.

## Trust / signature verification — reuse summary

The existing repo-add path already enforces everything we need:
`AddExtensionStore(indexUrl)` → `ExtensionStoreRepository.insert(indexUrl)` → `ExtensionStoreService.fetch(indexUrl)` validates the index (`index.min.json` / `repo.json` / protobuf) and extracts the signing key; rows are upserted into `extension_store`. Extensions are then loaded/trusted by `ExtensionManager` + `ExtensionLoader` (signature verification) + `TrustExtension`. **Phase 3 will call `AddExtensionStore` directly; no trust/signature logic is forked.**

## Notes for implementation

- New code goes in new `discover/` packages under the app source tree (or appropriate module). Touchpoints to existing files marked `// DISCOVER:`.
- Project uses SQLDelight (not Room). Phase spec mentions Room caches; we map that to a SQLDelight table in `extension_store.sq` or a new `discover_cache.sq`. We follow the repo's actual persistence choice (SQLDelight) to keep the build identical and up-rebase-clean.
- Jsoup is already a dependency (`libs.jsoup`), approved for the Miyomi HTML fallback.
