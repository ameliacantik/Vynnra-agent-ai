package lol.vynnra.agent.data.task

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
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
import lol.vynnra.agent.data.local.VynnraDatabaseMigrations

@RunWith(AndroidJUnit4::class)
class Phase9RestartInstrumentedTest {
    private lateinit var databaseName: String
    private var database: VynnraDatabase? = null

    @Before
    fun setUp() {
        databaseName = "phase9-restart-${System.currentTimeMillis()}"
    }

    @After
    fun tearDown() {
        database?.close()
        database = null
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(databaseName)
    }

    @Test
    fun running_task_survives_database_reopen() = runBlocking {
        val taskId = "restart-task"
        database = openDatabase()
        val firstRepository = RoomTaskRepository(database!!.agentTaskDao())
        val task = TaskRecord(
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
        firstRepository.create(task)
        firstRepository.checkpoint(
            task,
            TaskStepRecord(
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
        val resumable = secondRepository.resumableTasks()
        val steps = secondRepository.getSteps(taskId)

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
            databaseName
        ).addMigrations(VynnraDatabaseMigrations.FROM_1_TO_2).build()
}
