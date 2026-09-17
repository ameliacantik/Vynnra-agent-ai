package lol.vynnra.agent.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "memories",
    indices = [
        Index(value = ["kind"]),
        Index(value = ["updatedAt"]),
        Index(value = ["enabled", "pinned"])
    ]
)
data class MemoryEntity(
    @androidx.room.PrimaryKey val id: String,
    val kind: String,
    val key: String?,
    val content: String,
    val sourceSessionId: String?,
    val importance: Float,
    val sensitive: Boolean,
    val enabled: Boolean,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAccessedAt: Long?
)

@Entity(
    tableName = "agent_tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["updatedAt"])
    ]
)
data class AgentTaskEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val goal: String,
    val status: String,
    val currentStep: Int,
    val totalSteps: Int,
    val checkpointJson: String?,
    val lastError: String?,
    val requiresUserAction: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?
)

@Entity(
    tableName = "agent_task_steps",
    primaryKeys = ["taskId", "stepIndex"],
    foreignKeys = [
        ForeignKey(
            entity = AgentTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId", "status"])]
)
data class AgentTaskStepEntity(
    val taskId: String,
    val stepIndex: Int,
    val title: String,
    val status: String,
    val toolId: String?,
    val inputJson: String?,
    val outputSummary: String?,
    val attempts: Int,
    val startedAt: Long?,
    val completedAt: Long?,
    val errorMessage: String?
)
