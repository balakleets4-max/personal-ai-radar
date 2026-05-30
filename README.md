# Личный ИИ-Радар — Android vertical slice v0.1.5

Этот репозиторий теперь содержит собираемый Android-проект `com.personalradar.app` и первый рабочий вертикальный сценарий:

- ввод Capture / Захвата памяти;
- сохранение Capture в локальную Room-базу;
- базовый rule-based анализ текста;
- создание RadarCard;
- отображение активных Radar-карточек на экране;
- объяснение причины появления карточки через `whyText`.

## Текущий архитектурный путь

```text
MainActivity
→ AppContainer
→ CaptureRadarController
→ QuickCaptureRepository
→ Room DAO / Entity
```

## Ключевые файлы

- `src/main/kotlin/com/personalradar/app/MainActivity.kt`
- `src/main/kotlin/com/personalradar/app/di/AppContainer.kt`
- `src/main/kotlin/com/personalradar/app/quick/CaptureRadarController.kt`
- `src/main/kotlin/com/personalradar/app/quick/QuickCaptureRepository.kt`
- `src/main/kotlin/com/personalradar/app/core/database/AppDatabase.kt`
- `.github/workflows/android-build.yml`

## CI

GitHub Actions выполняет:

```bash
gradle -Pandroid.useAndroidX=true -Pandroid.nonTransitiveRClass=true assembleDebug --stacktrace
```

## Проверки, которые уже прошли

- Gradle запускается в GitHub Actions.
- AndroidX включён через `gradle.properties`.
- Kotlin-код компилируется.
- Room Entity/DAO проходят текущую сборку.
- Первый вертикальный сценарий собирается зелёным run.

## Следующий шаг

`Code Step 5`:

- добавить выгрузку debug APK как GitHub Actions artifact;
- скачать APK на Android;
- проверить запуск приложения на телефоне;
- вручную протестировать Capture → RadarCard.

## Build trigger

Последний ручной trigger сборки: 2026-05-30 03:33 local workflow check.
