package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.SkillDao
import com.example.data.dao.TaskDao
import com.example.data.dao.TaskLogDao
import com.example.data.model.AppSettingsEntity
import com.example.data.model.SkillEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SkillEntity::class,
        TaskEntity::class,
        TaskLogEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun skillDao(): SkillDao
    abstract fun taskDao(): TaskDao
    abstract fun taskLogDao(): TaskLogDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "skill_curve_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        // Only initialize default settings, do not seed demo tasks/skills
                        database.appSettingsDao().insertOrUpdate(
                            AppSettingsEntity(
                                id = 1,
                                decayEnabled = true,
                                defaultFormula = com.example.data.model.DecayFormula.EXPONENTIAL,
                                defaultDecayRate = 0.025,
                                defaultMinEfficiency = 0.10,
                                language = "FA",
                                hapticFeedback = true
                            )
                        )
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val settingsDao = database.appSettingsDao()
            val skillDao = database.skillDao()
            val taskDao = database.taskDao()

            // Default App Settings
            settingsDao.insertOrUpdate(
                AppSettingsEntity(
                    id = 1,
                    decayEnabled = true,
                    defaultFormula = com.example.data.model.DecayFormula.EXPONENTIAL,
                    defaultDecayRate = 0.025,
                    defaultMinEfficiency = 0.10,
                    language = "FA",
                    hapticFeedback = true
                )
            )

            // Seed initial skills
            val s1 = SkillEntity(
                name = "برنامه‌نویسی و کاتلین",
                category = "تخصصی و کدنویسی",
                colorHex = "#6366F1",
                iconName = "code",
                currentMastery = 12.45,
                targetMastery = 100.0
            )
            val s2 = SkillEntity(
                name = "مکالمه زبان انگلیسی",
                category = "زبان‌های خارجی",
                colorHex = "#10B981",
                iconName = "translate",
                currentMastery = 28.30,
                targetMastery = 100.0
            )
            val s3 = SkillEntity(
                name = "نوازندگی پیانو",
                category = "هنر و موسیقی",
                colorHex = "#F59E0B",
                iconName = "music_note",
                currentMastery = 6.80,
                targetMastery = 100.0
            )

            val s1Id = skillDao.insertSkill(s1)
            val s2Id = skillDao.insertSkill(s2)
            val s3Id = skillDao.insertSkill(s3)

            // Seed initial tasks with base gain e.g. 0.01% or 0.05%
            taskDao.insertTask(
                TaskEntity(
                    skillId = s1Id,
                    title = "حل یک مسئله الگوریتمی لیت‌کد",
                    description = "تمرین تمرکز و منطق مسئله‌گشایی",
                    baseGainPercent = 0.05,
                    recurrenceType = com.example.data.model.RecurrenceType.DAILY,
                    completionCount = 14,
                    useCustomDecay = false
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    skillId = s1Id,
                    title = "مطالعه معماری تمیز و کامپوز",
                    description = "خواندن یک بخش از مستندات یا کتاب",
                    baseGainPercent = 0.02,
                    recurrenceType = com.example.data.model.RecurrenceType.DAILY,
                    completionCount = 6,
                    useCustomDecay = false
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    skillId = s2Id,
                    title = "۲۰ دقیقه مکالمه یا سایه‌خوانی (Shadowing)",
                    description = "تمرین تلفظ و روانی لهجه",
                    baseGainPercent = 0.03,
                    recurrenceType = com.example.data.model.RecurrenceType.DAILY,
                    completionCount = 22,
                    useCustomDecay = true,
                    customDecayFormula = com.example.data.model.DecayFormula.EXPONENTIAL,
                    customDecayRate = 0.02,
                    customMinEfficiency = 0.15
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    skillId = s2Id,
                    title = "مرور فلش‌کارت‌های ۵۰۴ واژه",
                    description = "مرور جعبه لایتنر هفتگی",
                    baseGainPercent = 0.01,
                    recurrenceType = com.example.data.model.RecurrenceType.WEEKLY,
                    completionCount = 8,
                    useCustomDecay = false
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    skillId = s3Id,
                    title = "تمرین گام‌ها و آرپژ‌های پیانو",
                    description = "۱۵ دقیقه گرم کردن انگشتان",
                    baseGainPercent = 0.01,
                    recurrenceType = com.example.data.model.RecurrenceType.DAILY,
                    completionCount = 10,
                    useCustomDecay = false
                )
            )
        }
    }
}
