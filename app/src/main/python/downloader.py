import yt_dlp
import time

import os
import time
import yt_dlp

def downloadSong(url, output_path, retries=3, delay=3):
    ydl_opts = {
        "outtmpl": f"{output_path}/%(title)s.%(ext)s",
        "format": "bestaudio/best",
        "writethumbnail": True,
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

                artist = (
                        info.get("artist")
                        or info.get("album_artist")
                        or info.get("uploader")
                        or "Unknown Artist"
                )

                return {
                    "audio": audio_path,
                    "artist": artist,
                    "thumbnail": thumbnail_path,
                    "duration": info.get("duration")
                }

        except Exception as e:
            last_error = e
            if attempt < retries - 1:
                time.sleep(delay)

    raise last_error

def get_playlist_urls(playlist_url):
    ydl_opts = {"extract_flat": True, "quiet": True}
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(playlist_url, download=False)
        urls = [entry["url"] for entry in info["entries"] if entry]
        playlist_name = info.get("title", "Unknown Playlist")
        return {"name": playlist_name, "urls": urls}