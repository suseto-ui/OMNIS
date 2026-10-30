package com.example.domain.usecase

import com.example.data.OmnisGoal
import com.example.data.OmnisTask
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Use Case pro orchestraci cílů, konverzi návrhů 8D kompenzace na aktivní cíle
 * a sledování plnění dílčích kroků.
 */
class GoalOrchestrationUseCase {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * Převede schválený CompensatoryGoalDraft na nový aktivní OmnisGoal se sub-tasky.
     */
    fun createGoalFromCompensatoryDraft(draft: CompensatoryGoalDraft): OmnisGoal {
        val tasks = draft.recommendedTasks.mapIndexed { index, taskDesc ->
            OmnisTask(
                id = UUID.randomUUID().toString().take(8),
                title = taskDesc,
                description = "Automaticky vygenerovaný krok pro 8D kompenzaci",
                status = if (index == 0) "IN_PROGRESS" else "PENDING",
                assignedAgentId = "OMNIS_AUTOPOIESIS_AGENT",
                domain = draft.domainKey
            )
        }

        val tasksJson = try {
            jsonParser.encodeToString(tasks)
        } catch (e: Exception) {
            "[]"
        }

        return OmnisGoal(
            title = draft.title,
            description = draft.description,
            status = "ACTIVE",
            priority = if (draft.frictionScore > 0.6f) 5 else 4,
            progress = 0.05f,
            tasksJson = tasksJson,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Spočítá aktuální progres cíle na základě stavu jednotlivých dílčích úkolů.
     */
    fun calculateProgress(tasks: List<OmnisTask>): Float {
        if (tasks.isEmpty()) return 0f
        val doneCount = tasks.count { it.status.equals("DONE", ignoreCase = true) }
        val inProgressCount = tasks.count { it.status.equals("IN_PROGRESS", ignoreCase = true) }
        return ((doneCount * 1.0f) + (inProgressCount * 0.5f)) / tasks.size
    }
}
