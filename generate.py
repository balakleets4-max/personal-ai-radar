from pathlib import Path
root = Path('/mnt/data/personal_ai_radar_v014/src/main/kotlin/com/personalradar/app')

def write(rel, content):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content.strip()+"\n", encoding='utf-8')

# Core time
write('core/time/TimeProvider.kt', r'''
package com.personalradar.app.core.time

interface TimeProvider {
    fun nowMillis(): Long
    fun todayStartMillis(): Long
    fun todayEndMillis(): Long
    fun dateString(timestamp: Long): String
}
''')
write('core/time/SystemTimeProvider.kt', r'''
package com.personalradar.app.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SystemTimeProvider(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun todayStartMillis(): Long = LocalDate.now(zoneId)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()

    override fun todayEndMillis(): Long = LocalDate.now(zoneId)
        .plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli() - 1L

    override fun dateString(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
        .atZone(zoneId)
        .toLocalDate()
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
}
''')
# Transaction
write('core/transaction/TransactionRunner.kt', r'''
package com.personalradar.app.core.transaction

interface TransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}
''')
write('core/transaction/RoomTransactionRunner.kt', r'''
package com.personalradar.app.core.transaction

import androidx.room.withTransaction
import com.personalradar.app.core.database.AppDatabase

class RoomTransactionRunner(
    private val database: AppDatabase
) : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return database.withTransaction { block() }
    }
}
''')
# Versions
write('core/versions/AppVersions.kt', r'''
package com.personalradar.app.core.versions

object AppVersions {
    const val PARSER_VERSION = "parser-v0.1.4"
    const val ANALYZER_VERSION = "analyzer-v0.1.4"
    const val RADAR_ENGINE_VERSION = "radar-v0.1.4"
}
''')
# Utils
write('core/utils/TextLimits.kt', r'''
package com.personalradar.app.core.utils

object TextLimits {
    const val MAX_CAPTURE_LENGTH = 10_000
    const val SOURCE_QUOTE_MAX_LENGTH = 160
    const val WHY_TEXT_MAX_SENTENCES = 3
}
''')
write('core/utils/TextSanitizer.kt', r'''
package com.personalradar.app.core.utils

fun String.toSourceQuote(): String {
    val clean = trim()
    return if (clean.length <= TextLimits.SOURCE_QUOTE_MAX_LENGTH) {
        clean
    } else {
        clean.take(TextLimits.SOURCE_QUOTE_MAX_LENGTH - 3).trimEnd() + "..."
    }
}

fun String.normalizeKeyPart(): String {
    return trim()
        .lowercase()
        .replace('ё', 'е')
        .replace(Regex("\\s+"), " ")
        .trim('.', ',', ';', ':', '!', '?', '"', '\'', '«', '»')
        .ifBlank { "_" }
}

fun buildDedupeKey(
    cardType: String,
    action: String?,
    person: String?,
    topic: String?
): String {
    return listOf(cardType, action, person, topic)
        .map { (it ?: "_").normalizeKeyPart() }
        .joinToString(separator = ":")
}
''')
# Constants objects
constants = {
'CaptureStatus.kt': {'CaptureStatus':['ACTIVE','ARCHIVED','DELETED']},
'CaptureSource.kt': {'CaptureSource':['TEXT','VOICE','IMPORT','SYSTEM']},
'LanguageCode.kt': {'LanguageCode':['RU','EN','MIXED','UNKNOWN']},
'IntentType.kt': {'IntentType':['TASK','REMINDER','IDEA','RISK','PROJECT_NOTE','MEMORY_NOTE','UNKNOWN']},
'ParsedEntityType.kt': {'ParsedEntityType':['DATE','TIME','ACTION','TASK_SIGNAL','REMINDER_SIGNAL','PERSON','TOPIC','RISK_SIGNAL','PROJECT_SIGNAL','UNCERTAIN_DATE','IDEA_SIGNAL']},
'RadarCardType.kt': {'RadarCardType':['TASK_EXPLICIT','TASK_HIDDEN','REMINDER','RISK','PROJECT_SIGNAL','FORGOTTEN_THOUGHT','REPEATING_TOPIC']},
'RadarCardStatus.kt': {'RadarCardStatus':['ACTIVE','DONE','HIDDEN','SNOOZED','ARCHIVED']},
'ActionType.kt': {'ActionType':['MARK_DONE','HIDE','SNOOZE','CREATE_REMINDER','EDIT_DUE_DATE','MAKE_TASK','LINK_TOPIC','CONFIRM_USEFUL','MARK_NOT_USEFUL','ARCHIVE']},
'ReminderStatus.kt': {'ReminderStatus':['SCHEDULED','FIRED','MISSED','CANCELLED']},
'ReminderDeliveryMode.kt': {'ReminderDeliveryMode':['EXACT','APPROXIMATE','IN_APP_ONLY']},
'ReminderSchedulerState.kt': {'ReminderSchedulerState':['NOT_REQUIRED','PENDING','SCHEDULED','FAILED','UNKNOWN']},
'CapabilityState.kt': {'CapabilityState':['AVAILABLE','UNAVAILABLE','DENIED','UNKNOWN']},
'CapabilityCategory.kt': {'CapabilityCategory':['ANDROID_PERMISSION','ANDROID_CAPABILITY','APP_POLICY','SYSTEM_STATE']},
'MemoryFactType.kt': {'MemoryFactType':['PARSER_SIGNAL','TOPIC_PATTERN','CARD_PATTERN','USER_PREFERENCE','SUPPRESSION']},
'PendingSystemActionType.kt': {'PendingSystemActionType':['SCHEDULE_REMINDER','CANCEL_REMINDER','RESCHEDULE_REMINDER']},
'PendingSystemActionStatus.kt': {'PendingSystemActionStatus':['PENDING','IN_PROGRESS','DONE','FAILED','CANCELLED']},
'AppEventLevel.kt': {'AppEventLevel':['INFO','WARNING','ERROR']},
'AppEventCategory.kt': {'AppEventCategory':['PARSER','ANALYSIS','RADAR','REMINDER','DEVICE_CAPABILITY','DATABASE','SYSTEM']},
'TopicType.kt': {'TopicType':['PERSON','PROJECT','WORK','PLACE','OBJECT','UNKNOWN']},
'DeviceCapabilityName.kt': {'DeviceCapabilityName':['NOTIFICATIONS','EXACT_ALARM','INTERNET','BATTERY_OPTIMIZATION','BACKGROUND_WORK','DATA_STORED_LOCALLY','CLOUD_DISABLED','MICROPHONE_NOT_USED']},
'UserSettingKey.kt': {'UserSettingKey':['PRIVACY_LOCAL_ONLY','CLOUD_ENABLED','MICROPHONE_ENABLED','RADAR_MAX_CARDS_IMPORTANT','RADAR_MAX_CARDS_TODAY','RADAR_MAX_CARDS_HIDDEN_TASKS','RADAR_MAX_CARDS_PROJECTS','RADAR_MAX_CARDS_REPEATING','PARSER_MIN_CONFIDENCE_FOR_CARD','RADAR_DEFAULT_SNOOZE_DAYS','EVENT_LOG_RETENTION_DAYS_INFO','EVENT_LOG_RETENTION_DAYS_WARNING','EVENT_LOG_RETENTION_DAYS_ERROR']}
}
for fname, objs in constants.items():
    for obj, vals in objs.items():
        body = [f'package com.personalradar.app.core.model\n\nobject {obj} {{']
        for v in vals:
            body.append(f'    const val {v} = "{v}"')
        # Add keys actual lowercase for settings? hmm constants are uppercase values. We need real keys lowercase. override UserSettingKey content separately.
        body.append('}')
        write(f'core/model/{fname}', '\n'.join(body))
# Override UserSettingKey with lowercase keys
write('core/model/UserSettingKey.kt', r'''
package com.personalradar.app.core.model

object UserSettingKey {
    const val PRIVACY_LOCAL_ONLY = "privacy_local_only"
    const val CLOUD_ENABLED = "cloud_enabled"
    const val MICROPHONE_ENABLED = "microphone_enabled"

    const val RADAR_MAX_CARDS_IMPORTANT = "radar_max_cards_important"
    const val RADAR_MAX_CARDS_TODAY = "radar_max_cards_today"
    const val RADAR_MAX_CARDS_HIDDEN_TASKS = "radar_max_cards_hidden_tasks"
    const val RADAR_MAX_CARDS_PROJECTS = "radar_max_cards_projects"
    const val RADAR_MAX_CARDS_REPEATING = "radar_max_cards_repeating"

    const val PARSER_MIN_CONFIDENCE_FOR_CARD = "parser_min_confidence_for_card"
    const val RADAR_DEFAULT_SNOOZE_DAYS = "radar_default_snooze_days"

    const val EVENT_LOG_RETENTION_DAYS_INFO = "event_log_retention_days_info"
    const val EVENT_LOG_RETENTION_DAYS_WARNING = "event_log_retention_days_warning"
    const val EVENT_LOG_RETENTION_DAYS_ERROR = "event_log_retention_days_error"

    val ALL = setOf(
        PRIVACY_LOCAL_ONLY,
        CLOUD_ENABLED,
        MICROPHONE_ENABLED,
        RADAR_MAX_CARDS_IMPORTANT,
        RADAR_MAX_CARDS_TODAY,
        RADAR_MAX_CARDS_HIDDEN_TASKS,
        RADAR_MAX_CARDS_PROJECTS,
        RADAR_MAX_CARDS_REPEATING,
        PARSER_MIN_CONFIDENCE_FOR_CARD,
        RADAR_DEFAULT_SNOOZE_DAYS,
        EVENT_LOG_RETENTION_DAYS_INFO,
        EVENT_LOG_RETENTION_DAYS_WARNING,
        EVENT_LOG_RETENTION_DAYS_ERROR
    )
}
''')
