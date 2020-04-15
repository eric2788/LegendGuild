package com.ericlam.mc.legendguild.config

import com.ericlam.mc.kotlib.config.Resource
import com.ericlam.mc.kotlib.config.dto.ConfigFile
import com.ericlam.mc.legendguild.GuildSkill
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.dao.QuestType
import org.bukkit.Material

@Resource(locate = "config.yml")
data class Config(
        val maxLevel: Int,
        val maxExp: String,
        val maxChar: Int,
        val skillUpdate: String,
        val dailyContribution: Contribution,
        val postResources: PostResources,
        val salaries: Map<GuildPlayer.Role, Double>,
        val default_salaries: Map<GuildPlayer.Role, Double>,
        val materials: Materials,
        val leaderUpdate: Long,
        val skills: Map<GuildSkill, UpgradeRequirement>,
        val questItems: Map<QuestType, Material>,
        val lossExp: String
) : ConfigFile() {

    data class UpgradeRequirement(
            val money: Double,
            val items: Map<String, Int>
    )

    data class PostResources(
            val money: Double,
            val money_contribute: Int,
            val items: Map<String, Int>
    )

    data class Materials(
            val head: Material,
            val glassPane: Material,
            val leash: Material
    )

    data class Contribution(
            val money: Money,
            val points: Points
    ) {
        data class Money(
                val need: Double,
                val exp: Double,
                val contribute: Int
        )

        data class Points(
                val need: Double,
                val exp: Double,
                val contribute: Int
        )
    }

}