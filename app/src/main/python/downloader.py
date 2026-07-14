import yt_dlp

def downloadSong(url, output_path):
    ydl_opts = {
        "outtmpl": f"{output_path}/%(title)s.%(ext)s",
        "format": "bestaudio/best",
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        filename = ydl.prepare_filename(info)

    return filename