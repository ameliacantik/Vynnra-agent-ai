package lol.vynnra.agent.data.task

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import lol.vynnra.agent.core.task.TaskRecord
import lol.vynnra.agent.core.task.TaskStatus
import lol.vynnra.agent.core.task.TaskStepRecord
import lol.vynnra.agent.core.task.TaskStepStatus
import lol.vynnra.agent.data.local.VynnraDatabase
import java.io.File

@RunWith(AndroidJUnit4::class)
class Phase9RestartInstrumentedTest {
    private lateinit var databaseFile: File
    private var database: VynnraDatabase? = null

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseFile = File(context.noBackupFilesDir, "phase9-restart-${System.currentTimeMillis()}.db")
    }

    @After
    fun tearDown() {
        database?.close()
        database = null
        databaseFile.delete()
        File(databaseFile.path + "-shm").delete()
        File(databaseFile.path + "-wal").delete()
    }

    @Test
    fun running_task_survives_database_reopen() {
        val taskId = "restart-task"
        database = openDatabase()
        val firstRepository = RoomTaskRepository(database!!.agentTaskDao())
        firstRepository.runBlockingCreate(
            TaskRecord(
                id = taskId,
                title = "Restart test",
                goal = "Persist this task",
                status = TaskStatus.RUNNING,
                currentStep = 1,
                totalSteps = 3,
                checkpointJson = "{\"actionIndex\":1}",
                lastError = null,
                requiresUserAction = false,
                createdAt = 1L,
                updatedAt = 2L,
                completedAt = null
            )
        )
        firstRepository.runBlockingCheckpoint(
            firstRepositoryTask = TaskRecord(
                id = taskId,
                title = "Restart test",
                goal = "Persist this task",
                status = TaskStatus.RUNNING,
                currentStep = 1,
                totalSteps = 3,
                checkpointJson = "{\"actionIndex\":1}",
                lastError = null,
                requiresUserAction = false,
                createdAt = 1L,
                updatedAt = 2L,
                completedAt = null
            ),
            step = TaskStepRecord(
                taskId = taskId,
                stepIndex = 1,
                title = "Persistent step",
                status = TaskStepStatus.VERIFIED,
                toolId = "test.tool",
                inputJson = "{value:ok}",
                outputSummary = "verified",
                attempts = 1,
                startedAt = 2L,
                completedAt = 3L,
                errorMessage = null
            )
        )
        database!!.close()
        database = null

        database = openDatabase()
        val secondRepository = RoomTaskRepository(database!!.agentTaskDao())
        val resumable = secondRepository.runBlockingResumableTasks()
        val steps = secondRepository.runBlockingSteps(taskId)

        assertEquals(1, resumable.size)
        assertEquals(taskId, resumable.single().id)
        assertEquals(1, resumable.single().currentStep)
        assertEquals("{\"actionIndex\":1}", resumable.single().checkpointJson)
        assertTrue(steps.any { it.stepIndex == 1 && it.status == TaskStepStatus.VERIFIED })
    }

    private fun openDatabase(): VynnraDatabase =
        Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VynnraDatabase::class.java,
            databaseFile.absolutePath
        ).addMigrations(lol.vynnra.agent.data.local.VynnraDatabaseMigrations.FROM_1_TO_2).build()
}

private fun RoomTaskRepository.runBlockingCreate(task: TaskRecord) =
    kotlinx.coroutines.runBlocking { create(task) }

private fun RoomTaskRepository.runBlockingCheckpoint(task: TaskRecord, step: TaskStepRecord) =
    kotlinx.coroutines.runBlocking { checkpoint(task, step) }

private fun RoomTaskRepository.runBlockingResumableTasks() =
    kotlinx.coroutines.runBlocking { resumableTasks() }

private fun RoomTaskRepository.runBlockingSteps(taskId: String) =
    kotlinx.coroutines.runBlocking { getSteps(taskId) }
