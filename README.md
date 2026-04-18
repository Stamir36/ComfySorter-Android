# ComfySorter — Android Client for ComfyFileSorter

> 📱 Android mobile client for [ComfyFileSorter](https://github.com/Stamir36/ComfyFileSorter) — a powerful local gallery manager for ComfyUI generations.
>
> 📖 **[Описание на русском](README_RU.md)** — читать на русском языке.

---

Android application for browsing and managing files from your ComfyUI server. Scan QR codes, view images and videos, explore generation metadata, and download files — all from your phone.

## Prerequisites

This app is a **client** and requires [ComfyFileSorter](https://github.com/Stamir36/ComfyFileSorter) to be running on your PC or server.

## Screenshots

| Gallery Interface | Filter & Sort | Image Viewer & Metadata |
| :---: | :---: | :---: |
| ![Gallery Interface](https://raw.githubusercontent.com/Stamir36/ComfySorter-Android/refs/heads/master/gradle/Home.png) | ![Filters](https://raw.githubusercontent.com/Stamir36/ComfySorter-Android/refs/heads/master/gradle/Filter.png) | ![Image Viewer](https://raw.githubusercontent.com/Stamir36/ComfySorter-Android/refs/heads/master/gradle/Image.png) |
| Browse folders and your generated artworks | Flexible sorting and display settings | Fullscreen viewing with generation parameters |


**Setup ComfyFileSorter:**

```bash
# Clone the repository
git clone [https://github.com/Stamir36/ComfyFileSorter.git](https://github.com/Stamir36/ComfyFileSorter.git)
cd ComfyFileSorter

# Install dependencies
pip install -r requirements.txt

# Run the server
python app.py
```

The server will start at `http://127.0.0.1:7865`. For remote/mobile access, use the built-in Ngrok or localhost.run support — ComfyFileSorter will generate a QR code you can scan directly from this app.

## Features

- **Server Management** — add and store multiple ComfyUI server connections
- **QR Code Scanner** — quickly connect to a server by scanning the QR code (powered by ML Kit)
- **Gallery Browser** — browse folders and files with search, sorting, and filtering
- **Image Viewer** — fullscreen viewing with pinch-to-zoom and double-tap zoom
- **Video Player** — built-in ExoPlayer with playback controls
- **Generation Metadata** — view prompts, parameters, and LoRA weights
- **Download & Share** — save files to your device or share with other apps
- **Copy Prompt** — copy positive/negative prompts to clipboard with one tap

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| QR Scanning | CameraX + ML Kit Barcode Scanning |
| Images | Coil (with memory & disk caching) |
| Network | Retrofit + Gson |
| Video | ExoPlayer (Media3) |
| Permissions | Accompanist Permissions |

## Minimum Requirements

- Android 9.0 (API 28) and above
- Camera (optional, for QR code scanning)

## Build

```bash
# Clone the repository
git clone [https://github.com/Stamir36/ComfySorter-Android.git](https://github.com/Stamir36/ComfySorter-Android.git)
cd ComfySorter-Android

# Open in Android Studio or build from CLI
./gradlew assembleDebug
```

## Project Structure

```text
app/src/main/java/com/unesell/comfysorter/
├── MainActivity.kt          # Entry point & navigation
├── ServerListScreen.kt      # Server list screen
├── ScannerScreen.kt         # QR code scanner with camera
├── GalleryScreen.kt         # File & folder gallery browser
├── ViewerScreen.kt          # Fullscreen file viewer
├── ServerRepository.kt      # Local server storage
├── network/
│   └── ApiService.kt        # API client for ComfyFileSorter
└── ui/theme/                # App theme & styling
```

## Usage

1. **Start ComfyFileSorter** on your PC/server — see [installation instructions](https://github.com/Stamir36/ComfyFileSorter#-how-to-install-and-run).
2. **Expose the server** for mobile access (use Ngrok or localhost.run — built into ComfyFileSorter).
3. **Open this app** → tap "New Connection" → scan the QR code displayed by ComfyFileSorter.
4. **Browse your gallery** — search, sort, and filter your generations.
5. **Tap a file** to view it fullscreen — zoom in, view metadata, download, or share.

## License

MIT

## Links

- 🖥️ **[ComfyFileSorter (Server)](https://github.com/Stamir36/ComfyFileSorter)** — the backend server
- 📖 **[Русская версия README](README_RU.md)** — описание на русском языке
