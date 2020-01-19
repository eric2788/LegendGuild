package com.ericlam.mc.legendguild.config

import com.ericlam.mc.kotlib.config.Resource
import com.ericlam.mc.kotlib.config.dto.ConfigFile
import com.ericlam.mc.legendguild.guild.GuildPlayer

@Resource(locate = "config.yml")
data class Config(
        val maxLevel: Int,
        val maxExp: String,
        val maxChar: Int,
        val skillUpdate: String,
        val dailyContribution: Contribution,
        val postResources: PostResources,
        val salaries: MutableMap<GuildPlayer.Role, Double>,
        val default_salaries: MutableMap<GuildPlayer.Role, Double>
) : ConfigFile(){

    data class PostResources(
            val money: Double,
            val money_contribute: Int,
            val items: Map<String, Int>
    )

    data class Contribution(
            val money: Money,
            val points: Points
    ){
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