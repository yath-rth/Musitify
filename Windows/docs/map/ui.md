# Musitify Android → Desktop: UI Layer Map

Source root: `Android/app/src/main/java/com/yatharth/musitify/` (abbrev. `src/`).
Covers `MainActivity.kt`, `ui/theme/*`, `ui/screens/*`, `ui/elements/*`, `ui/elements/display/*`, and `res/`.
All dimensions are dp unless noted; all text sizes are sp before adaptive scaling.

> **Read §0 first** — several behaviors are surprising (percent-based corners, pixel-based vinyl rings, index/id mixups, no drag in "reorderable" list, dead code). A 1:1 port should reproduce the *visible* behavior, then decide on each quirk deliberately.

---

## 0. Surprises / quirks you must know before porting

1. **`ShadowBox` corners are PERCENT, not dp.** `RoundedCornerShape(rounding)` where `rounding: Int` resolves to the `RoundedCornerShape(percent: Int)` overload. `rounding = 5` ⇒ 5 % of the shorter side (e.g. a 180 dp-wide card ≈ 9 dp radius; a 26 dp-tall bar ≈ 1.3 dp). Same for `FillBar`: `RoundedCornerShape(20)` = 20 % (12 dp bar ⇒ 2.4 dp radius). Everything else that uses `RoundedCornerShape(x.dp)` is dp.
2. **`ShadowBox` shadow colour = `cardBorder`, border colour = `text`** — both are `#1A1A2E`, so visually identical.
3. **Vinyl rings are in raw pixels**: `radius - i * 20f` px, stroke `1f` px. On desktop (density 1.0) rings will be spaced 20 dp; on a 2.75x phone they were ~7 dp apart. Decide whether to keep px or convert to dp (`20f / 2.75 ≈ 7.3dp` reproduces a typical phone look).
4. **The entire vinyl Box (disc + album art) rotates**, not just the disc. The cover art is a centred 100 dp circle overlaid on the canvas.
5. **`ReorderableSongList` has NO drag logic.** Reordering is done exclusively with up/down arrow `IconButton`s (the header text literally says "USE ARROWS TO REORDER SONGS"). No long-press-drag, no swipe.
6. **`PlaylistCarousel` passes `playlist.id` where an index is expected**: `onPlaylistSelected(playlistInfo.playlist.id)` (VM `loadPlaylist(id)` — fine, it's an id) but the off-centre tap does `listState.animateScrollToItem(playlistInfo.playlist.id)` (bug: scrolls to index == id). Also HomeScreen passes `currentPlayingIndex = selectedPlaylistId ?: 0` (an id) and the carousel scrolls to that *index* on change.
7. **Carousel "dead zone"**: any item whose normalized distance from centre is < `threshold` (0.5) is treated as distance 0 ⇒ full scale/alpha, zero rotation, and counts as "near centre" (tap plays/selects). With 180–200 dp items and ‑20 dp spacing, usually only one item qualifies on a phone, but on a wide desktop window **several items could be inside the dead zone**. See §6.1 for a desktop recommendation.
8. **Carousels use `LocalConfiguration.screenWidthDp`** (full screen width, not the composable's width) to compute side padding so the first/last item can be centred. On desktop use `BoxWithConstraints { maxWidth }`.
9. **`SongListRow.onLongClick` is never wired** — the row only has `.clickable(onClick)`. In list mode there is no way to long-press (the HomeScreen passes a delete-confirm lambda that never fires). `PlaylistListRow` is completely unused (dead code). `SongItemUI`, `BudgetModeSelector`, all of `DesignSystem.kt` (`designSystem*`, `DesignSystemPreview`), `categoryColors`, `transparentBg`, `textFieldColors()`, the `addSong`/`createPlaylist` params of `PlaybackControls` (button commented out) are unused in the live UI.
10. **Add Song has no visible entry point.** The `+` button in `PlaybackControls` is commented out; `showAddDialog` is never set to true anywhere reachable. `AddSongDialog` and `DownloadQueueCard` are still fully wired; a desktop port should probably restore a "+" button (tap = add song, long-press = create playlist, per the commented code).
11. **`SleepTimerDialog` uses `headlineLarge`**, which is NOT defined in `adaptiveTypography()` ⇒ falls back to Material3 default (Roboto/system default font, 32sp, lineHeight 40sp, weight Normal, overridden to Bold). So the countdown is the only non-JetBrains-Mono text in the app.
12. **Dark theme == light theme**: both colour schemes are identical. The app is always the light sage palette.
13. **Typography is re-created on every call**: screens call `adaptiveTypography().xxx` inline instead of `MaterialTheme.typography`. Equivalent result; port can just use `MaterialTheme.typography`.
14. `PlaybackScreen` song lookup: `allSongs[allSongs.indexOfFirst { it.name == currentTitle }.coerceAtLeast(0)]` — matches by **name**, and crashes if `allSongs` is empty while something is playing. Current-playing highlighting everywhere is by `song.name == uiState.currentTitle`.
15. StatsScreen's `PlaybackControls` passes `onStats = onClose`, so the Insights button toggles back to Home; its sleep timer button does nothing (`onSleepTimer = {}`) — it only reflects active state.
16. `DesignSystem.kt` header still says "Tracknix UI design patterns" — this design language was copied from another app (Tracknix, an expense tracker; hence `categoryColors` like "Groceries", "Rent", `BudgetModeSelector`, comments about "total spend").

---

## 1. Resources (`res/`)

| File | Content |
|---|---|
| `values/strings.xml` | `app_name = "Musitify"` (only string; all UI strings are hard-coded in Kotlin) |
| `values/colors.xml` | Android template leftovers, unused by Compose: purple_200 `#FFBB86FC`, purple_500 `#FF6200EE`, purple_700 `#FF3700B3`, teal_200 `#FF03DAC5`, teal_700 `#FF018786`, black `#FF000000`, white `#FFFFFFFF` |
| `values/themes.xml` | `Theme.Musitify` parent `android:Theme.Material.Light.NoActionBar` (window theme only) |
| `font/` | `jetbrains_mono_nl_regular.ttf`, `jetbrains_mono_nl_extrabold.ttf`, `jetbrains_mono_nl_extralight.ttf` (JetBrains Mono **NL** = no ligatures). Copy these to desktop `composeResources/font/`. |
| `drawable/art_placeholder.png` | Used only in `MusicRecord` under `LocalInspectionMode` (previews). Useful as a fallback image on desktop for missing art. |
| `drawable/ic_launcher_*`, `mipmap-*` | Default Android Studio launcher icon (green grid + droid). No custom brand icon exists. |

---

## 2. Theme

### 2.1 Colors (`ui/theme/Color.kt`)

| Name | Hex (ARGB) | Comment / usage |
|---|---|---|
| `accentColor1` | `#FFCB3D38` | coral red — primary, destructive/confirm buttons, Stats hero numbers, chart bars |
| `accentColor2` | `#FF52AB2E` | green — secondary, "add"/success, switch on, cursor, reorder-mode tint |
| `accentColor3` | `#FFE7C277` | golden amber — tertiary, "THIS MONTH", playlist stats |
| `background` | `#FFDBE0D8` | soft sage — Scaffold container colour |
| `secondaryBg` | `#FFDDE3DB` | slightly darker sage — all AlertDialog containers, FillBar track |
| `itemsBg` | `#FFF8FFFF` | near-white card colour — ShadowBox default bg, buttons |
| `text` | `#FF1A1A2E` | near-black navy — all text, ShadowBox border |
| `cardBorder` | `#FF1A1A2E` | same as text — shadows, borders, icon tint |
| `disableButtonColor` | `#FFBDB8AF` | unused |
| `transparentBg` | `#CCF5F0E8` | unused |
| `iconColor` | `#FF4A4A5A` | unused |
| `dividerColor` | `#FFCCC7BE` | → `outlineVariant` only |

`categoryColors` map (unused leftover): Food `#1B1B38`, Groceries `#B56C8A`, Transport `#6E87A6`, Education `#8D7AB8`, Entertainment `#C4905B`, Shopping `#8A7668`, Health `#7A9A8C`, Utilities `#D0C16B`, Finance `#5E9A68`, Travel `#8DAF55`, Rent `#C0AA82`, Transfer `#708A36`, Other `#888888`.

Derived colours used repeatedly:
- `accentColor1.copy(alpha=0.2f)` — confirm/delete button bg
- `accentColor2.copy(alpha=0.2f)` — add/positive button bg
- `accentColor2.copy(alpha=0.1f)` — "CREATE PLAYLIST" card bg
- `cardBorder.copy(alpha=0.1f)` — rename button bg; `0.2f` — sleep-timer buttons bg; `0.25f` — empty chart bar; `0.3f` — disabled arrow; `0.5f` — inactive toggle icons; `0.6f` — reorder index number
- `text.copy(alpha=…)`: 0.7 label/secondary, 0.6 sublines, 0.5 hints/placeholders, 0.4 text-field placeholders, 0.2 switch-off thumb
- `itemsBg.copy(alpha=0.8f)` — inner rounded (10 dp) pill behind button text in CustomAlertBox/SleepTimer

### 2.2 Material color scheme (`Theme.kt`) — Light and Dark are identical

```
primary = accentColor1        onPrimary = itemsBg
primaryContainer = accentColor1 @0.2   onPrimaryContainer = text
secondary = accentColor2      onSecondary = itemsBg
secondaryContainer = accentColor2 @0.2 onSecondaryContainer = text
tertiary = accentColor3       onTertiary = text
background = background       onBackground = text
surface = itemsBg             onSurface = text
surfaceVariant = secondaryBg  onSurfaceVariant = text
outline = cardBorder          outlineVariant = dividerColor
```
`MusitifyTheme(darkTheme = isSystemInDarkTheme())` → `MaterialTheme(colorScheme, typography = adaptiveTypography())`.

Helpers:
- `textFieldColors()` (unused): text=text, container=itemsBg, indicators transparent, cursor=accent2, leadingIcon focused=accent2 / unfocused=text@0.4.
- `checkBoxColors()`: `checkedColor = accentColor2 @0.6`, `uncheckedColor = text`, `checkmarkColor = accentColor2` (used in PlaylistDialog / AddToPlaylistDialog).

Material defaults that leak through (match on desktop by using Material3 with this scheme): `OutlinedTextField` outline uses `outline` (= `#1A1A2E`) focused / `outline` unfocused unless overridden; `LinearProgressIndicator` track = `surfaceVariant`-ish default; `IconButton` 48 dp touch target; `AlertDialog` shape = 28 dp `extraLarge`, tonal elevation; `TextButton` colour overridden by child Text colour.

### 2.3 Typography (`Type.kt`)

Font family `JetBrainsMonoNL`: Regular (`FontWeight.Normal`), ExtraBold, ExtraLight. **No Medium/Bold files** — `FontWeight.Medium`/`Bold` used in code are synthesised/fallback by the platform (Android picks nearest: Medium→Regular, Bold→ExtraBold). On desktop Skia similarly picks nearest; to be safe, map Medium→Normal, Bold→ExtraBold explicitly or add those weights.

**Adaptive scaling** (`adaptiveSp(base)`) using `LocalConfiguration.screenWidthDp`:
```kotlin
when {
  w < 360 -> base * 0.85f
  w < 400 -> base * 0.90f
  w < 500 -> base * 1.00f
  w < 600 -> base * 1.05f
  else    -> base * 1.10f
}.sp
```
Desktop: use window width (`LocalWindowInfo.current.containerSize` / density, or `BoxWithConstraints` at root). Desktop windows are typically ≥600 dp ⇒ everything ×1.1. Consider capping or choosing the phone-equivalent factor (1.0) for a 1:1 look.

Letter spacing is NOT scaled. Line heights are scaled.

| Style | Weight | Size | Line height | Letter spacing |
|---|---|---|---|---|
| bodySmall | Normal | 12 | 18 | 0.3 |
| bodyMedium | Normal | 14 | 22 | 0.2 |
| bodyLarge | Normal | 17 | 24 | 0 |
| titleLarge | ExtraBold | 38 | 44 | -0.5 |
| titleMedium | ExtraBold | 30 | 36 | -0.3 |
| titleSmall | ExtraBold | 22 | 28 | 0 |
| labelSmall | ExtraLight | 10 | 16 | 1.5 |
| labelMedium | ExtraLight | 12 | 18 | 1.2 |
| labelLarge | ExtraBold | 20 | 26 | 2.0 |
| headlineMedium | ExtraBold | 28 | 34 | -0.3 |
| (headlineLarge) | M3 default (32/40, default font) | — | — | — |

All other M3 styles (displayX, headlineSmall, titleX not listed) are M3 defaults.

### 2.4 Shapes / spacing constants
- No `Shapes` object; shapes are inline. Common: `RoundedCornerShape(4.dp)` (buttons, switch, segmented, input shadows thumbnails), `5.dp` (text-field cards), `8.dp` (reorder rows), `10.dp` (inner text pill), `3.dp` (stat thumbnails, chart bar tops), `2.dp` (switch thumb, period-card colour stripe), `CircleShape` (circular buttons, vinyl art), `RoundedCornerShape(50)` = 50 % (progress indicators), percent shapes in ShadowBox/FillBar (see §0.1).
- Hard shadow: an offset filled box behind content, same shape, colour `#1A1A2E`. Offsets: ShadowBox 4/4, CustomButton 3/3, CustomCircularButton 3/3, CustomSwitch 3/3, text-field cards 4/4, FillBar 2/3, BudgetModeSelector 4/4.
- Borders: ShadowBox 2 dp (6 dp for carousel cards), CustomButton 1.5, CustomCircularButton 1.0, Switch track 2 / thumb 1.5, input cards 1.5, FillBar 1.5.

### 2.5 DesignSystem.kt tokens (reference only — nothing here is used by the live UI)
```
SHADOW_OFFSET_DP = 4    BORDER_THICKNESS_DP = 2    CARD_RADIUS_DP = 5    BUTTON_ANIMATION_MS = 50
```
- `designSystemShadowBox(bgColor=#F8FFFF, shadowOffset=4, borderThickness=2, rounding=5)` — `fillMaxSize` Box; shadow `matchParentSize().offset(4,4)` `#1A1A2E` `RoundedCornerShape(rounding.dp)` (dp here!); content box bg + border `#1A1A2E`.
- `designSystemButton(bgColor=#F8FFFF, text, borderRadius=10, padding=(16,12), textColor=#1A1A2E)` — offset animates to a constant 4 dp (tween 50; never changes ⇒ always offset 4,4), shadow box offset 4/4 r=4 dp, Card height 48, shape r=borderRadius, border 1.5; inner bg `bgColor@0.8`; Text Bold with padding.
- `designSystemSwitch(trackHeight=26, thumbSize=18)` — same as CustomSwitch except track `.size(48.dp).height(26)` (⇒ 48×26), track on colour `#52AB2E@0.15` (not lerp).
- `designSystemSegmentedControl(selected, onSelect, options, selectedColor=#CB3D38)` — padding end/bottom 4; shadow 4/4 r4; Row clip r4, bg `#F8FFFF`, border 2; each option weight 1, selected bg `selectedColor@0.12`, index>0 gets a full 2 dp rectangular border (acts as divider), padding vertical 10; Text `option.uppercase()`, `FontFamily.Monospace`, selected ExtraBold/selectedColor else ExtraLight/`#1A1A2E@0.4`, letterSpacing 1.2, 12sp.
- `designSystemProgressBar(value, max, label?, progressHeight=12, accent=#CB3D38, secondary=#E7C277, empty=#DDE3DB, labelColor=#1A1A2E)` — progress = clamp(value/max); fill colour: >0.8 accent, >0.5 secondary, else empty; optional right-aligned Medium monospace label; 1 dp spacer; Box h=12: shadow offset (2,3) CircleShape; track weight 1, border 1.5 CircleShape, clip, bg empty; fill `fillMaxWidth(progress)` (no fillMaxHeight ⇒ effectively 0 height — another bug).
- `designSystemTextField(value, onValueChange, label?, borderRadius=5)` — padding h8 v4; shadow 4/4 r4; Card bg `#F8FFFF` border 1.5; Row padding 12: `"$label: "` monospace, spacer 4, TextField singleLine (text colour green while pressed else `#1A1A2E@0.7`, indicators = container colour), spacer 8, Check icon 18 dp green when pressed (click clears value), Edit icon 18 dp.
- `DesignSystemPreview()` — Column padding 16 spacedBy 24 showing all of the above ("Click Me", "Daily/Monthly/Weekly", "65% Spent", "Budget"/"1000").

---

## 3. App shell & navigation (`MainActivity.kt`)

- `@AndroidEntryPoint ComponentActivity`; Hilt injects `SongRepository`, `PlaylistRepository`, `SettingsRepository`, `IdManager`, `LegacyFixer` (legacyFixer unused).
- `enableEdgeToEdge()` (content draws behind system bars; screens use `navigationBarsPadding()` + Scaffold insets).
- First-launch hook: `lifecycleScope.launch { if (!settingsRepository.hasUserOpenedApp()) { idGenerator.generateDeviceId(); settingsRepository.openedApp() } }`.
- A commented-out debug `playlistRepository.downloadPlaylist(...)` call.
- **Navigation = one boolean**, no NavHost:
```kotlin
setContent { MusitifyTheme {
    var showStats by rememberSaveable { mutableStateOf(false) }
    if (showStats) StatsScreen(Modifier, onClose = { showStats = false })
    else HomeScreen(Modifier, onOpenStats = { showStats = true })
}}
```
  No transition animation between screens (instant swap). `rememberSaveable` survives rotation.
- **Back handling**
  - StatsScreen: `BackHandler(onBack = onClose)` — always returns to Home.
  - HomeScreen: `BackHandler(enabled = songsInPlaylist.isNotEmpty()) { homeScreenViewModel.clearSelectedPlaylist() }` — exits playlist view back to the playlist carousel; otherwise system back closes the app.
  - Dialogs: Material `AlertDialog` `onDismissRequest` (back / outside tap) → each dialog's `onDismiss` (CustomAlertBox only sets `showClearConfirm=false` and does NOT call `onDismiss` on outside-tap).
- Desktop: `application { Window(title="Musitify", onCloseRequest=::exitApplication) { MusitifyTheme { … } } }`; back = Esc key via `Modifier.onPreviewKeyEvent` on the root (Key.Escape → same handlers) and/or a visible back button; Hilt → manual DI / Koin.
- Each screen gets its own `hiltViewModel()` instances; **Home and Stats get separate `PlaybackControllerViewModel` instances** (state flows come from singleton repositories, so they agree).

---

## 4. HomeScreen (`ui/screens/HomeScreen.kt`)

### 4.1 Signature & ViewModels
```kotlin
HomeScreen(modifier, homeScreenViewModel: HomeScreenViewModel, playbackControllerViewModel: PlaybackControllerViewModel,
           addSongsViewModel: AddSongsViewModel, onOpenStats: () -> Unit)
```
Collected flows:
| local | source |
|---|---|
| `allSongs` | `homeScreenViewModel.sortedSongs` |
| `filteredSongs` | `homeScreenViewModel.searchResults` |
| `songFilterQuery` | `homeScreenViewModel.searchQuery` |
| `songSortMode` / `playlistSortMode` | `homeScreenViewModel.songSortMode` / `.playlistSortMode` (`SortMode`) |
| `playlists` | `homeScreenViewModel.sortedPlaylists` (`List<PlaylistInfo>`) |
| `playlistState` | `homeScreenViewModel.state` (`PlaylistDialogUiState`: Idle/Loading/Success/Error(message)) |
| `songsInPlaylist` | `homeScreenViewModel.songsInPlaylist` (non-empty ⇒ "inside a playlist") |
| `selectedPlaylistId` | `homeScreenViewModel.playlistId` (`Int?`) |
| `uiState` | `playbackControllerViewModel.uiState` (`PlayerUiState{isPlaying, currentSongId, currentTitle, repeatMode:Int(0 off,1 one,2 all), shuffleMode, positionMs, durationMs}`) |
| `sleepTimerState` | `playbackControllerViewModel.sleepTimerState` (`{isActive, duration, remainingMs}`) |
| `addState` | `addSongsViewModel.state` (`AddSongsUiState`, has `Queued`, `Error(message)`) |
| `downloadJobs` | `addSongsViewModel.jobs` (`List<DownloadJob{id:Long,url,type:SONG/PLAYLIST,status,completed,total,error,isActive}>`) |

Local `remember` state: `showAddDialog`, `showSleepTimerDialog`, `url`, `isPlaylistMode`, `isListMode` (All-songs list vs carousel), `isReorderMode`, `showPlaylistDialog`, `isAddToExistingMode`, `newPlaylistName`, `selectedSongIds: Set<Song>`, `selectedPlaylistForDialog: PlaylistInfo?`, `songToDelete: Pair<Int,String>?` (id,name), `playlistToDelete: Playlist?`, `showSongDeleteConfirm` / `showPlaylistDeleteConfirm` (`MutableState<Boolean>`), `songForContextMenu`, `showSongContextMenu`, `showAddToPlaylistDialog`, `showRenameSongDialog`, `newSongName`, `selectedPlaylistForAddSong`, `isAddToExistingPlaylistMode`, `newPlaylistNameForSong`, `playlistForContextMenu`, `showPlaylistContextMenu`, `showRenamePlaylistDialog`, `newPlaylistNameForRename`.
`selectedPlaylist = remember(selectedPlaylistId) { playlists.firstOrNull { it.playlist.id == selectedPlaylistId } }` (note: not keyed on `playlists`).

Effect: `LaunchedEffect(addState) { if (addState is Queued) { showAddDialog=false; url=""; addSongsViewModel.resetState() } }`.

### 4.2 Layout
```
Scaffold(fillMaxSize, containerColor = background)
 ├ bottomBar: Column(fillMaxWidth, navigationBarsPadding, horizontalAlignment End)
 │    └ PlaybackControls(fillMaxWidth, …)                      (§5.6)
 └ content: LazyColumn(modifier.navigationBarsPadding().padding(innerPadding).padding(8.dp))
     ├ [if downloadJobs non-empty] item("download_queue"):
     │     DownloadQueueCard(fillMaxWidth, padding(bottom=16))   (§4.4)
     ├ item  — PLAYLIST SECTION
     │   Row(fillMaxWidth, SpaceBetween, CenterVertically)
     │     Text( selectedPlaylist?.name?.uppercase() ?: "PLAYLISTS",
     │           labelLarge, ExtraBold, letterSpacing 2sp, color text, padding(h16, v8))
     │     if selectedPlaylist == null: SortIcon(playlistSortMode) → vm.onPlaylistSortModeToggle()
     │     else IconButton(Icons.Outlined.Reorder; tint accent2 if reorder mode else cardBorder;
     │                     cd "Exit reorder mode"/"Reorder songs") → isReorderMode = !isReorderMode
     │   AnimatedVisibility(songsInPlaylist.isEmpty())
     │     PlaylistCarousel(playlists, currentPlayingIndex = selectedPlaylistId?.coerceAtLeast(0) ?: 0,
     │        onPlaylistSelected = { vm.loadPlaylist(it) },
     │        onLongClick = { playlistForContextMenu = it; showPlaylistContextMenu = true },
     │        onCreatePlaylist = { showPlaylistDialog = true })
     │   selectedPlaylistId?.let { pid ->
     │     AnimatedVisibility(songsInPlaylist.isNotEmpty() && !isReorderMode)
     │       MusicCarousel(songsInPlaylist, currentPlayingIndex = indexOfFirst{name==currentTitle}.coerceAtLeast(0),
     │          onTrackSelected = { i -> pbVm.playSongs(songsInPlaylist, i, pid) },
     │          onLongSelect = { songForContextMenu = it; showSongContextMenu = true })   (scroll=true)
     │     AnimatedVisibility(songsInPlaylist.isNotEmpty() && isReorderMode)
     │       Column
     │         Text("USE ARROWS TO REORDER SONGS", accent2, labelSmall, Bold, letterSpacing 1sp, padding(h16,v8))
     │         ReorderableSongList(songsInPlaylist, currentPlayingIndex(same calc),
     │            onTrackSelected = { pbVm.playSongs(songsInPlaylist, it, pid) },
     │            onReorder = { f,t -> vm.reorderSongsInPlaylist(f,t) },
     │            Modifier.fillMaxWidth().padding(horizontal=8))
     │   }
     │   Spacer(16)
     ├ item  — ALL SONGS SECTION (only visible when not inside a playlist)
     │   AnimatedVisibility(songsInPlaylist.isEmpty())
     │     Row(fillMaxWidth, SpaceBetween, CenterVertically)
     │       Text("ALL SONGS", labelLarge, ExtraBold, ls 2sp, padding(h16,v8))
     │       SortIcon(songSortMode) → vm.onSongSortModeToggle()
     │       IconButton(icon = isListMode ? Outlined.ViewCarousel : Outlined.List, tint cardBorder,
     │                  cd "Switch to carousel view"/"Switch to list view") → isListMode = !isListMode
     │   AnimatedVisibility(empty && !isListMode)
     │     MusicCarousel(allSongs, scroll = false, currentPlayingIndex(by name),
     │        onTrackSelected = { pbVm.playSongs(allSongs, it, null) }, onLongSelect → song context menu)
     │   AnimatedVisibility(empty && isListMode)
     │     ShadowBox(fillMaxWidth, heightIn(max=500))
     │       Column(padding 12)
     │         LibrarySearchBar(query=songFilterQuery, onQueryChange=vm.onSearchQueryChange, placeholder "search your library")
     │         LazyColumn(padding(vertical=12))   // nested scrollable inside LazyColumn, bounded by 500dp
     │           listedSongs = if (query.isBlank()) allSongs else filteredSongs
     │           items(listedSongs) SongListRow(song, isPlaying = name==currentTitle,
     │               onClick = { pbVm.playSongs(listedSongs, listedSongs.indexOf(song), null) },
     │               onLongClick = { songToDelete = id to name; showSongDeleteConfirm = true })  // never fires (§0.9)
     └ item  — NOW PLAYING VINYL
         uiState.currentTitle?.let { PlaybackScreen(fillMaxWidth, song = allSongs[match by name or 0], playing = uiState.isPlaying) }
```
The three row-header layouts: "ALL SONGS" row has SpaceBetween over 3 children ⇒ title left, SortIcon centre, list toggle right.

`AnimatedVisibility` uses defaults: enter = `fadeIn + expandIn`, exit = `shrinkOut + fadeOut` (default spring specs). Desktop Compose has the same API.

### 4.3 Dialogs hosted by HomeScreen (rendered after the LazyColumn, inside Scaffold content)
| Condition | Dialog | Wiring |
|---|---|---|
| `showAddDialog` | `AddSongDialog` | onDismiss: `showAddDialog=false; addVm.resetState(); url=""`; onUrlChange: `url=it`; onModeChange: `isPlaylistMode=it; addVm.resetState()`; onSubmit: if url not blank → `isPlaylistMode ? addVm.addPlaylist(url) : addVm.addSingleSong(url)` |
| `showSongDeleteConfirm && songToDelete!=null` | `CustomAlertBox` title "Delete Song", body `Are you sure you want to delete "<name>"?`, yes "Delete", no "Cancel" | onConfirm: if `selectedPlaylistId != null` → `vm.deleteSongFromPlaylist(index = songId, playlistId)` else `vm.deleteSong(songId)`; then `songToDelete=null`. onDismiss: `songToDelete=null` |
| `showPlaylistDeleteConfirm && playlistToDelete!=null` | `CustomAlertBox` "Delete Playlist", `Are you sure you want to delete "<name>"?`, "Delete"/"Cancel" | onConfirm: `vm.deletePlaylist(playlist.id)`; `playlistToDelete=null` |
| `showPlaylistDialog` | `PlaylistDialog` | onDismiss: `show=false; vm.resetState(); selectedSongIds=∅; selectedPlaylistForDialog=null; newPlaylistName=""`; onModeChange: `isAddToExistingMode=it; vm.resetState()`; songQuery = `songFilterQuery`, onSongFilter = `vm.onSearchQueryChange`; `allSongs = filteredSongs`; onToggleSong toggles membership in `selectedSongIds`; onSubmit: add-mode → `vm.addSongsToPlaylist(selected.playlist, selectedSongIds)` else `vm.createPlaylistWithSongs(newPlaylistName, selectedSongIds)`. Dialog does **not** auto-close on success (shows "SAVED"). |
| `showSongContextMenu && songForContextMenu!=null` | `SongContextMenuDialog` | onDismiss: hide + clear song; onAddToPlaylist: hide menu, `showAddToPlaylistDialog=true`; onRename: hide, `newSongName=song.name`, `showRenameSongDialog=true`; onDelete: hide, `songToDelete=id to name`, `showSongDeleteConfirm=true`, clear song |
| `showAddToPlaylistDialog && song!=null` | `AddToPlaylistDialog` | onDismiss resets: show=false, selectedPlaylistForAddSong=null, newPlaylistNameForSong="", songForContextMenu=null, isAddToExistingPlaylistMode=false, `vm.resetState()`; onModeChange: set + `vm.resetState()`; onSubmit: existing → `vm.addSongsToPlaylist(sel.playlist, setOf(song))`; new (non-blank) → `vm.createPlaylistWithSongs(name, setOf(song))` |
| `showRenameSongDialog && song!=null` | `RenameSongDialog` | onConfirm: if non-blank & changed → `vm.renameSong(song, newSongName)`; always close & reset |
| `showPlaylistContextMenu && playlist!=null` | `PlaylistContextMenuDialog` | onAddSongs: hide, `showPlaylistDialog=true`, `isAddToExistingMode=true`, `selectedPlaylistForDialog = playlists.first{id==}`; onRename: hide, prefill name, show rename; onDelete: hide, `playlistToDelete=playlist`, show confirm, clear |
| `showRenamePlaylistDialog && playlist!=null` | `RenamePlaylistDialog` | onConfirm: if non-blank & changed → `vm.renamePlaylist(playlist, newName)`; close & reset |
| `showSleepTimerDialog` | `SleepTimerDialog` | onSetTimer: `pbVm.startSleepTimer(duration)`; onCancelTimer: `pbVm.cancelSleepTimer()`; onDismiss: hide |

PlaybackControls wiring (Home): onBack→`pbVm.previous()`, onPause→`pbVm.pause()` (toggle), onNext→`pbVm.next()`, onRepeatToggle→`pbVm.toggleRepeatMode()`, onShuffleToggle→`pbVm.toggleShuffleMode()`, addSong→`showAddDialog=true`, createPlaylist→`showPlaylistDialog=true`, onStats→`onOpenStats`, onSleepTimer→`showSleepTimerDialog=true`, sleepTimerActive=`sleepTimerState.isActive`.

### 4.4 Private composables in HomeScreen.kt

**`DownloadQueueCard(jobs, onCancel(Long), onRetry(Long), onClearFinished, modifier)`**
- state `expanded` (false). `ordered = jobs.sortedBy { !it.isActive }` (stable: active first). `COLLAPSED_ROWS = 3`. `visible = expanded ? ordered : ordered.take(3)`; `hiddenCount = ordered.size - visible.size`.
- `ShadowBox(modifier)` → Column(padding 12):
  - Row(SpaceBetween, CenterV): Text(`activeCount>0 ? "DOWNLOADS · $activeCount ACTIVE" : "DOWNLOADS"`, labelLarge ExtraBold ls 2, padding 4/4); if any finished → `TextButton(onClearFinished){ Text("Clear", cardBorder, labelMedium) }` → `addVm.clearFinished()`.
  - `visible.forEach { DownloadJobRow }` → onCancel `addVm.cancel(id)`, onRetry `addVm.retry(id)`.
  - if `hiddenCount>0 || (expanded && size>3)`: centered `TextButton { expanded = !expanded }` text `expanded ? "Show less" : "Show $hiddenCount more"` (cardBorder, labelMedium).

**`DownloadJobRow(job, onCancel, onRetry)`** — Row(CenterV, spacedBy 8, fillMaxWidth, padding h4 v6):
- Column(weight 1):
  - Text(`(PLAYLIST ? "Playlist · " : "Song · ") + displayUrl(url)`, bodyMedium Medium, 1 line ellipsis). `displayUrl` strips `https://`, `http://`, `www.` prefixes.
  - Status text (labelSmall, 1 line ellipsis; colour `text` if FAILED else `text@0.6`):
    - QUEUED → "Waiting in queue"
    - RUNNING → PLAYLIST && total>0 ? "Downloading ${completed} of ${total}" : "Downloading…"
    - COMPLETED → PLAYLIST && total>1 ? "Done · ${total} songs" : "Done"
    - FAILED → error?.let{"Failed · $it"} ?: "Failed"
    - CANCELLED → "Cancelled"
  - if RUNNING: Spacer 6; `LinearProgressIndicator` (determinate `completed/total` clamped if total>0, else indeterminate), colour cardBorder, fillMaxWidth, clip 50 %.
- Trailing: active → `IconButton(Close, "Cancel download", tint cardBorder)`; FAILED/CANCELLED → `IconButton(Refresh, "Retry download")`; else nothing.

**`LibrarySearchBar(query, onQueryChange, placeholder)`** — M3 `OutlinedTextField` (default colours ⇒ outline `#1A1A2E`, default 4 dp shape), placeholder Text(bodyMedium, text@0.5), leadingIcon `Outlined.Search` tint cardBorder, singleLine, fillMaxWidth, padding(h8, v4).

**`SongListRow(song, isPlaying, onClick, onLongClick)`** — Row(CenterV, spacedBy 10, fillMaxWidth, `.clickable(onClick)` then padding h12 v8):
- `AsyncImage(File(song.artPath))` 34×34, Crop (no clip, square).
- Column(weight 1): Text(name, colour `isPlaying ? cardBorder : text` — identical colours, so no visible highlight!, bodyMedium Medium, 1 line ellipsis); Text(`"${formatTime(length)} · ${playCountOf(song)} plays"`, text@0.6, labelSmall).

**`PlaylistListRow`** — same as SongListRow with playlist art and `"${numOfSongs} songs"`; **unused**.

---

## 5. Elements (`ui/elements/`)

### 5.1 `ShadowBox` — the core card
```kotlin
fun ShadowBox(modifier = Modifier, bgColor = itemsBg, shadowOffsetX = 4.dp, shadowOffsetY = 4.dp,
              borderThickness = 2.dp, rounding: Int = 5, offset = 0.dp,
              alignment = Alignment.Center, content)
Box(modifier, contentAlignment = Center) {
    if (shadowOffsetX != 0.dp || shadowOffsetY != 0.dp)
        Box(Modifier.matchParentSize().offset(shadowOffsetX, shadowOffsetY)
            .background(cardBorder, RoundedCornerShape(rounding)))      // PERCENT
    Box(Modifier.fillMaxWidth().offset(offset, offset)
        .background(bgColor, RoundedCornerShape(rounding))
        .border(borderThickness, text, RoundedCornerShape(rounding)),
        contentAlignment = alignment) { content() }
}
```
- Shadow is drawn via `.offset` so it overflows outside the box bounds (not clipped, no layout space taken). Parents must leave ≥4 dp room (e.g. `PlaybackControls` uses padding 8).
- Content box is `fillMaxWidth`; height wraps content (or is forced by modifier e.g. carousels' `fillMaxSize`).

### 5.2 `CustomButton` — press-offset rectangular button
```kotlin
fun CustomButton(modifier, onClick, onLongClick = {}, bgColor = itemsBg, content)
var pressed by remember { mutableStateOf(false) }
val offset by animateDpAsState(if (pressed) 2.dp else 0.dp, tween(20))
Box(modifier.wrapContentSize().animateContentSize().padding(4.dp)
    .pointerInput(Unit) { detectTapGestures(
        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
        onTap = { onClick() }, onLongPress = { onLongClick() }) }) {
    Box(Modifier.matchParentSize().offset(3.dp, 3.dp).background(cardBorder, RoundedCornerShape(4.dp))) // shadow
    Card(Modifier.wrapContentWidth().wrapContentHeight().offset(offset, offset),
         colors = cardColors(containerColor = itemsBg), shape = RoundedCornerShape(4.dp),
         border = BorderStroke(1.5.dp, cardBorder)) {
        Box(Modifier.wrapContentSize().background(bgColor)) { content() }
    }
}
```
- Visual: card sits at (0,0), shadow at (+3,+3). On press the card slides +2/+2 towards the shadow (20 ms tween). `padding(4)` comes before `pointerInput` in the chain, so the 4 dp margin is outside the hit area; the shadow (`matchParentSize`) is the same size as the card and overflows 3 dp into that margin.
- `bgColor` is painted over `itemsBg` card container ⇒ translucent bgColors (e.g. accent@0.2) appear tinted on white.
- No ripple. `Card` default elevation (M3 filled card = 0 dp) so no real shadow.
- **Caveat**: `pointerInput(Unit)` captures the first `onClick` lambda forever (stale-closure bug in Android if callbacks change). Desktop: key `pointerInput(onClick, onLongClick)` or use `rememberUpdatedState`.
- Desktop: `detectTapGestures` works with mouse; add `Modifier.pointerHoverIcon(PointerIcon.Hand)`; long-press could also be mapped to right-click (`onPointerEvent(PointerEventType.Press)` with `buttons.isSecondaryPressed`).

### 5.3 `CustomCircularButton`
Same as CustomButton except: pressed offset **3 dp**; modifier order `.wrapContentSize().animateContentSize().pointerInput{…}.padding(4.dp)` (hit area includes padding); shadow `CircleShape` offset 3/3; Card `CircleShape`, border **1 dp**; inner Box(bg bgColor, Center) → `Box(modifier.padding(4.dp), Center) { content() }` — **note the outer `modifier` is reused on the inner box** (Compose bug; with default `Modifier` harmless). With a 24 dp Icon the button is 24 + 2·4 = 32 dp circle (+4 dp outer padding each side ⇒ 40 dp footprint).
Preview: PlayArrow icon.

### 5.4 `CustomSwitch` (+ `BudgetModeSelector`)
```kotlin
val thumbOffset by animateDpAsState(if (checked) 20.dp else 0.dp, tween(120))
val trackColor = if (checked) lerp(itemsBg, accentColor2, 0.15f) else itemsBg   // ≈ #D7EFD3
val thumbColor = if (checked) accentColor2 else text.copy(alpha = 0.2f)
Box(Modifier.padding(end = 3.dp, bottom = 3.dp)) {
  Box(Modifier.matchParentSize().offset(3.dp, 3.dp).background(cardBorder, RoundedCornerShape(4.dp)))
  Box(Modifier.width(48.dp).height(26.dp).clip(RoundedCornerShape(4.dp)).background(trackColor)
      .border(2.dp, cardBorder, RoundedCornerShape(4.dp))
      .clickable(indication = null) { onCheckedChange(!checked) }.padding(4.dp),
      contentAlignment = CenterStart) {
    Box(Modifier.offset(x = thumbOffset).size(18.dp).clip(RoundedCornerShape(2.dp))
        .background(thumbColor).border(1.5.dp, cardBorder, RoundedCornerShape(2.dp)))
  }
}
```
Geometry: track inner width = 48 − 8 = 40; thumb 18; travel 20 ⇒ right edge at 38 (2 dp gap). `lerp` here is `androidx.compose.ui.graphics.lerp(Color, Color, Float)`.
(The outer `padding(end = 3, bottom = 3)` only reserves layout space; the shadow is `matchParentSize` = track size 48×26, offset +3/+3, so a 3 dp shadow strip shows right and bottom and fits inside the reserved space. Copy the structure as-is.)

`BudgetModeSelector(selected: BudgetMode{Monthly,Daily}, onSelect)` — **unused**: same as `designSystemSegmentedControl` but text style labelMedium, colours accent1/text@0.4, no-ripple clickable.

### 5.5 `FillBar` — labelled progress bar (used for playback progress and stat shares)
```kotlin
fun FillBar(modifier, label: String? = null, secondaryLabel: String? = "", color = accentColor1,
            value: Double, max: Double, bgColor = secondaryBg)
val progress = if (max > 0) (abs(value) / max).coerceIn(0.0, 1.0) else 1.0   // max<=0 ⇒ FULL bar
val animatedProgress by animateFloatAsState(progress.toFloat())             // default spring
Column(modifier) {
  Row(CenterVertically, SpaceBetween) {
    if (label != null) Text(label, text, bodySmall, Normal, 1 line ellipsis, Modifier.weight(1f))
    Spacer(width 12)
    if (secondaryLabel != null) Text(secondaryLabel, text@0.7, labelSmall, ExtraLight, ls 0.5sp)
  }
  Spacer(Modifier.padding(1.dp))                 // 2dp tall gap
  Box(Modifier.height(12.dp)) {
    Box(Modifier.matchParentSize().offset(2.dp, 3.dp).background(cardBorder, RoundedCornerShape(20)))  // 20%
    Row(CenterVertically) {
      Box(Modifier.weight(1f).border(1.5.dp, cardBorder, RoundedCornerShape(20)).clip(RoundedCornerShape(20)).background(secondaryBg)) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20)).background(bgColor)) {
          Box(Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).background(color))
        }
      }
    }
  }
}
```
Notes: the Row has no `fillMaxWidth`, but the weighted child fills available width. The border is drawn *before* the clip/background in the chain so the fill paints over the inner half of the border (border looks ~0.75 dp where fill covers it). Since there is no label row when label==null && secondaryLabel==null, only the 12 dp spacer of the row remains (Spacer 12 width, zero height). When no playback, PlaybackControls passes `max = 0` ⇒ bar shows 100 % full red.

### 5.6 `PlaybackControls` — bottom bar
Params: `modifier, uiState: PlayerUiState, onBack, onPause, onNext, addSong, createPlaylist, onRepeatToggle, onShuffleToggle, onStats = {}, onSleepTimer = {}, sleepTimerActive = false`.
```
ShadowBox(modifier.padding(8.dp))                        // default 4/4 shadow, 2dp border, itemsBg
  Column(padding 8, spacedBy 2, CenterHorizontally)
    FillBar(padding(h8), label = uiState.currentTitle, value = positionMs, max = durationMs,
            secondaryLabel = if (durationMs != 0) "${formatTime(pos)} / ${formatTime(dur)}" else null)
    Row(spacedBy 2, CenterVertically)
      CustomCircularButton(onBack)          Icon Outlined.ArrowBackIosNew, tint cardBorder
      CustomCircularButton(onPause)         Icon isPlaying ? Outlined.Pause : Outlined.PlayArrow
      CustomCircularButton(onNext)          Icon AutoMirrored.Outlined.ArrowForwardIos
      CustomCircularButton(onRepeatToggle)  Icon repeatMode==1 ? Outlined.RepeatOne : Outlined.Repeat;
                                            tint repeatMode==0 ? cardBorder@0.5 : cardBorder
      CustomCircularButton(onShuffleToggle) Icon Outlined.Shuffle; tint shuffle ? cardBorder : @0.5
      CustomCircularButton(onStats)         Icon Outlined.Insights
      CustomCircularButton(onSleepTimer)    Icon Outlined.Bedtime; tint active ? cardBorder : @0.5
      // commented out: CustomCircularButton(onClick = addSong, onLongClick = createPlaylist) { Icon(Outlined.Add) }
```
All icons 24 dp default, no content descriptions. `formatTime(ms)` = `"%d:%02d"` (minutes:seconds, minutes unbounded). When nothing plays, label null & secondaryLabel null ⇒ bar only, full (max=0).
Desktop: add keyboard shortcuts (Space = play/pause, ←/→ prev/next) — optional enhancement.

### 5.7 `MusicRecord` — vinyl
```kotlin
fun MusicRecord(modifier = Modifier, song: Song, imageSize: Int = 80)
Canvas(modifier.aspectRatio(1f)) {                 // fills available width (square)
    val radius = size.minDimension / 2
    drawCircle(Color.Black, radius)
    repeat(15) { i -> drawCircle(Color.DarkGray.copy(alpha = 0.35f), radius - i * 20f, style = Stroke(1f)) }  // px!
    drawCircle(Color.White, radius * 0.22f)        // label
}
// sibling of the canvas, emitted into the caller's Box (centred there):
AsyncImage(model = song.artPath, Modifier.size(imageSize.dp).clip(CircleShape), ContentScale.Crop)
// (in preview: painterResource(R.drawable.art_placeholder))
```
`Color.DarkGray` = `#FF444444`. Rings: radius, radius−20, …, radius−280 px. If radius < 280 px some radii go negative (Skia ignores/draws nothing). The art (100 dp from PlaybackScreen) covers the white label (label diameter = 0.44 × disc width, e.g. ~150 dp on a 360 dp phone, so a white ring shows around the art).

### 5.8 `SortIcon` & `SortMode`
```kotlin
enum class SortMode { DOWNLOADED_DESC, DOWNLOADED_ASC, PLAYS_DESC, PLAYS_ASC; fun next() = entries[(ordinal + 1) % entries.size] }
```
| mode | field icon (18 dp) | arrow (14 dp) |
|---|---|---|
| DOWNLOADED_DESC | `Icons.Default.Schedule` | `ArrowDownward` |
| DOWNLOADED_ASC | Schedule | `ArrowUpward` |
| PLAYS_DESC | `Icons.Default.Headphones` | ArrowDownward |
| PLAYS_ASC | Headphones | ArrowUpward |
Rendered as `CustomCircularButton(onClick) { Row(CenterV) { fieldIcon 18dp; arrowIcon 14dp } }` (default tint = LocalContentColor). Content descriptions "Sort field" / "Sort direction". Cycling is done in the VM (`onSongSortModeToggle` / `onPlaylistSortModeToggle`).

### 5.9 `SongItemUI` (unused)
`CustomButton(onPlay, onLongPress)` → Row(padding 16, CenterV): art 56 dp if file exists else `Icons.Default.MusicNote` 56 dp; Spacer 12; Text(name, bodyMedium, weight 2, 1 line).

### 5.10 `ReorderableSongList`
```
LazyColumn(fillMaxWidth, height(500.dp) FIXED, spacedBy 8)   // nested inside Home LazyColumn
  itemsIndexed(songs, key = song.id) → ReorderableSongRow(isPlaying = index == currentPlayingIndex,
      onClick = onTrackSelected(index),
      onMoveUp = if (index > 0) onReorder(index, index-1),
      onMoveDown = if (index < size-1) onReorder(index, index+1))
ReorderableSongRow:
  Card(shape 8dp, container = isPlaying ? itemsBg@0.8 : itemsBg)          // no border, M3 default elevation 0
    Row(CenterV, spacedBy 8, fillMaxWidth, padding h12 v8)
      Column(width 40, CenterH, Center)
        IconButton(size 32, enabled index>0)          Icon Default.KeyboardArrowUp 20dp, tint cardBorder (or @0.3 disabled), cd "Move up"
        IconButton(size 32, enabled index<total-1)    Icon KeyboardArrowDown 20dp, cd "Move down"
      AsyncImage(File(artPath), 48dp, Crop, clickable onClick)
      Column(weight 1, clickable onClick)
        Text(name, isPlaying ? accentColor2 : text, bodyMedium, isPlaying ? Bold : Medium, 1 line ellipsis)
        Spacer 4
        Text("${formatTime(length)} · ${playCount} plays", text@0.6, labelSmall, ls 0.5)
      Text("${index+1}", cardBorder@0.6, labelMedium, Bold, padding(end 8))
```
No drag/drop, no animation of item moves (LazyColumn keyed — you could add `Modifier.animateItem()` on desktop). VM: `HomeScreenViewModel.reorderSongsInPlaylist(from, to)`.

### 5.11 Dialog shell (shared by all dialogs)
All dialogs are Material3 `AlertDialog(onDismissRequest, title, text, confirmButton, dismissButton, containerColor = secondaryBg)` with default shape (28 dp rounded), default padding (24 dp), default tonal elevation, scrim. Title = Text(UPPERCASE, labelLarge, ExtraBold, letterSpacing 2sp, colour text). Buttons are `CustomButton`s:
- **Confirm style**: `CustomButton(bgColor = accentColor1@0.2)` with Text(labelMedium, accentColor1, ExtraBold, ls 1.2, padding 10) (green variant uses accentColor2).
- **Cancel style**: `CustomButton()` with Text("CANCEL", labelMedium, text, ExtraLight, ls 1.2, padding 10).
- **Input card pattern** ("Tracknix input field"): `Box(fillMaxWidth, padding(end 4, bottom 4)) { Box(matchParentSize, offset 4/4, bg cardBorder r5dp); Card(fillMaxWidth, itemsBg, r5dp, border 1.5 cardBorder) { OutlinedTextField(fillMaxWidth, singleLine, placeholder Text(bodyMedium, text@0.4), colors: text=text, container=itemsBg, cursor=accent2 [, focused/unfocusedBorder=Transparent in AddToPlaylist & Rename dialogs]) } }`. In AddSongDialog and PlaylistDialog the outline border colours are NOT overridden ⇒ an M3 outline (`#1A1A2E`, 1 dp unfocused / 2 dp focused, 4 dp radius) shows inside the card.
Desktop: M3 `AlertDialog` exists in Compose Multiplatform (`androidx.compose.material3.AlertDialog`) and renders as an in-window popup with scrim; Esc dismisses via `onDismissRequest` (DialogProperties). Works 1:1.

### 5.12 `CustomAlertBox` (confirmation)
Params: `showClearConfirm: MutableState<Boolean>, onConfirm, onDismiss = {}, title, body, yes, no, dontShowAgain: Boolean? = null, onDontShowAgainChange = {}`.
- onDismissRequest: `showClearConfirm.value = false` only.
- title: `title.uppercase()` (labelLarge ExtraBold ls 2).
- text: Column { Text(body, bodySmall, text); if dontShowAgain != null: Spacer 16, Row(CenterV){ Checkbox(default colours); Text("DON'T SHOW AGAIN", labelSmall, ls 1sp) } } (never used with checkbox).
- confirm: `CustomButton(bg accent1@0.2) { Text(yes.uppercase(), labelMedium, accent1, ExtraBold, ls 1.2, Modifier.background(itemsBg@0.8, RoundedCornerShape(10.dp)).padding(10)) }` → `showClearConfirm=false; onConfirm()`.
- dismiss: `CustomButton { Text(no.uppercase(), labelMedium, text, ExtraLight, same pill bg) }` → `showClearConfirm=false; onDismiss()`.
- Strings used: "DELETE SONG"/"DELETE PLAYLIST", `Are you sure you want to delete "<name>"?`, "DELETE", "CANCEL".

### 5.13 `AddSongDialog`
Params: `onDismiss, url, onUrlChange, isPlaylistMode, onModeChange(Boolean), state: AddSongsUiState, onSubmit`.
- Title "ADD SONGS".
- Column(spacedBy 12):
  - Row(SpaceBetween): Text(`isPlaylistMode ? "PLAYLIST URL" : "SINGLE VIDEO URL"`, labelSmall, text@0.7, ExtraLight, ls 1.2) + `CustomSwitch(isPlaylistMode, onModeChange)`.
  - Input card: OutlinedTextField(url) placeholder `isPlaylistMode ? "Paste playlist URL" : "Paste video URL"`.
  - if `state is Error`: Text("FAILED: ${message}", labelSmall, accent1, ExtraBold, ls 1.2).
- Confirm: "ADD TO QUEUE" (accent1 style) → onSubmit. Dismiss: "CANCEL".
- Desktop: consider Ctrl+V auto-paste / Enter-to-submit (`KeyboardActions`/onKeyEvent).

### 5.14 `PlaylistDialog` (create new / add songs to existing)
Params: `onDismiss, isAddToExistingMode, onModeChange, newPlaylistName, onNewPlaylistNameChange, songQuery, onSongFilter, playlists, selectedPlaylist, onSelectPlaylist, allSongs, selectedSongIds: Set<Song>, onToggleSong, state: PlaylistDialogUiState, onSubmit`.
- Title "PLAYLISTS".
- Column(spacedBy 12):
  - Row(SpaceBetween): Text(`isAddToExistingMode ? "ADD TO EXISTING" : "CREATE NEW"`, labelSmall text@0.7 ExtraLight ls 1.2) + CustomSwitch.
  - Add-existing mode: if no playlists → Text("NO PLAYLISTS YET - SWITCH TO CREATE NEW", labelSmall, text@0.5, ls 1); else Text("SELECT PLAYLIST", labelSmall text@0.6 ExtraLight ls 1.2), Spacer 4, `LazyColumn(heightIn(max=120))` of Row(spacedBy 8, CenterV){ Checkbox(checked = selected id match, onCheckedChange = onSelectPlaylist(p), checkBoxColors()); Text(name, bodySmall) } — single-select via checkboxes.
  - Create mode: input card with OutlinedTextField(newPlaylistName), placeholder "Playlist name".
  - Text("SELECT SONGS (${selectedSongIds.size} SELECTED)", labelSmall text@0.6 ExtraLight ls 1.2).
  - Input card: OutlinedTextField(songQuery → onSongFilter) placeholder "FIND SONG...". (Shares the Home search query — typing here also filters the Home list.)
  - `LazyColumn(heightIn(max=200))` items(allSongs, key=id): Row(spacedBy 8, CenterV){ Checkbox(selectedSongIds.contains(song), onToggleSong, checkBoxColors()); AsyncImage(song.artPath, 32dp, default Fit); Text(name, bodySmall, maxLines 2, ellipsis) }.
  - State: Loading → Row(spacedBy 8){ CircularProgressIndicator(height 20, stroke 2, accent2); Text("SAVING...", labelSmall text@0.7 ls 1.2) }; Success → Text("SAVED", labelSmall accent2 ExtraBold ls 1.5); Error → "FAILED: msg" (accent1 ExtraBold ls 1.2); Idle → nothing.
- Confirm: `state is Loading ? "SAVING..." : "SAVE"` (accent1 style). Dismiss "CANCEL".
- Note `allSongs` = `filteredSongs` from Home (search results; check VM: when query blank searchResults presumably = all songs).

### 5.15 `AddToPlaylistDialog` (single song)
Params: `song, onDismiss, isAddToExistingMode, onModeChange, newPlaylistName, onNewPlaylistNameChange, playlists, selectedPlaylist, onSelectPlaylist, state, onSubmit`.
- Title "ADD TO PLAYLIST". Column(spacedBy 12): Text("Song: ${song.name}", bodySmall, text@0.7, Medium); mode Row ("ADD TO EXISTING"/"CREATE NEW" + CustomSwitch); existing-mode list identical to PlaylistDialog but `heightIn(max=200)`; create mode input card (outline transparent) placeholder "Playlist name"; state: Loading "ADDING..." (spinner), Success "ADDED", Error "FAILED: msg".
- Confirm: **green** — `CustomButton(bg accent2@0.2)`, Text(`Loading ? "ADDING..." : "ADD"`, accent2). Dismiss "CANCEL".

### 5.16 `SongContextMenuDialog` (long-press song)
- Title: Row(fillMaxWidth, spacedBy 12, CenterV){ AsyncImage(song.artPath, 48dp, clip r4dp, Crop); Column(weight 1){ Text(name, bodyMedium, Bold, maxLines 2); Text(song.artist, labelSmall, text@0.7, 1 line) } }.
- Text: Column(spacedBy 8) of three `CustomButton`s, each content `Row(fillMaxWidth, padding 12, spacedBy 12, CenterV){ Icon; Text(labelMedium, ExtraBold, ls 1.2) }`:
  1. bg accent2@0.2, Icon `Outlined.Add` accent2, "ADD TO PLAYLIST" accent2 → onAddToPlaylist
  2. bg cardBorder@0.1, Icon `Outlined.Edit` text, "RENAME" text → onRename
  3. bg accent1@0.2, Icon `Outlined.Delete` accent1, "DELETE" accent1 → onDelete
- confirmButton = {} (empty); dismiss = "CANCEL".
- Note: `Row.fillMaxWidth` inside a `wrapContentSize` CustomButton ⇒ buttons stretch to dialog width.

### 5.17 `PlaylistContextMenuDialog` (long-press playlist)
Same structure. Title: art (if `playlist.artPath != null`) 48 dp r4 + Column{ name (bodyMedium Bold, 2 lines), "Playlist" (labelSmall text@0.7) }. Buttons: "ADD SONGS" (green, Add icon) → onAddSongs, "RENAME" → onRename, "DELETE" (red) → onDelete. Dismiss "CANCEL".

### 5.18 `RenameSongDialog` / `RenamePlaylistDialog`
- Title "RENAME SONG" / "RENAME PLAYLIST".
- Column(spacedBy 12): Text("Current name: ${name}", bodySmall, text@0.7, Medium); input card (transparent outline) with OutlinedTextField(newName), placeholder "Enter new name".
- Confirm "RENAME" (accent1 style) → onConfirm; dismiss "CANCEL".

### 5.19 `SleepTimerDialog` + `TimerDurationButton`
- Title "SLEEP TIMER". Column(fillMaxWidth, CenterHorizontally, spacedBy 12):
  - **Active**: Text("Timer Active", bodyMedium, accent1, Bold); Text(`String.format("%02d:%02d", remainingMs/60000, (remainingMs%60000)/1000)`, **headlineLarge** (M3 default), text, Bold) — live updates since `sleepTimerState` is a flow; Spacer 8; `CustomButton(bg cardBorder@0.2) { Text("CANCEL TIMER", labelMedium, text, ExtraBold, ls 1.2, pill bg itemsBg@0.8 r10, padding 10) }` → `onCancelTimer(); onDismiss()`.
  - **Inactive**: Text("Set sleep timer duration", bodySmall, text, TextAlign.Center); Spacer 8; Row(fillMaxWidth, SpaceEvenly){ 5m, 10m, 15m }; Row(SpaceEvenly){ 30m, 45m, 60m }. Each tap → `onSetTimer(duration); onDismiss()`.
- Confirm: "CLOSE" (accent1 style with pill bg) → onDismiss. No dismiss button.
- `TimerDurationButton(duration, onClick)`: `CustomButton(bg cardBorder@0.2) { Text("${duration.minutes}m", labelMedium, text, Bold, pill bg itemsBg@0.8 r10, padding h12 v8) }`.
- `SleepTimerDuration` enum: FIVE(5), TEN(10), FIFTEEN(15), THIRTY(30), FORTY_FIVE(45), SIXTY(60) minutes.

---

## 6. Display (`ui/elements/display/`)

### 6.1 `MusicCarousel` — 3D cover-flow of songs
```kotlin
fun MusicCarousel(modifier, songs: List<Song>, size: Int = 180, spacing: Int = -20, threshold: Float = 0.5f,
                  scroll: Boolean = true, currentPlayingIndex: Int, onTrackSelected: (Int) -> Unit, onLongSelect: (Song) -> Unit)
val listState = rememberLazyListState()
val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)   // snaps item to centre (SnapPosition.Center default)
val sidePadding = (LocalConfiguration.current.screenWidthDp.dp - size.dp) / 2
if (scroll) LaunchedEffect(currentPlayingIndex) { if (songs.isNotEmpty()) listState.animateScrollToItem(currentPlayingIndex) }
// centeredIndex (derivedStateOf findCenterItemIndex) is computed but UNUSED
LazyRow(state, flingBehavior, contentPadding = PaddingValues(horizontal = sidePadding),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp) /* -20dp overlap */, modifier.fillMaxWidth()) {
  itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
    val info = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
    val rawDistance = if (info == null) 1f else {
        val center = layoutInfo.viewportEndOffset / 2f
        val itemCenter = info.offset + info.size / 2f
        ((itemCenter - center) / (layoutInfo.viewportEndOffset / 2f)).coerceIn(-1f, 1f)  // -1 (left edge) .. +1 (right edge)
    }
    val d = if (abs(rawDistance) < threshold) 0f else rawDistance          // dead zone → hard jump at |d|=0.5
    val scale    = lerp(1f, 0.75f, abs(d))      // 1.0 .. 0.75
    val rotY     = lerp(0f, 45f, d)             // signed: right items rotate +Y up to 45°, left items -45°
    val alpha    = lerp(1f, 0.5f, abs(d))       // 1.0 .. 0.5
    val isNearCenter = abs(d) < 0.15f
    Box(Modifier.width(size.dp).height(size.dp + 80.dp)
        .graphicsLayer { scaleX = scale; scaleY = scale; rotationY = rotY; this.alpha = alpha; cameraDistance = 12f * density }
        .combinedClickable(indication = null,
            onLongClick = { onLongSelect(song) },
            onClick = { if (isNearCenter) onTrackSelected(index) else scope.launch { listState.animateScrollToItem(index) } })) {
        AlbumArt(song, Modifier.fillMaxSize(), isPlaying = index == currentPlayingIndex)
    }
  }
}
fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction
```
Notes:
- Because of the dead zone, transforms are a **step** at |raw| = 0.5 (from scale 1 straight to 0.875, rot ±22.5°, alpha 0.75), then linear to the edge. Not a smooth fade. Values are recomputed each frame from `layoutInfo` (reading layoutInfo in composition ⇒ recomposes on scroll).
- `animateScrollToItem(index)` scrolls so the item's *start* is at the content-padding start — which, with `sidePadding = (screen − item)/2`, centres it.
- `scroll=false` for the All Songs carousel (doesn't follow current track); `true` in playlist view.
- Item box 180 × 260 dp; `AlbumArt` fills it.
- Gestures: horizontal drag/fling with snap; tap centred = play; tap side = scroll to it; long-press = context menu. No ripple.
- **Desktop**: LazyRow drag works with touchpad scroll but **mouse wheel scrolls vertically** — add `Modifier.onPointerEvent(PointerEventType.Scroll)` translating vertical wheel delta into `listState.scrollBy` (then snap), or `draggable` for mouse-drag (desktop LazyRow doesn't drag with mouse by default). Replace `screenWidthDp` with `BoxWithConstraints.maxWidth`. Consider normalising distance by `itemWidth*k` rather than half the viewport so wide windows still show the 3D falloff; `combinedClickable` works with mouse; right-click → context menu is a natural extra. Arrow keys ←/→ could step items.

`findCenterItemIndex(listState)`: visible item minimising `|offset + size/2 − viewportEndOffset/2|` (unused result).

**`AlbumArt(song, modifier, isPlaying = false)`** (`isPlaying` is unused visually):
```
ShadowBox(modifier, shadowOffsetX = 0, shadowOffsetY = 0 /* no shadow */, borderThickness = 6.dp, rounding = 5 /*%*/)
  Column(padding 12)
    AsyncImage(File(song.artPath), fillMaxWidth, aspectRatio 1, Crop)   // 156×156 for 180 card
    Spacer 8
    Text(song.name, maxLines 2, ellipsis, bodyMedium, text, Normal)
    Text(formatTime(length) + " • " + playCountOf(song) + " plays", 1 line, labelSmall, text@0.6, ExtraLight, ls 0.5)
```
`playCountOf(song) = round(song.timeHeard / song.length)` (0 if length 0).

### 6.2 `PlaylistCarousel`
Same mechanics as MusicCarousel with: item width **200 dp**, height **264 dp** (200+64), spacing −20, fixed threshold 0.5, always `animateScrollToItem(currentPlayingIndex)` on change, key `playlist.id`.
- Tap centred → `onPlaylistSelected(playlist.id)` (→ `homeScreenViewModel.loadPlaylist(id)`); tap side → `animateScrollToItem(playlist.id)` (**bug: id used as index**; port should use `index`); long-press → `onLongClick(playlistInfo.playlist)`.
- Trailing extra item key `"add_playlist"` at index = playlists.size, same transforms, click → `onCreatePlaylist` (always, even off-centre; no long-press).

**`PlaylistArt(playlist: PlaylistInfo, modifier)`**: ShadowBox(no shadow, border 6, rounding 5 %) → Column(padding 12): if `artPath != null` AsyncImage(File, fillMaxWidth, 1:1, Crop) (if null, no placeholder — text moves up); Spacer 8; Text(name, bodyLarge, Normal, 2 lines); Text(`"${numOfSongs} songs • ${formatTimeToMinutes(playlistLength)}"`, labelSmall, text@0.6, ExtraLight, ls 0.5, 1 line).
`formatTimeToMinutes`: hours 0 → "N mins"; 1 → "1 hr M mins"; else "H hrs M mins".

**`AddPlaylistCard(modifier)`**: ShadowBox(no shadow, border 6, 5 %, bg accent2@0.1) → Column(fillMaxSize, padding 12, Center, CenterH): Icon(Outlined.Add, "Create playlist", 64 dp, accent2); Spacer 8; Text("CREATE PLAYLIST", accent2, labelLarge, ExtraBold, ls 1.5).

---

## 7. PlaybackScreen (`ui/screens/PlaybackScreen.kt`) — spinning vinyl card
```kotlin
fun PlaybackScreen(modifier = Modifier, song: Song, imageSize: Int = 100, playing: Boolean)
val rotation = remember { Animatable(0f) }
LaunchedEffect(playing) {
    if (playing) {
        rotation.animateTo(rotation.value + 360f, tween(10000, easing = LinearEasing))
        while (true) rotation.animateTo(rotation.value + 360f, tween(10000, easing = LinearEasing))
    }
}   // when playing → false the effect is cancelled, freezing the disc at its current angle; resumes from there
ShadowBox(modifier.padding(16.dp), shadowOffsetX = 4.dp, shadowOffsetY = 4.dp, borderThickness = 2.dp, rounding = 5)
  Box(Modifier.fillMaxWidth().padding(16.dp).graphicsLayer { rotationZ = rotation.value }, contentAlignment = Center)
    MusicRecord(song = song, imageSize = imageSize)   // canvas square = full inner width; 100dp circular art centred
```
- 1 revolution / 10 s, linear, infinite. Rotation value grows unbounded (fine for floats for a long time; desktop could use `% 360`).
- Equivalent desktop code: `rememberInfiniteTransition` is NOT equivalent (it wouldn't freeze/resume); keep the Animatable loop.
- `@Preview PlaybackScreenPreview` with a fake Song ("Blinding Lights", "The Weeknd", 200 000 ms).
- On a wide desktop window this square disc would be huge — consider `widthIn(max = …)`.

---

## 8. StatsScreen (`ui/screens/StatsScreen.kt`)

Params: `modifier, onClose, playbackControllerViewModel, statsViewModel`. Flows: `uiState`, `stats: StatsSnapshot` (`statsViewModel.stats`), `sleepTimerState`. `LaunchedEffect(Unit) { statsViewModel.refresh() }`. `BackHandler(onBack = onClose)`.

`StatsSnapshot` fields used: `hasAnyListenTime, totalListenMs, totalSessions, averageSessionMs, currentStreak, longestStreak, completionRate (Float/Double, %), todayListenMs, listensToday, thisWeekListenMs, thisMonthListenMs, listensThisMonth, dailySeries: List<DailyStat{dayStartMs,totalMs}>, topSongsThisMonth/topSongsAllTime: List<SongStat{name, artistName, artPath?, totalMs, fullPlays}>, topArtists: List<ArtistStat{name, songCount, totalMs}>, topPlaylists: List<PlaylistStat{name, sessionCount, totalMs}>`.

Layout:
```
Scaffold(fillMaxSize, containerColor = background)
  bottomBar: Column(fillMaxWidth, navigationBarsPadding, End) { PlaybackControls(
      onBack/pause/next/repeat/shuffle → pbVm, addSong = {}, createPlaylist = {}, onStats = onClose, onSleepTimer = {}, sleepTimerActive) }
  LazyColumn(fillMaxSize, padding(innerPadding), padding(h12, v8), spacedBy 12)
    item Text("YOUR STATS", labelLarge, ExtraBold, ls 2, padding(h8, v4))
    if !hasAnyListenTime: item EmptyStatsCard
    else:
      item HeroTotalsCard
      item InsightsCardsRow
      item PeriodCardsRow
      if dailySeries.any{totalMs>0}: item ListeningChartCard(dailySeries)
      item TopListCard("TOP SONGS · THIS MONTH", "your most-played tracks lately", empty "No listens this month yet") { SongStatRow… }
      item TopListCard("TOP SONGS · ALL TIME", "your most-played tracks ever", "Keep listening to fill this up") { SongStatRow… }
      item TopListCard("TOP ARTISTS", "artists you listen to the most", "No artist data yet") { ArtistStatRow… }
      item TopListCard("TOP PLAYLISTS", "playlists you play the most", "Play a playlist to see its stats here") { PlaylistStatRow… }
```
For each top list, `maxMs = list.first().totalMs` (bars are relative to #1).

Sub-composables (all private):
- **EmptyStatsCard**: ShadowBox(fillMaxWidth, alignment TopStart) → Column(padding 16): Text("NO LISTENING HISTORY YET", labelSmall, ExtraBold, ls 1.5); Spacer 8; Text("Play any song and your listening stats — total time, top songs, favourite playlists — will show up here, just like Spotify Wrapped.", text@0.7, bodySmall).
- **HeroTotalsCard**: ShadowBox(bg itemsBg, TopStart) → Column(padding 20): Text("ALL-TIME LISTENING", labelSmall ExtraBold ls 1.5); Spacer 6; Text(formatListenTime(totalListenMs), accent1, titleMedium, ExtraBold, 1 line); Spacer 8; Row(SpaceBetween){ Text("${totalSessions} sessions", text@0.6, bodySmall); Text("avg ${formatListenTime(averageSessionMs)}/session", same) }.
- **InsightsCardsRow**: Row(fillMaxWidth, spacedBy 10) of 2 `InsightCard`s weight 1:
  - "STREAK", value "${currentStreak}", sub "current · ${longestStreak} longest", accent1, icon "🔥"
  - "COMPLETION", value "${completionRate.toInt()}%", sub "songs finished", accent2, icon "✓"
- **InsightCard(label, value, sub, color, icon)**: ShadowBox(CenterStart) → Column(padding 12): Row(fillMaxWidth, SpaceBetween, Top){ Text(label, text@0.7, labelSmall, ExtraLight, ls 1); Text(icon, bodyMedium) }; Spacer 4; Text(value, color, headlineMedium, ExtraBold, 1 line); Spacer 4; Text(sub, text@0.5, labelSmall). (Emoji on desktop: Skia needs an emoji-capable fallback font — Windows has Segoe UI Emoji; verify rendering.)
- **PeriodCardsRow**: 3 `PeriodCard`s weight 1, spacedBy 10:
  - "TODAY", formatListenTime(todayListenMs), sub `listensToday>0 ? "${listensToday} sessions" : "no listens"`, accent1
  - "THIS WEEK", formatListenTime(thisWeekListenMs), "avg ${formatListenTime(thisWeekListenMs/7)}/day", accent2
  - "THIS MONTH", formatListenTime(thisMonthListenMs), "${listensThisMonth} sessions", accent3
- **PeriodCard**: ShadowBox(CenterStart) → Column(padding 12): Text(label, text@0.7, labelSmall, ExtraLight, ls 1); Spacer 4; Text(value, text, bodyLarge, ExtraBold, maxLines 2); Spacer 4; Box(4×28 dp, clip r2, bg color) colour stripe; sub?: Spacer 4 + Text(sub, text@0.5, labelSmall).
- **ListeningChartCard(series)**: ShadowBox(TopStart) → Column(padding 16): Text("LISTENING · LAST ${series.size} DAYS", labelSmall ExtraBold ls 1.5); Spacer 12; ListeningBarChart.
- **ListeningBarChart**: `max = series.maxOf{totalMs}`; Row(fillMaxWidth, spacedBy 4, Bottom); per day Column(weight 1, CenterH): Box(fillMaxWidth, height 96, BottomCenter){ Box(fillMaxWidth, fillMaxHeight(fraction), clip(topStart 3, topEnd 3), bg = totalMs>0 ? accent1 : cardBorder@0.25) } where `fraction = max>0 ? (totalMs/max).coerceIn(0.03, 1) : 0.03`; Spacer 4; Text(dayOfMonth(dayStartMs) as string, text@0.5, labelSmall, fontSize **9.sp** (fixed, overrides adaptive)). `dayOfMonth` via `java.util.Calendar` (JVM—works on desktop as-is). No bar animation.
- **TopListCard(title, subtitle, emptyLabel, hasData, content)**: ShadowBox(TopStart) → Column(padding 16): Text(title, labelSmall ExtraBold ls 1.5); Spacer 2; Text(subtitle, text@0.6, bodySmall); Spacer 12; hasData ? content() : Text(emptyLabel, text@0.5, bodySmall).
- **StatRowBase(rank, title, subtitle, valueLabel, shareValue, shareMax, color, leading)**: Row(fillMaxWidth, padding v6, CenterV): Text("${rank+1}", text@0.5, labelLarge, ExtraBold, width 28); leading(); Spacer 10; Column(weight 1){ Text(title, bodyMedium, Medium, 1 line); Text(subtitle, text@0.6, labelSmall, 1 line); Spacer 4; FillBar(value=shareValue, max=shareMax, color, secondaryLabel=valueLabel) }.
- **SongStatRow**: title name, subtitle artistName, valueLabel "${formatListenTime(totalMs)} · ${fullPlays} plays", colour accent1, leading AsyncImage(File(artPath.orEmpty()), 40 dp, clip r3, Crop).
- **ArtistStatRow**: subtitle "${songCount} songs", valueLabel formatListenTime(totalMs), accent2, leading Box(40 dp, r3, bg accent2@0.2, Center){ Text(name.take(2).uppercase(), bodySmall, ExtraBold) }.
- **PlaylistStatRow**: subtitle "${sessionCount} sessions", formatListenTime, accent3, leading Box(40, r3, bg accent3@0.25) with initials.

`formatListenTime(ms)`: ≤0 → "0 mins"; minutes = ms/60000; hours part "" / "1 hr" / "N hrs"; minute part "M mins" if M≠0 or hours==0; joined with space (e.g. "45 mins", "1 hr 12 mins", "5 hrs").

---

## 9. Android-specific APIs used in the UI → desktop equivalents

| Android API | Where | Desktop (Compose Multiplatform/JVM) equivalent |
|---|---|---|
| `ComponentActivity`, `setContent`, `enableEdgeToEdge` | MainActivity | `fun main() = application { Window(...) { … } }`; no edge-to-edge needed |
| `@AndroidEntryPoint`, `@Inject`, `hiltViewModel()` | MainActivity, screens | Manual DI / Koin (`koinViewModel()`/`koinInject()`), or plain singletons + `remember { ViewModel() }`; CMP has `androidx.lifecycle.viewmodel.compose.viewModel` too |
| `lifecycleScope.launch` | MainActivity first-run | `LaunchedEffect(Unit)` in root, or a coroutine in `main` before `application{}` |
| `rememberSaveable` | showStats | `remember` is enough (no config changes); `rememberSaveable` also exists in CMP |
| `BackHandler` (androidx.activity.compose) | Home, Stats | Esc key: `Modifier.onPreviewKeyEvent { it.key == Key.Escape && it.type == KeyDown }` at the window root dispatching to the current screen's back action; optionally a visible back/close button; mouse "back" button (`PointerButton.Back`) |
| `LocalConfiguration.current.screenWidthDp` | Type.kt adaptiveSp, MusicCarousel, PlaylistCarousel | Window width in dp: `LocalWindowInfo.current.containerSize.width / density`, or `BoxWithConstraints { maxWidth }`; provide via a CompositionLocal at the root |
| `R.font.*` + `Font(resId, weight)` | Type.kt | Compose Resources `Font(Res.font.jetbrains_mono_nl_regular, FontWeight.Normal)` (composable), or `androidx.compose.ui.text.platform.Font("font/…ttf", weight)` from classpath resources |
| `painterResource(R.drawable.art_placeholder)` | MusicRecord (preview only) | `painterResource(Res.drawable.art_placeholder)` (Compose Resources) |
| `LocalInspectionMode` | MusicRecord | Exists in CMP (desktop @Preview); can keep |
| Coil 2 `coil.compose.AsyncImage(model = File / String path)` | everywhere for art | Coil 3 (`io.coil-kt.coil3:coil-compose`, multiplatform, supports `File`/path on JVM), or Kamel, or manual `File.readBytes().decodeToImageBitmap()` (`org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()`) cached in `produceState`. Keep `ContentScale.Crop` & sizes. Add placeholder/error painter (Android shows blank on missing file). |
| `rememberSnapFlingBehavior(lazyListState)` | carousels | Available in CMP foundation (`androidx.compose.foundation.gestures.snapping`) — works for touchpad flings; add mouse-wheel→horizontal mapping and mouse drag (`Modifier.draggable(rememberDraggableState { listState.dispatchRawDelta(-it) })` + snap on stop) |
| `combinedClickable(onLongClick)` / `detectTapGestures(onLongPress)` | carousels, CustomButton | Same APIs on desktop (long-press = hold mouse ~400 ms). Add right-click as context-menu trigger (`Modifier.onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary))` with `@ExperimentalFoundationApi`, or `ContextMenuArea`) |
| `graphicsLayer { rotationY, cameraDistance = 12f * density }` | carousels | Identical in CMP (Skia supports 3D rotationY with camera distance) |
| `Canvas` / `drawCircle` / `Stroke` | MusicRecord | Identical |
| `Animatable`, `tween`, `LinearEasing`, `animateDpAsState`, `animateFloatAsState`, `AnimatedVisibility`, `animateContentSize` | various | Identical |
| Material3 `AlertDialog`, `Scaffold`, `Card`, `OutlinedTextField`, `Checkbox`, `IconButton`, `TextButton`, `LinearProgressIndicator`, `CircularProgressIndicator` | various | `org.jetbrains.compose.material3` — identical |
| `material-icons-extended` (`Icons.Outlined.*`, `Icons.Default.*`, `AutoMirrored`) | various | `org.jetbrains.compose.material:material-icons-extended` (same names). Icons used: Outlined.{ArrowDownward, ArrowUpward, Close, List, Refresh, Reorder, Search, ViewCarousel, Add, ArrowBackIosNew, Bedtime, Delete, Edit, Insights, Pause, PlayArrow, Repeat, RepeatOne, Shuffle, Check}, AutoMirrored.Outlined.ArrowForwardIos, Default/Filled.{ArrowDownward, ArrowUpward, Headphones, KeyboardArrowDown, KeyboardArrowUp, MusicNote, Schedule} |
| `navigationBarsPadding()` / Scaffold window insets | Home, Stats | No-op on desktop (insets are zero); can remove |
| `java.io.File`, `java.util.Calendar`, `String.format` | various | Work as-is on JVM desktop |
| `@Preview` (androidx.compose.ui.tooling.preview) | PlaybackScreen, CustomButton | `org.jetbrains.compose.ui.tooling.preview.Preview` (desktop) or drop |
| Toast / haptics / vibration | **not used** in UI layer | n/a |

Desktop-only considerations (not in Android):
- Hover states: none exist on Android; adding `PointerIcon.Hand` on clickable cards/buttons is recommended but cosmetic.
- Nested scrolling: Home's LazyColumn contains a LazyColumn (list mode, `heightIn(max=500)`) and ReorderableSongList (fixed 500 dp). Works the same on desktop; mouse wheel scrolls the inner list when hovered.
- Vertical scrollbar: Android has none; desktop users expect one (`VerticalScrollbar(rememberScrollbarAdapter(listState))`) — optional.
- Window min size: design is phone-first (~360–420 dp wide). Suggest `Window(state = rememberWindowState(width = 420.dp, height = 860.dp))` for a 1:1 look, or constrain content width.

---

## 10. ViewModel surface used by the UI (for wiring the port)

- `HomeScreenViewModel`: flows `sortedSongs, searchResults, searchQuery, songSortMode, playlistSortMode, sortedPlaylists, state, songsInPlaylist, playlistId`; methods `onSearchQueryChange(String)`, `onSongSortModeToggle()`, `onPlaylistSortModeToggle()`, `loadPlaylist(id: Int)`, `clearSelectedPlaylist()`, `reorderSongsInPlaylist(from, to)`, `deleteSong(id)`, `deleteSongFromPlaylist(index = songId, playlistId)`, `deletePlaylist(id)`, `addSongsToPlaylist(Playlist, Set<Song>)`, `createPlaylistWithSongs(name, Set<Song>)`, `renameSong(Song, String)`, `renamePlaylist(Playlist, String)`, `resetState()`.
- `PlaybackControllerViewModel`: flows `uiState`, `sleepTimerState`; methods `playSongs(list, index, playlistId: Int?)`, `previous()`, `pause()` (play/pause toggle), `next()`, `toggleRepeatMode()`, `toggleShuffleMode()`, `startSleepTimer(SleepTimerDuration)`, `cancelSleepTimer()`.
- `AddSongsViewModel`: flows `state`, `jobs`; methods `addSingleSong(url)`, `addPlaylist(url)`, `resetState()`, `cancel(jobId)`, `retry(jobId)`, `clearFinished()`.
- `StatsViewModel`: flow `stats`; method `refresh()`.

---

## 11. Complete user-visible string inventory

Headers: "PLAYLISTS", `<PLAYLIST NAME UPPERCASED>`, "ALL SONGS", "USE ARROWS TO REORDER SONGS", "YOUR STATS", "DOWNLOADS", "DOWNLOADS · N ACTIVE".
Download queue: "Clear", "Show less", "Show N more", "Playlist · ", "Song · ", "Waiting in queue", "Downloading A of B", "Downloading…", "Done · N songs", "Done", "Failed · <err>", "Failed", "Cancelled".
Lists/cards: "search your library", "<m:ss> · N plays", "<m:ss> • N plays", "N songs", "N songs • <H hrs M mins>", "CREATE PLAYLIST", "Create playlist" (cd).
Dialogs: "ADD SONGS", "PLAYLIST URL", "SINGLE VIDEO URL", "Paste playlist URL", "Paste video URL", "FAILED: <msg>", "ADD TO QUEUE", "CANCEL", "PLAYLISTS", "ADD TO EXISTING", "CREATE NEW", "NO PLAYLISTS YET - SWITCH TO CREATE NEW", "SELECT PLAYLIST", "Playlist name", "SELECT SONGS (N SELECTED)", "FIND SONG...", "SAVING...", "SAVED", "SAVE", "ADD TO PLAYLIST", "Song: <name>", "ADDING...", "ADDED", "ADD", "RENAME", "DELETE", "ADD SONGS", "Playlist", "RENAME SONG", "RENAME PLAYLIST", "Current name: <name>", "Enter new name", "DELETE SONG", "DELETE PLAYLIST", `Are you sure you want to delete "<name>"?`, "DON'T SHOW AGAIN", "SLEEP TIMER", "Timer Active", "MM:SS", "CANCEL TIMER", "Set sleep timer duration", "5m" "10m" "15m" "30m" "45m" "60m", "CLOSE".
Stats: "NO LISTENING HISTORY YET", "Play any song and your listening stats — total time, top songs, favourite playlists — will show up here, just like Spotify Wrapped.", "ALL-TIME LISTENING", "N sessions", "avg X/session", "STREAK", "🔥", "current · N longest", "COMPLETION", "✓", "N%", "songs finished", "TODAY", "no listens", "THIS WEEK", "avg X/day", "THIS MONTH", "LISTENING · LAST N DAYS", "TOP SONGS · THIS MONTH", "your most-played tracks lately", "No listens this month yet", "TOP SONGS · ALL TIME", "your most-played tracks ever", "Keep listening to fill this up", "TOP ARTISTS", "artists you listen to the most", "No artist data yet", "TOP PLAYLISTS", "playlists you play the most", "Play a playlist to see its stats here", "X · N plays".
Content descriptions: "Exit reorder mode", "Reorder songs", "Switch to carousel view", "Switch to list view", "Cancel download", "Retry download", "Sort field", "Sort direction", "Move up", "Move down", "Create playlist", "Done", "Edit".
