package com.ericlam.mc.legendguild.dao

import com.ericlam.mc.kotlib.config.dao.DataFile
import com.ericlam.mc.kotlib.config.dao.DataResource
import com.ericlam.mc.kotlib.config.dao.PrimaryKey
import com.ericlam.mc.legendguild.LegendGuild
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@DataResource(folder = "PlayerData")
data class GuildPlayer(
        @PrimaryKey val uuid: UUID,
        var name: String,
        val guild: String,
        var skinValue: String,
        var contribution: Int = 0,
        var role: Role = Role.OUT_DISCIPLE,
        private var last_contribute: Long = 0,
        private var last_salary: Long = 0,
        private val quests: MutableMap<String, Long> = mutableMapOf()
) : DataFile {

    enum class Role(val ch: String) {
        POPE("宗主"),
        CO_POPE("副宗主"),
        ELDER("大長老"),
        CO_ELDER("長老"),
        DISCIPLE("內門弟子"),
        OUT_DISCIPLE("外門弟子");

        companion object Factory {
            fun fromName(name: String): Role? {
                return values().find { it.ch == name || it.name == name }
            }
        }

        infix fun hasPower(o: Role): Boolean {
            return o.ordinal >= this.ordinal
        }
    }

    val canGetSalary: Boolean
        get() = Duration.between(Timestamp(last_salary).toLocalDateTime(), LocalDateTime.now()).toDays() > 0 && LegendGuild.config.salaries.containsKey(role)

    fun setLastSalary() {
        this.last_salary = System.currentTimeMillis()
    }

    val canContribute: Boolean
        get() = Duration.between(Timestamp(last_contribute).toLocalDateTime(), LocalDateTime.now()).toDays() > 0

    fun setLastContribute() {
        this.last_contribute = System.currentTimeMillis()
    }

    val player: OfflinePlayer
        get() = Bukkit.getOfflinePlayer(uuid)

    infix fun consume(price: Int): Boolean {
        return contribution.takeUnless { it < price }?.let { contribution -= price }?.let { true } ?: false
    }

}