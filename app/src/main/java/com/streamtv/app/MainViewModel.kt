package com.streamtv.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamtv.app.data.Channel
import com.streamtv.app.data.Movie
import com.streamtv.app.data.Prefs
import com.streamtv.app.data.Repository
import com.streamtv.app.data.Shelf
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository()
    private val prefs = Prefs(app)

    // ---- Live TV ----
    var playlistUrl by mutableStateOf(prefs.playlistUrl); private set
    var channels by mutableStateOf<List<Channel>>(emptyList()); private set
    var channelsLoading by mutableStateOf(false); private set
    var channelsError by mutableStateOf<String?>(null); private set
    var favorites by mutableStateOf(prefs.favorites); private set

    val groups: List<String>
        get() = buildList {
            if (channels.any { it.url in favorites }) add(FAVORITES)
            add(ALL)
            addAll(channels.map { it.group }.distinct().sortedBy { it.lowercase() })
        }

    fun channelsIn(group: String): List<Channel> = when (group) {
        ALL -> channels
        FAVORITES -> channels.filter { it.url in favorites }
        else -> channels.filter { it.group == group }
    }

    // ---- Movies & shows ----
    var shelves by mutableStateOf<List<Shelf>>(emptyList()); private set
    var featured by mutableStateOf<Movie?>(null); private set
    var moviesLoading by mutableStateOf(false); private set
    var moviesError by mutableStateOf<String?>(null); private set
    var recent by mutableStateOf(prefs.recentMovies); private set

    // ---- Search ----
    var searchResults by mutableStateOf<List<Movie>>(emptyList()); private set
    var searching by mutableStateOf(false); private set
    var searchError by mutableStateOf<String?>(null); private set
    private var searchJob: Job? = null

    init {
        loadPlaylist()
        loadMovies()
    }

    fun loadPlaylist(url: String = playlistUrl) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        playlistUrl = trimmed
        prefs.playlistUrl = trimmed
        viewModelScope.launch {
            channelsLoading = true
            channelsError = null
            runCatching { repo.loadPlaylist(trimmed) }
                .onSuccess {
                    channels = it
                    if (it.isEmpty()) channelsError = "No channels found in this playlist."
                }
                .onFailure { channelsError = "Couldn't load playlist: ${it.message}" }
            channelsLoading = false
        }
    }

    fun loadMovies() {
        viewModelScope.launch {
            moviesLoading = true
            moviesError = null
            val loaded = CATEGORIES.map { (title, query) ->
                async { runCatching { Shelf(title, repo.searchArchive(query)) }.getOrNull() }
            }.awaitAll().filterNotNull().filter { it.movies.isNotEmpty() }
            shelves = loaded
            featured = loaded.firstOrNull()?.movies
                ?.take(15)?.filter { !it.description.isNullOrBlank() }?.randomOrNull()
                ?: loaded.firstOrNull()?.movies?.firstOrNull()
            if (loaded.isEmpty()) moviesError = "Couldn't reach the movie library. Check your internet connection."
            moviesLoading = false
        }
    }

    fun search(text: String) {
        // Keep only characters that are safe inside an Archive search query.
        val q = text.replace(Regex("[^\\p{L}\\p{N} '\\-]"), " ").trim()
        searchJob?.cancel()
        if (q.isEmpty()) {
            searchResults = emptyList(); return
        }
        searchJob = viewModelScope.launch {
            searching = true
            searchError = null
            runCatching { repo.searchArchive("title:($q) AND ($SEARCH_COLLECTIONS)", rows = 60) }
                .onSuccess { searchResults = it }
                .onFailure { searchError = "Search failed: ${it.message}" }
            searching = false
        }
    }

    fun searchChannels(text: String): List<Channel> {
        val q = text.trim()
        if (q.length < 2) return emptyList()
        return channels.filter { it.name.contains(q, ignoreCase = true) }.take(40)
    }

    fun toggleFavorite(channel: Channel) {
        favorites = if (channel.url in favorites) favorites - channel.url else favorites + channel.url
        prefs.favorites = favorites
    }

    suspend fun resolveStream(movie: Movie): String? = repo.resolveArchiveStream(movie.id)

    fun markWatched(movie: Movie) {
        recent = listOf(movie) + recent.filter { it.id != movie.id }
        prefs.recentMovies = recent
    }

    companion object {
        const val ALL = "All channels"
        const val FAVORITES = "★ Favorites"

        /** Home screen rows: title to Internet Archive query. All public domain / freely licensed. */
        val CATEGORIES = listOf(
            "Popular Classic Movies" to "collection:feature_films",
            "Sci-Fi & Horror" to "collection:SciFi_Horror",
            "Film Noir" to "collection:film_noir",
            "Comedy" to "collection:feature_films AND subject:comedy",
            "Westerns" to "collection:feature_films AND subject:western",
            "Classic TV Shows" to "collection:classic_tv",
            "Cartoons" to "collection:classic_cartoons",
        )

        private const val SEARCH_COLLECTIONS =
            "collection:feature_films OR collection:SciFi_Horror OR collection:film_noir OR " +
                "collection:classic_tv OR collection:classic_cartoons"
    }
}
