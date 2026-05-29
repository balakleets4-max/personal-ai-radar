# Личный ИИ-Радар — Code Step 1 + базовый доменный каркас v0.1.4

Этот пакет содержит первый кодовый слой проекта:

- core infrastructure;
- Room entities;
- DAO;
- AppDatabase;
- доменные draft-модели;
- интерфейсы движков;
- простые rule-based реализации;
- базовые Use Cases для вертикального среза.

## Важно

Это не полный Android-проект с Gradle/Manifest/UI. Это исходный Kotlin-пакет, который нужно перенести в Android-проект `com.personalradar.app`.

## Что входит

### Core

- `TimeProvider`
- `SystemTimeProvider`
- `TransactionRunner`
- `RoomTransactionRunner`
- `AppVersions`
- constants/status objects
- text/dedupe helpers

### Room schema v0.1.4

- `CaptureEntity`
- `AnalysisResultEntity`
- `ParsedEntityEntity`
- `RadarCardEntity`
- `ActionEntity`
- `ReminderEntity`
- `TopicEntity`
- `CaptureTopicCrossRef`
- `DeviceCapabilityEntity`
- `MemoryFactEntity`
- `ReflectionLogEntity`
- `UserSettingsEntity`
- `AppEventLogEntity`
- `PendingSystemActionEntity`

### DAO

- `CaptureDao`
- `AnalysisDao`
- `ParsedEntityDao`
- `RadarCardDao`
- `ActionDao`
- `ReminderDao`
- `TopicDao`
- `DeviceCapabilityDao`
- `MemoryFactDao`
- `ReflectionDao`
- `UserSettingsDao`
- `AppEventLogDao`
- `PendingSystemActionDao`

### Engines

- `LanguageDetector` + `BasicLanguageDetector`
- `ParserEngine` + `RuleBasedParserEngine`
- `AnalysisEngine` + `RuleBasedAnalysisEngine`
- `RadarEngine` + `RuleBasedRadarEngine`

### Use Cases

- `AddCaptureUseCase`
- `PerformCardActionUseCase`
- `CreateReminderUseCase`
- `ProcessPendingSystemActionsUseCase`
- `ReactivateSnoozedCardsUseCase`
- `RefreshDeviceCapabilitiesUseCase`
- `GenerateDailyReflectionUseCase`
- `CheckOverdueRemindersUseCase`
- `InitializeDefaultSettingsUseCase`
- `CleanOldAppEventLogsUseCase`

## Проверки, которые уже проведены

- Все `.kt` файлы имеют package declaration.
- Все 14 Entity-файлов присутствуют.
- Все 13 DAO-файлов присутствуют.
- В схеме есть `PendingSystemActionEntity`.
- В `ReminderEntity` есть `schedulerState`.
- В `RadarCardEntity` есть `duplicateHitCount` отдельно от `shownCount`.
- В `AnalysisResultEntity` есть `isLatest`, `parserVersion`, `analyzerVersion`.
- В `MemoryFactEntity` есть `expiresAt`.
- В `DeviceCapabilityEntity` есть `category`.
- `AppEventLogEntity` не должен хранить raw Capture; в коде логируется только ID и техническое сообщение.

## Что нужно проверить уже в Android Studio

1. Компиляцию Room-аннотаций.
2. SQL-запросы DAO через Room processor.
3. Наличие зависимостей:
   - Room runtime / ktx / compiler или ksp;
   - Kotlin coroutines;
   - lifecycle-viewmodel-compose позже;
   - navigation-compose позже.
4. Unit-тесты для `AddCaptureUseCase`, `RadarEngine`, `ParserEngine`, DAO.

## Следующий кодовый шаг

`Code Step 2`:

- подключить этот пакет к реальному Android-проекту;
- настроить Gradle dependencies;
- собрать проект;
- исправить ошибки Room-компилятора;
- написать первые DAO tests.


## Патч проверки v0.1.5

После повторной сверки с матчастью и структурной проверки добавлены:

- `RefreshDeviceCapabilitiesUseCase`;
- `GenerateDailyReflectionUseCase`;
- DAO count/query методы для Reflection;
- retry threshold для `PendingSystemActionDao`;
- безопасное экранирование title в `CreateReminderUseCase` payloadJson;
- `CALENDAR_NOT_USED` как APP_POLICY capability для v0.1.

Также уточнено: `ProcessPendingSystemActionsUseCase` не должен бесконечно повторять failed actions; лимит попыток по умолчанию — 3.
