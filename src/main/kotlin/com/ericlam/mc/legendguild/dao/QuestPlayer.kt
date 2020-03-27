package com.ericlam.mc.legendguild.dao

import com.ericlam.mc.kotlib.config.dao.DataFile
import com.ericlam.mc.kotlib.config.dao.DataResource
import com.ericlam.mc.kotlib.config.dao.ForeignKey
import com.ericlam.mc.kotlib.config.dao.PrimaryKey
import com.ericlam.mc.legendguild.LegendGuild
import org.bukkit.entity.Entity
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@DataResource(folder = "QuestPlayers")
data class QuestPlayer(
        @PrimaryKey
        @ForeignKey(GuildPlayer::class) val user: UUID,
        var item: QuestItem? = null,
        var request: RequestItem? = null,
        var job: JobItem? = null
) : DataFile {

    enum class QuestResult {
        DEADLINED,
        NOT_STARTED_ANY,
        FAILED,
        SUCCESS_AND_REWARDED
    }

    data class RequestItem(
            val goal: List<String>,
            val contribute: Int,
            val taken: UUID?
    )

    data class JobItem(
            val request: RequestItem,
            val owner: UUID
    )

    data class QuestItem(
            val questType: QuestType,
            val start: Long = System.currentTimeMillis(),
            val killed: MutableList<Entity> = mutableListOf()
    ) {

        val matchGoal: Boolean
            get() = questType.goal(killed)

        val deadlined: Boolean
            get() = Duration.between(Timestamp(start).toLocalDateTime(), LocalDateTime.now()).toDays() > 1

        val progress: Pair<Int, Int>
            get() = questType.progress(killed)

    }

    fun tryFinish(): QuestResult {
        return when {
            item == null -> QuestResult.NOT_STARTED_ANY
            item!!.deadlined -> QuestResult.DEADLINED
            item!!.matchGoal -> QuestResult.SUCCESS_AND_REWARDED.also {
                LegendGuild.guildPlayerController.update(user) {
                    contribution += item!!.questType.contribution
                    LegendGuild.guildController.update(guild) {
                        this exp item!!.questType.exp
                    }
                }
            }
            else -> QuestResult.FAILED
        }
    }


}