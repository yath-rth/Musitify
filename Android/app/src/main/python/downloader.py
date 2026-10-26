import yt_dlp
import time

import ast
import json
import os
import re

_TOPIC = re.compile(r"\s+-\s+topic$", re.I)
_VEVO = re.compile(r"\s*vevo$", re.I)
_OFFICIAL_SUFFIX = re.compile(r"\s*[-|–]?\s*\(?\bofficial(?:\s+artist)?(?:\s+channel)?\)?$", re.I)
_OFFICIAL_PREFIX = re.compile(r"^official\s+", re.I)
_CAMEL = re.compile(r"(?<=[a-z])(?=[A-Z])")


def clean_artist(name):
    """Strips channel noise: 'X - Topic', 'XVEVO', 'X Official', 'Official X'."""
    original = name.strip()
    s = original
    while True:
        before = s
        s = _TOPIC.sub("", s).strip()
        if _VEVO.search(s):
            s = _VEVO.sub("", s).strip()
            if " " not in s:
                s = _CAMEL.sub(" ", s)  # DaftPunkVEVO -> Daft Punk
        s = _OFFICIAL_SUFFIX.sub("", s).strip()
        s = _OFFICIAL_PREFIX.sub("", s).strip()
        if s == before:
            break
    return s or original


def pick_artist(info):
    """artist -> artists (joined) -> uploader -> 'Unknown Artist'. Skips missing/blank values."""
    artist = info.get("artist")
    if isinstance(artist, str) and artist.strip():
        return clean_artist(artist)
    artists = [a.strip() for a in (info.get("artists") or []) if isinstance(a, str) and a.strip()]
    if artists:
        return ", ".join(artists)
    uploader = info.get("uploader")
    if isinstance(uploader, str) and uploader.strip():
        return clean_artist(uploader)
    return "Unknown Artist"


_BROKEN_ARTISTS = {"", "none", "null", "unknown"}


def fix_artist(stored, url):
    """One-time repair of an already stored artist (see LegacyFixer.fixArtists).

    - "['A', 'B']" (old str(list) bug) -> "A, B", no network needed
    - "None" / "null" / "unknown" / "" -> re-read from YouTube (raises if that fails)
    - anything else -> channel noise cleaned
    """
    s = (stored or "").strip()
    if s.startswith("["):
        try:
            names = [str(a).strip() for a in ast.literal_eval(s) if str(a).strip()]
            if names:
                return ", ".join(names)
        except (ValueError, SyntaxError):
            pass
    if s.lower() in _BROKEN_ARTISTS or s.startswith("["):
        if not url or not url.startswith("http"):
            return "Unknown Artist"
        opts = {"quiet": True, "no_warnings": True, "skip_download": True, "noplaylist": True}
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
        return pick_artist(info)
    return clean_artist(s)


def downloadSong(url, output_path, retries=3, delay=3):
    ydl_opts = {
        "outtmpl": f"{output_path}/%(title)s-%(uploader)s-%(release_date)s.%(ext)s",
        "format": "bestaudio/best",
        "writethumbnail": True,
        # "quiet": True,
        "no_warnings": True,
    }

    last_error = None

    for attempt in range(retries):
        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=True)
                audio_path = ydl.prepare_filename(info)
                base = os.path.splitext(audio_path)[0]
                
                thumbnail_path = None
                for ext in (".webp", ".jpg", ".png"):
                    path = base + ext
                    if os.path.exists(path):
                        thumbnail_path = path
                        break
                
                artist = pick_artist(info)

                return {
                    "audio": audio_path,
                    "title": info.get("title"),
                    "artist": artist,
                    "thumbnail": thumbnail_path,
                    "duration": info.get("duration")
                }

        except Exception as e:
            last_error = e
            if attempt < retries - 1:
                time.sleep(delay)

    raise last_error

def search_youtube(query, limit=8):
    """Top YouTube matches as a JSON string: [{id, url, title, artist, duration, thumbnail}]."""
    opts = {"extract_flat": True, "quiet": True, "no_warnings": True, "skip_download": True}
    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(f"ytsearch{int(limit)}:{query}", download=False)
    results = []
    for entry in (info.get("entries") or []):
        vid = entry.get("id") if entry else None
        if not vid:
            continue
        results.append({
            "id": vid,
            "url": f"https://www.youtube.com/watch?v={vid}",
            "title": entry.get("title") or "Unknown title",
            "artist": pick_artist(entry),
            "duration": entry.get("duration"),
            "thumbnail": f"https://i.ytimg.com/vi/{vid}/mqdefault.jpg",
        })
    return json.dumps(results)


def get_stream_url(url):
    """Direct audio stream for a quick preview, without downloading anything."""
    opts = {
        "format": "bestaudio[ext=m4a]/bestaudio/best",
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "noplaylist": True,
    }
    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=False)
    return info["url"]


def get_playlist_urls(playlist_url):
    ydl_opts = {"extract_flat": True, "quiet": True}
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(playlist_url, download=False)
        urls = [entry["url"] for entry in info["entries"] if entry]
        playlist_name = info.get("title", "Unknown Playlist")
        return {"name": playlist_name, "urls": urls}
    
def main():
    # Test downloading a song
    test_download()

    # Test retrieving playlist URLs (uncomment to use)
    # test_get_playlist_urls()


def test_download():
    url = "https://youtu.be/NxgyfY70-xQ"
    output_dir = "./downloads"
    
    print(f"\nTesting downloadSong() with URL: {url}")
    print("-" * 50)
    
    try:
        result = downloadSong(url, output_dir, retries=3, delay=3)
        
        if 'audio' in result and os.path.exists(result['audio']):
            print("✅ Download successful!")
            print(f"- Audio file path: {result['audio']}")
            print(f"- Artist: {result.get('artist', 'N/A')}")
            print(f"- Duration: {result.get('duration')} seconds")
            
            if result.get('thumbnail'):
                thumbnail_ext = os.path.splitext(result['thumbnail'])[1]
                print(f"- Thumbnail format: {thumbnail_ext}")
        else:
            raise Exception("Download failed to produce expected results")
            
    except yt_dlp.utils.ExtractorError as e:
        print("⚠️  Error downloading video:")
        print(e)
        
    except Exception as e:
        print(f"❌ Unexpected error occurred during download: {str(e)}")
    
if __name__ == "__main__":
    # Create output directory if it doesn't exist
    os.makedirs("./downloads", exist_ok=True)
    
    main()