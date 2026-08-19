import yt_dlp
import time

import os
import time
import yt_dlp

def downloadSong(url, output_path, retries=3, delay=3):
    ydl_opts = {
        "outtmpl": f"{output_path}/%(title)s-%(artist)s-%(release_date)s.%(ext)s",
        "format": "bestaudio/best",
        "writethumbnail": True,
        # "quiet": True,
        "no_warnings": True,
    }

    print("\n YT_DLP VERSION ID: " + str(yt_dlp.version) + "\n")

    last_error = None

    for attempt in range(retries):
        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=True)
                audio_path = ydl.prepare_filename(info)
                base = os.path.splitext(audio_path)[0]

                print(f"Information extracted from the video: {list(info.keys())}\n--------------\n")
                
                thumbnail_path = None
                for ext in (".webp", ".jpg", ".png"):
                    path = base + ext
                    if os.path.exists(path):
                        thumbnail_path = path
                        break

                artist = (
                        info.get("artist")
                        or str(info.get("artists"))
                        or info.get("channel")
                        or "Unknown Artist"
                )

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
    # url = "https://youtu.be/polycpBREYA?si=3MFHgdibK3wcQHVq"  # Example video URL
    url = "https://youtu.be/vNBNIce9XDM?si=ZiCf0Yc8OtkuggPL"
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