package com.ericlam.mc.legendguild.dao

import com.ericlam.mc.kotlib.config.dao.DataFile
import com.ericlam.mc.kotlib.config.dao.DataResource
import com.ericlam.mc.kotlib.config.dao.PrimaryKey
import com.ericlam.mc.legendguild.GuildSkill
import com.ericlam.mc.legendguild.JavaScript
import com.ericlam.mc.legendguild.LegendGuild
import java.util.*

@DataResource(folder = "GuildData")
data class Guild(
        @PrimaryKey val name: String,
        private var level: Int = 0,
        private var exp: Double = 0.0,
        private val skills: MutableMap<GuildSkill, Int> = GuildSkill.values().map { it to 0 }.toMap().toMutableMap(),
        var public: Boolean = true,
        val salaries: MutableMap<GuildPlayer.Role, Double> = LegendGuild.config.default_salaries.toMutableMap(),
        val wannaJoins: MutableSet<UUID> = mutableSetOf(),
        val invites: MutableSet<UUID> = mutableSetOf(),
        val resource: Resource = Resource()
) : DataFile, Comparable<Guild> {

    val members: List<GuildPlayer>
        get() = LegendGuild.guildPlayerController.findAll().filter { it.guild == name }

    val memberMax: Int
        get() = 50 + level * 5

    val maxExp: Int
        get() {
            return JavaScript.eval(LegendGuild.config.maxExp.trim().replace("%level%", level.toString())) as Int
        }

    val currentLevel: Int
        get() = level

    val currentExp: Double
        get() = exp

    val currentSkills: Map<GuildSkill, Int>
        get() = skills

    infix fun exp(exp: Double) {
        this.exp += exp
        while (this.exp >= maxExp) {
            this.exp -= maxExp
            level++
        }
        while (this.exp < 0) {
            level--
            this.exp += maxExp
        }
        if (this.level < 0) {
            this.level = 0
            this.exp = 0.0
        }
    }

    infix fun level(level: Int) {
        this.level += level
        if (this.level < 0) this.level = 0
        this.exp = 0.0
    }

    fun percentage(skill: GuildSkill): Double {
        val level = skills[skill] ?: 0
        return (JavaScript.eval(LegendGuild.config.skillValue.trim().replace("%level%", level.toString())) as Number).toDouble() / 100
    }

    fun setSkillLevel(skill: GuildSkill, level: Int) {
        skills[skill]?.let { it + level }.also { skills[skill] = (it ?: level) }
    }

    fun isMember(uuid: UUID): Boolean {
        return LegendGuild.guildPlayerController.findById(uuid)?.guild == name
    }

    operator fun get(uuid: UUID): GuildPlayer? {
        return members.find { it.uuid == uuid }
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? Guild)?.let { it.name == this.name }
                ?: false
    }

    override fun compareTo(other: Guild): Int {
        return this.level.compareTo(other.level)
    }

    data class Resource(val items: MutableMap<String, Int> = mutableMapOf(), var money: Double = 0.0)
}