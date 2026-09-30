package com.planetofoses989.kwideo.ui.streaming.series

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import com.planetofoses989.kwideo.ui.theme.GlassKind
import com.planetofoses989.kwideo.ui.theme.LocalGlass
import com.planetofoses989.kwideo.ui.theme.glassChrome
import com.planetofoses989.kwideo.ui.theme.glassHazeStyle
import com.planetofoses989.kwideo.ui.theme.glassSearchBarColors
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.planetofoses989.kwideo.domain.streaming.SeriesDetector
import com.planetofoses989.kwideo.domain.streaming.StreamingMetadataRepository
import com.planetofoses989.kwideo.domain.streaming.model.LocalSeries
import com.planetofoses989.kwideo.presentation.Screen
import com.planetofoses989.kwideo.presentation.components.pullrefresh.PullRefreshBox
import com.planetofoses989.kwideo.ui.browser.LocalNavigationBarHeight
import com.planetofoses989.kwideo.ui.browser.states.EmptyState
import com.planetofoses989.kwideo.ui.streaming.components.SeriesPosterCard
import com.planetofoses989.kwideo.ui.theme.rememberGlassHazeState
import com.planetofoses989.kwideo.ui.utils.LocalBackStack
import com.planetofoses989.kwideo.ui.utils.LocalDetailPaneBack
import com.planetofoses989.kwideo.utils.media.MediaLibraryEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object SeriesGridScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val seriesDetector = koinInject<SeriesDetector>()
    val metadataRepository = koinInject<StreamingMetadataRepository>()
    val navigationBarHeight = LocalNavigationBarHeight.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val columns = when {
      isTablet -> if (isLandscape) 5 else 4
      isLandscape -> 4
      else -> 3
    }

    var seriesList by remember { mutableStateOf<List<LocalSeries>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val isRefreshing = remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    // Master-detail on wide screens when this grid is the tab root.
    // Pushed instances (e.g. from home See-all) stay single-pane.
    var selectedSeriesId by rememberSaveable { mutableStateOf<String?>(null) }
    val twoPane = configuration.screenWidthDp >= 840 && backstack.size == 1
    BackHandler(enabled = twoPane && selectedSeriesId != null) {
      selectedSeriesId = null
    }
    val atTop by remember {
      derivedStateOf {
        gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
      }
    }

    LaunchedEffect(refreshKey) {
      withContext(Dispatchers.IO) {
        val detected = seriesDetector.detectLibrary()
        // Drop cached TMDB entries for shows with zero episodes left.
        // Remaining shows reuse their cache with no re-scrape.
        runCatching {
          metadataRepository.pruneStaleMetadata(
            presentSeriesIds = detected.series.map { it.id }.toSet(),
          )
        }
        seriesList = detected.series
        isLoading = false

        // Background enrichment
        val enriched = detected.series.map { metadataRepository.enrichSeries(it) }
        seriesList = enriched
      }
    }

    // Refresh progress after exiting the player. The player persists its final
    // position in onStop/onDestroy, which can land after ON_RESUME, so also
    // refresh on the post-save notification to correct a stale read. Cached
    // enrichment only: no extra network calls.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
      var resumedOnce = false
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          if (resumedOnce) {
            refreshKey++
          } else {
            resumedOnce = true
          }
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
      MediaLibraryEvents.changes.collect { refreshKey++ }
    }

    val filteredSeries = remember(seriesList, searchQuery) {
      if (searchQuery.isBlank()) {
        seriesList
      } else {
        seriesList.filter { it.title.lowercase().contains(searchQuery.lowercase()) }
      }
    }

    Scaffold(
      topBar = {
        val isGlass = LocalGlass.current
        val glassHaze = rememberGlassHazeState()
        val glassStyle = glassHazeStyle(isDark = isSystemInDarkTheme(), kind = GlassKind.Bar)
        if (isSearching) {
          SearchBar(
            inputField = {
              SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text("Search TV shows...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                  IconButton(onClick = {
                    isSearching = false
                    searchQuery = ""
                  }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                  }
                },
              )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = glassSearchBarColors(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
          ) {}
        } else {
          TopAppBar(
            title = {
              Text(
                text = "Shows",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              )
            },
            navigationIcon = {
              if (backstack.size > 1) {
                IconButton(
                  onClick = {
                    if (backstack.size > 1) {
                      backstack.removeLastOrNull()
                    }
                  }
                ) {
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
              }
            },
            actions = {
              IconButton(onClick = { isSearching = true }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
              }
            },
            colors = if (isGlass) TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
              else TopAppBarDefaults.topAppBarColors(),
            modifier = Modifier.glassChrome(
              state = glassHaze,
              style = glassStyle,
              shape = RoundedCornerShape(0.dp),
              enabled = isGlass,
            ),
          )
        }
      },
    ) { innerPadding ->
      if (twoPane) {
        Row(modifier = Modifier.fillMaxSize()) {
          Box(
            modifier = Modifier
              .weight(0.42f)
              .fillMaxHeight(),
          ) {
            SeriesGridContent(
              innerPadding = innerPadding,
              isLoading = isLoading,
              filteredSeries = filteredSeries,
              searchQuery = searchQuery,
              onClearSearch = { searchQuery = "" },
              columns = columns,
              gridState = gridState,
              navigationBarHeight = navigationBarHeight,
              isRefreshing = isRefreshing,
              onRefresh = { refreshKey++ },
              refreshEnabled = atTop && !isLoading,
              onSeriesClick = { selectedSeriesId = it.id },
            )
          }
          VerticalDivider(modifier = Modifier.fillMaxHeight())
          Box(
            modifier = Modifier
              .weight(0.58f)
              .fillMaxHeight(),
          ) {
            SeriesDetailPane(
              selectedSeriesId = selectedSeriesId,
              seriesList = seriesList,
              onClearSelection = { selectedSeriesId = null },
            )
          }
        }
      } else {
        SeriesGridContent(
          innerPadding = innerPadding,
          isLoading = isLoading,
          filteredSeries = filteredSeries,
          searchQuery = searchQuery,
          onClearSearch = { searchQuery = "" },
          columns = columns,
          gridState = gridState,
          navigationBarHeight = navigationBarHeight,
          isRefreshing = isRefreshing,
          onRefresh = { refreshKey++ },
          refreshEnabled = atTop && !isLoading,
          onSeriesClick = { backstack.add(SeriesDetailScreen(it.id)) },
        )
      }
    }
  }
}

@Composable
private fun SeriesGridContent(
  innerPadding: PaddingValues,
  isLoading: Boolean,
  filteredSeries: List<LocalSeries>,
  searchQuery: String,
  onClearSearch: () -> Unit,
  columns: Int,
  gridState: LazyGridState,
  navigationBarHeight: Dp,
  isRefreshing: MutableState<Boolean>,
  onRefresh: suspend () -> Unit,
  refreshEnabled: Boolean,
  onSeriesClick: (LocalSeries) -> Unit,
) {
  if (isLoading) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator()
    }
  } else if (filteredSeries.isEmpty()) {
    EmptyState(
      icon = Icons.Filled.Tv,
      title = if (searchQuery.isNotBlank()) "No TV shows found" else "No TV shows detected",
      message = if (searchQuery.isNotBlank()) "No TV shows match '$searchQuery'" else "Add TV shows with S01E01 or season folders to see them here",
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      actionLabel = if (searchQuery.isNotBlank()) "Clear search" else null,
      onAction = if (searchQuery.isNotBlank()) {
        onClearSearch
      } else {
        null
      },
    )
  } else {
    PullRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      enabled = refreshEnabled,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = 12.dp,
          end = 12.dp,
          top = 8.dp,
          bottom = navigationBarHeight + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        items(filteredSeries, key = { "grid_series_${it.id}" }) { series ->
          SeriesPosterCard(
            series = series,
            onClick = { onSeriesClick(series) },
            cardWidth = 180.dp,
          )
        }
      }
    }
  }
}

@Composable
private fun SeriesDetailPane(
  selectedSeriesId: String?,
  seriesList: List<LocalSeries>,
  onClearSelection: () -> Unit,
) {
  val selected = seriesList.find { it.id == selectedSeriesId }
  Box(modifier = Modifier.fillMaxSize()) {
    if (selected == null) {
      EmptyState(
        icon = Icons.Filled.Tv,
        title = "Select a TV show",
        message = "Pick a show to see seasons, episodes and actions here",
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      key(selected.id) {
        CompositionLocalProvider(LocalDetailPaneBack provides onClearSelection) {
          SeriesDetailScreen(selected.id).Content()
        }
      }
    }
  }
}
