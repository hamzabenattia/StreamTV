# StreamTV — a Netflix-style streaming app for Android TV / Google TV / Fire TV

- **Home**: big "Featured" banner, then rows you scroll sideways — Continue Watching, your favorite
  live channels, Popular Classic Movies, Sci-Fi & Horror, Film Noir, Comedy, Westerns, Classic TV Shows, Cartoons.
- **Live TV**: a live **preview window** plays whichever channel you highlight. Filter by **Country**
  (🇹🇳 Tunisia and Arab countries first) and **Category** (News, Sports, Movies, Kids, Religious…).
  While watching: ▲/▼ or CH+/CH− zaps channels. Hold OK on a channel to favorite it.
- **Tunisian & Arabic**: Tunisian channels row on Home, plus "Tunisia" and "Arabic Cinema" movie rows.
- **Subtitles**: Arabic (or French/English) picked automatically when available; CC button in the player.
  Add a free OpenSubtitles API key in Settings to fetch Arabic subtitles for movies.
- **Search**: searches movies/shows *and* your live channels.
- **Settings**: paste a playlist URL, or pick one of the free public playlists from iptv-org.

Movies and shows come from the Internet Archive's public-domain film collections, so everything is free and legal.
Use only IPTV playlists you have the right to watch.

## Get the APK

**Option A — no install needed (GitHub):** push this folder to a GitHub repo. The *Build APK* workflow
runs automatically; open the run in the **Actions** tab and download the `StreamTV-apk` artifact.

**Option B — Android Studio:** open this folder, wait for Gradle sync, then *Build → Build APK(s)*,
or run `./gradlew assembleRelease`. The APK is at `app/build/outputs/apk/release/app-release.apk`.

## Install it on your TV

1. On the TV: *Settings → Device Preferences → About* → click **Build** 7 times to enable Developer options,
   then turn on **USB debugging / Network debugging** (Fire TV: *My Fire TV → Developer options → ADB debugging*).
2. From your computer on the same Wi‑Fi: `adb connect <TV-IP-address>` then `adb install app-release.apk`.
   Or use the **Downloader** / **Send Files to TV** app to sideload the APK without a computer.
3. StreamTV appears in the TV's app row.

## Project layout

```
app/src/main/java/com/streamtv/app/
  MainActivity.kt          entry point
  MainViewModel.kt         app state: channels, movie rows, search, favorites
  data/M3uParser.kt        playlist parser (#EXTINF, logos, groups, user-agent/referrer)
  data/Repository.kt       network: playlists + Internet Archive catalog & stream lookup
  data/Prefs.kt            saved playlist, favorites, continue-watching
  player/PlayerActivity.kt ExoPlayer (HLS/DASH/MP4/TS), channel zapping, auto-retry as HLS
  ui/                      Compose for TV screens (Home, Live TV, Search, Settings, Details)
```

## Ideas for next steps
- EPG (TV guide "now/next") via the playlist's `url-tvg` XMLTV file
- Multiple saved playlists, parental PIN, TMDB artwork for nicer posters
