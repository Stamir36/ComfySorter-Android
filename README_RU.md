# ComfySorter — Android-клиент для ComfyFileSorter

> 📱 Мобильный Android-клиент для [ComfyFileSorter](https://github.com/Stamir36/ComfyFileSorter) — мощного локального менеджера галереи для генераций ComfyUI.
>
> 📖 **[English README](README.md)** — read in English.

---

Android-приложение для просмотра и управления файлами с вашего ComfyUI сервера. Сканируйте QR-коды, просматривайте изображения и видео, изучайте метаданные генерации и скачивайте файлы — всё с вашего телефона.

## Требования

Это приложение является **клиентом** и требует запущенного [ComfyFileSorter](https://github.com/Stamir36/ComfyFileSorter) на вашем ПК или сервере.

## Скриншоты

| Галерея и навигация | Настройки отображения | Просмотр и метаданные |
| :---: | :---: | :---: |
| ![Gallery Interface](https://raw.githubusercontent.com/Stamir36/ComfySorter-Android/refs/heads/master/gradle/Home.png) | ![Filters](https://raw.githubusercontent.com/Stamir36/ComfySorter-Android/refs/heads/master/gradle/Filter.png) | ![Image Viewer](https://raw.githubusercontent.com/Stamir36/ComfySorter-Android/refs/heads/master/gradle/Image.png) |
| Удобный просмотр папок и генераций | Гибкая настройка фильтров и сортировки | Полноэкранный просмотр со всеми параметрами |


**Установка ComfyFileSorter:**

```bash
# Клонируйте репозиторий
git clone [https://github.com/Stamir36/ComfyFileSorter.git](https://github.com/Stamir36/ComfyFileSorter.git)
cd ComfyFileSorter

# Установите зависимости
pip install -r requirements.txt

# Запустите сервер
python app.py
```

Сервер запустится по адресу `http://127.0.0.1:7865`. Для удалённого доступа с телефона используйте встроенную поддержку Ngrok или localhost.run — ComfyFileSorter сгенерирует QR-код, который можно отсканировать прямо из этого приложения.

## Возможности

- **Управление серверами** — добавляйте и храните несколько подключений к ComfyUI серверам
- **QR-сканер** — быстрое подключение сервера через сканирование QR-кода (ML Kit)
- **Галерея** — просмотр папок и файлов с поиском, сортировкой и фильтрацией
- **Просмотр изображений** — полноэкранный режим с зумом (щипок и двойное нажатие)
- **Видеоплеер** — встроенный плеер на базе ExoPlayer с элементами управления
- **Метаданные генерации** — просмотр промптов, параметров и весов LoRA
- **Скачивание и шаринг** — сохранение файлов на устройство или отправка другим приложениям
- **Копирование промпта** — копирование позитивного/негативного промпта в буфер обмена одним нажатием

## Технологии

| Компонент | Технология |
|---|---|
| Язык | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Навигация | Navigation Compose |
| QR-сканирование | CameraX + ML Kit Barcode Scanning |
| Изображения | Coil (кэширование в памяти и на диске) |
| Сеть | Retrofit + Gson |
| Видео | ExoPlayer (Media3) |
| Разрешения | Accompanist Permissions |

## Минимальные требования

- Android 9.0 (API 28) и выше
- Камера (опционально, для сканирования QR-кодов)

## Сборка

```bash
# Клонируйте репозиторий
git clone [https://github.com/Stamir36/ComfySorter-Android.git](https://github.com/Stamir36/ComfySorter-Android.git)
cd ComfySorter-Android

# Откройте в Android Studio или соберите из командной строки
./gradlew assembleDebug
```

## Структура проекта

```text
app/src/main/java/com/unesell/comfysorter/
├── MainActivity.kt          # Точка входа и навигация
├── ServerListScreen.kt      # Экран списка серверов
├── ScannerScreen.kt         # QR-сканер с камерой
├── GalleryScreen.kt         # Галерея файлов и папок
├── ViewerScreen.kt          # Полноэкранный просмотр файлов
├── ServerRepository.kt      # Локальное хранилище серверов
├── network/
│   └── ApiService.kt        # API-клиент для ComfyFileSorter
└── ui/theme/                # Тема и стили приложения
```

## Использование

1. **Запустите ComfyFileSorter** на вашем ПК/сервере — см. [инструкцию по установке](https://github.com/Stamir36/ComfyFileSorter#-как-установить-и-запустить).
2. **Откройте доступ для телефона** — используйте Ngrok или localhost.run (встроено в ComfyFileSorter).
3. **Откройте приложение** → нажмите «Новое подключение» → отсканируйте QR-код, показанный ComfyFileSorter.
4. **Просматривайте галерею** — ищите, сортируйте и фильтруйте ваши генерации.
5. **Нажмите на файл** для полноэкранного просмотра — приближайте, смотрите метаданные, скачивайте или делитесь.

## Лицензия

MIT

## Ссылки

- 🖥️ **[ComfyFileSorter (Сервер)](https://github.com/Stamir36/ComfyFileSorter)** — серверная часть
- 📖 **[English README](README.md)** — описание на английском языке
