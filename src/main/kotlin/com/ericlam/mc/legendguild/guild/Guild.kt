package com.ericlam.mc.legendguild.guild

import com.ericlam.mc.kotlib.config.dao.DataFile
import com.ericlam.mc.kotlib.config.dao.DataResource
import com.ericlam.mc.kotlib.config.dao.PrimaryKey
import com.ericlam.mc.legendguild.GuildSkill
import com.ericlam.mc.legendguild.JavaScript
import com.ericlam.mc.legendguild.LegendGuild
import org.bukkit.OfflinePlayer
import java.util.*

@DataResource(folder = "GuildData")
data class Guild(
        @PrimaryKey val name: String,
        private var level: Int = 0,
        private var exp: Double = 0.0,
        private val skills: MutableMap<GuildSkill, Int> = GuildSkill.values().map { it to 0 }.toMap().toMutableMap(),
        val resource: Resource
) : DataFile, Comparable<Guild> {

    val members: List<GuildPlayer>
        get() = LegendGuild.guildPlayerController.findAll().filter { it.guild == name }

    val memberMax: Int
        get() = 50 + level * 5

    val maxExp: Int
        get() {
            return JavaScript.eval(LegendGuild.config.maxExp.replace("%level%", level.toString())) as Int
        }

    val currentLevel: Int
        get() = level

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
    }

    infix fun level(level: Int) {
        this.level += level
        this.exp = 0.0
    }

    fun save() {
        LegendGuild.guildController.save { this }
    }

    fun percentage(skill: GuildSkill): Double {
        val level = skills[skill] ?: 0
        return JavaScript.eval(LegendGuild.config.skillUpdate.replace("%level%", level.toString())) as Double / 100
    }

    fun setSkillLevel(skill: GuildSkill, level: Int) {
        skills[skill]?.let { it + level }.also { skills[skill] = (it ?: level) }
    }

    enum class JoinResponse {
        FULL,
        ALREADY_IN_SAME_GUILD,
        ALREADY_IN_OTHER_GUILD,
        SUCCESS
    }

    infix fun join(player: OfflinePlayer): JoinResponse {
        with(LegendGuild.guildPlayerController) {
            val gplayer = findById(player.uniqueId)
            return when {
                gplayer?.guild == name -> JoinResponse.ALREADY_IN_SAME_GUILD
                gplayer != null -> JoinResponse.ALREADY_IN_OTHER_GUILD
                memberMax <= members.size -> JoinResponse.FULL
                else -> {
                    save { GuildPlayer(player.uniqueId, player.name, name) }
                    JoinResponse.SUCCESS
                }
            }

        }
    }

    fun isMember(uuid: UUID): Boolean {
        return LegendGuild.guildPlayerController.findById(uuid)?.guild == name
    }

    operator fun get(uuid: UUID): GuildPlayer? {
        return members.find { it.uuid == uuid }
    }

    override fun hashCode(): Int {
        return name.hashCode() + members.size
    }

    override fun equals(other: Any?): Boolean {
        return (other as? Guild)?.let { it.name == this.name && it.members.size == this.members.size }
                ?: false
    }

    override fun compareTo(other: Guild): Int {
        return this.level.compareTo(other.level)
    }

    data class Resource(val items: MutableMap<String, Int>, var money: Double)
}