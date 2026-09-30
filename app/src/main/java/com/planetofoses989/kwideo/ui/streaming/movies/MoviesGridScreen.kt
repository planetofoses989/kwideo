package com.planetofoses989.kwideo.ui.streaming.movies

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
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
import com.planetofoses989.kwideo.repository.intro.IntroSkipRepository
import com.planetofoses989.kwideo.domain.streaming.model.LocalMovie
import com.planetofoses989.kwideo.presentation.Screen
import com.planetofoses989.kwideo.presentation.components.pullrefresh.PullRefreshBox
import com.planetofoses989.kwideo.ui.browser.LocalNavigationBarHeight
import com.planetofoses989.kwideo.ui.browser.states.EmptyState
import com.planetofoses989.kwideo.ui.browser.dialogs.DeleteConfirmationDialog
import com.planetofoses989.kwideo.ui.streaming.components.MoviePosterCard
import com.planetofoses989.kwideo.ui.theme.rememberGlassHazeState
import com.planetofoses989.kwideo.ui.utils.LocalBackStack
import com.planetofoses989.kwideo.ui.utils.LocalDetailPaneBack
import com.planetofoses989.kwideo.utils.media.MediaLibraryEvents
import com.planetofoses989.kwideo.utils.media.MediaUtils
import com.planetofoses989.kwideo.utils.permission.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object MoviesGridScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val seriesDetector = koinInject<SeriesDetector>()
    val metadataRepository = koinInject<StreamingMetadataRepository>()
    val introSkipRepository = koinInject<IntroSkipRepository>()
    val navigationBarHeight = LocalNavigationBarHeight.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val columns = when {
      isTablet -> if (isLandscape) 5 else 4
      isLandscape -> 4
      else -> 3
    }

    var movieList by remember { mutableStateOf<List<LocalMovie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var moviePendingDeletion by remember { mutableStateOf<LocalMovie?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val isRefreshing = remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    // Master-detail on wide screens when this grid is the tab root.
    // Pushed instances (e.g. from home See-all) stay single-pane.
    var selectedMovieId by rememberSaveable { mutableStateOf<Long?>(null) }
    val twoPane = configuration.screenWidthDp >= 840 && backstack.size == 1
    BackHandler(enabled = twoPane && selectedMovieId != null) {
      selectedMovieId = null
    }
    val atTop by remember {
      derivedStateOf {
        gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
      }
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
      withContext(Dispatchers.IO) {
        val detected = seriesDetector.detectLibrary()
        // Drop cached TMDB entries for deleted movies.
        // Remaining movies reuse their cache with no re-scrape.
        runCatching {
          metadataRepository.pruneStaleMetadata(
            presentMovieTitles = detected.movies.map { it.title }.toSet(),
          )
        }
        movieList = detected.movies
        isLoading = false

        // Background enrichment
        val enriched = detected.movies.map { metadataRepository.enrichMovie(it) }
        movieList = enriched
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

    val filteredMovies = remember(movieList, searchQuery) {
      if (searchQuery.isBlank()) {
        movieList
      } else {
        movieList.filter { it.title.lowercase().contains(searchQuery.lowercase()) }
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
                placeholder = { Text("Search movies...") },
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
                text = "Movies",
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
            MoviesGridContent(
              innerPadding = innerPadding,
              isLoading = isLoading,
              filteredMovies = filteredMovies,
              searchQuery = searchQuery,
              onClearSearch = { searchQuery = "" },
              columns = columns,
              gridState = gridState,
              navigationBarHeight = navigationBarHeight,
              isRefreshing = isRefreshing,
              onRefresh = { refreshKey++ },
              refreshEnabled = atTop && !isLoading,
              onMovieClick = { selectedMovieId = it.video.id },
              onMovieLongClick = { moviePendingDeletion = it },
            )
          }
          VerticalDivider(modifier = Modifier.fillMaxHeight())
          Box(
            modifier = Modifier
              .weight(0.58f)
              .fillMaxHeight(),
          ) {
            MovieDetailPane(
              selectedMovieId = selectedMovieId,
              movieList = movieList,
              onClearSelection = { selectedMovieId = null },
            )
          }
        }
      } else {
        MoviesGridContent(
          innerPadding = innerPadding,
          isLoading = isLoading,
          filteredMovies = filteredMovies,
          searchQuery = searchQuery,
          onClearSearch = { searchQuery = "" },
          columns = columns,
          gridState = gridState,
          navigationBarHeight = navigationBarHeight,
          isRefreshing = isRefreshing,
          onRefresh = { refreshKey++ },
          refreshEnabled = atTop && !isLoading,
          onMovieClick = { backstack.add(MovieDetailScreen(it.video.id, it.title)) },
          onMovieLongClick = { moviePendingDeletion = it },
        )
      }
    }

    moviePendingDeletion?.let { movie ->
      DeleteConfirmationDialog(
        isOpen = true,
        onDismiss = { moviePendingDeletion = null },
        onConfirm = {
          coroutineScope.launch {
            PermissionUtils.StorageOps.deleteVideos(context, listOf(movie.video))
            // Movie file is gone: drop its cached TMDB entry immediately.
            runCatching { metadataRepository.clearMovieMetadata(movie.title) }
            // Movie file is gone: drop its cached intro/recap windows too.
            runCatching { introSkipRepository.evictAll(listOf(movie.video.displayName, movie.title)) }
            movieList = movieList.filterNot { it.video.id == movie.video.id }
          }
        },
        itemType = "movie",
        itemCount = 1,
        itemNames = listOf(movie.title),
      )
    }
  }
}

@Composable
private fun MoviesGridContent(
  innerPadding: PaddingValues,
  isLoading: Boolean,
  filteredMovies: List<LocalMovie>,
  searchQuery: String,
  onClearSearch: () -> Unit,
  columns: Int,
  gridState: LazyGridState,
  navigationBarHeight: Dp,
  isRefreshing: MutableState<Boolean>,
  onRefresh: suspend () -> Unit,
  refreshEnabled: Boolean,
  onMovieClick: (LocalMovie) -> Unit,
  onMovieLongClick: (LocalMovie) -> Unit,
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
  } else if (filteredMovies.isEmpty()) {
    EmptyState(
      icon = Icons.Filled.Movie,
      title = if (searchQuery.isNotBlank()) "No movies found" else "No movies detected",
      message = if (searchQuery.isNotBlank()) "No movies match '$searchQuery'" else "Add movie files to your device storage to see them here",
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
        items(filteredMovies, key = { "grid_movie_${it.video.id}" }) { movie ->
          MoviePosterCard(
            movie = movie,
            onClick = { onMovieClick(movie) },
            onLongClick = { onMovieLongClick(movie) },
            cardWidth = 180.dp,
          )
        }
      }
    }
  }
}

@Composable
private fun MovieDetailPane(
  selectedMovieId: Long?,
  movieList: List<LocalMovie>,
  onClearSelection: () -> Unit,
) {
  val selected = movieList.find { it.video.id == selectedMovieId }
  Box(modifier = Modifier.fillMaxSize()) {
    if (selected == null) {
      EmptyState(
        icon = Icons.Filled.Movie,
        title = "Select a movie",
        message = "Pick a movie to see details, actions and matches here",
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      key(selected.video.id) {
        CompositionLocalProvider(LocalDetailPaneBack provides onClearSelection) {
          MovieDetailScreen(selected.video.id, selected.title).Content()
        }
      }
    }
  }
}
