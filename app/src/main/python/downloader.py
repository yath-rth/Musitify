import yt_dlp
import time

def downloadSong(url, output_path, retries=3, delay=3):
    ydl_opts = {
        "outtmpl": f"{output_path}/%(title)s.%(ext)s",
        "format": "bestaudio/best",
    }

    last_error = None
    for attempt in range(retries):
        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=True)
                return ydl.prepare_filename(info)
        except Exception as e:
            last_error = e
            if attempt < retries - 1:
                time.sleep(delay)

    raise last_error

def get_playlist_urls(playlist_url):
    ydl_opts = {"extract_flat": True, "quiet": True}
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(playlist_url, download=False)
        return [entry["url"] for entry in info["entries"] if entry]