package com.streamtv.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamtv.app.data.Channel
import com.streamtv.app.data.Countries
import com.streamtv.app.data.Movie
import com.streamtv.app.data.PlayableItem
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

    /** Country codes and categories present in the playlist, for the filter chips. */
    var countries by mutableStateOf<List<String>>(emptyList()); private set
    var categories by mutableStateOf<List<String>>(emptyList()); private set

    /** null = all countries. Starts on Tunisia when the playlist has Tunisian channels. */
    var countryFilter by mutableStateOf<String?>(null)

    /** null = all categories; [FAVORITES] = favorites only. */
    var categoryFilter by mutableStateOf<String?>(null)

    fun filteredChannels(): List<Channel> = channels.filter { ch ->
        (countryFilter == null || ch.country == countryFilter) &&
            when (val cat = categoryFilter) {
                null -> true
                FAVORITES -> ch.url in favorites
                else -> cat in ch.categories
            }
    }

    fun channelsIn(group: String): List<Channel> = when (group) {
        ALL -> channels
        FAVORITES -> channels.filter { it.url in favorites }
        else -> channels.filter { it.group == group }
    }

    fun channelsFrom(country: String): List<Channel> =
        channels.filter { it.country == country }.distinctBy { it.name.substringBefore(" (") }

    // ---- Player settings ----
    var subtitleLanguage by mutableStateOf(prefs.subtitleLanguage); private set
    var openSubtitlesKey by mutableStateOf(prefs.openSubtitlesKey); private set

    fun updateSubtitleLanguage(code: String) {
        subtitleLanguage = code
        prefs.subtitleLanguage = code
    }

    fun updateOpenSubtitlesKey(key: String) {
        openSubtitlesKey = key.trim()
        prefs.openSubtitlesKey = key
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
                .onSuccess { list ->
                    channels = list
                    countries = Countries.sort(list.mapNotNull { it.country }.toSet())
                    categories = list.flatMap { it.categories }.distinct().sortedBy { it.lowercase() }
                    if (countryFilter !in countries) countryFilter = if ("tn" in countries) "tn" else null
                    if (categoryFilter != FAVORITES && categoryFilter !in categories) categoryFilter = null
                    if (list.isEmpty()) channelsError = "No channels found in this playlist."
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
            runCatching { repo.searchArchive("(title:($q) OR subject:($q)) AND ($SEARCH_COLLECTIONS)", rows = 60) }
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

    /** Finds the video file and subtitles (Archive's own, plus OpenSubtitles when a key is set). */
    suspend fun prepareMovie(movie: Movie): PlayableItem? {
        val stream = repo.resolveArchiveStream(movie.id) ?: return null
        val lang = subtitleLanguage
        val extra = if (lang.isNotEmpty() && openSubtitlesKey.isNotEmpty() && stream.subtitles.none { it.language == lang }) {
            runCatching { repo.findOpenSubtitle(openSubtitlesKey, movie.title, movie.year, lang) }.getOrNull()
        } else null
        return PlayableItem(movie.title, stream.url, isLive = false, subtitles = stream.subtitles + listOfNotNull(extra))
    }

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
            "Tunisia · تونس" to "(tunisia OR tunisian OR tunisie OR subject:tunisia) AND NOT collection:podcasts",
            "Arabic Cinema · سينما عربية" to "language:(ara OR arabic) OR (subject:arabic AND collection:feature_films)",
            "Sci-Fi & Horror" to "collection:SciFi_Horror",
            "Film Noir" to "collection:film_noir",
            "Comedy" to "collection:feature_films AND subject:comedy",
            "Westerns" to "collection:feature_films AND subject:western",
            "Classic TV Shows" to "collection:classic_tv",
            "Cartoons" to "collection:classic_cartoons",
        )

        private const val SEARCH_COLLECTIONS =
            "collection:feature_films OR collection:SciFi_Horror OR collection:film_noir OR " +
                "collection:classic_tv OR collection:classic_cartoons OR language:(ara OR arabic) OR tunisia"
    }
}
