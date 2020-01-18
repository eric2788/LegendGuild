package com.ericlam.mc.legendguild.guild

import com.ericlam.mc.kotlib.config.dao.DataFile
import com.ericlam.mc.kotlib.config.dao.DataResource
import com.ericlam.mc.kotlib.config.dao.PrimaryKey
import com.ericlam.mc.legendguild.LegendGuild
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.*

@DataResource(folder = "PlayerData")
data class GuildPlayer(
        @PrimaryKey val uuid: UUID,
        var name: String,
        val guild: String,
        var contribution: Int = 0,
        var role: Role = Role.OUT_DISCIPLE
) : DataFile {

    enum class Role(val ch: String) {
        POPE("宗主"),
        CO_POPE("副宗主"),
        ELDER("大長老"),
        CO_ELDER("長老"),
        DISCIPLE("內門弟子"),
        OUT_DISCIPLE("外門弟子");

        companion object Factory{
            fun fromName(name: String): Role?{
                return values().find { it.ch == name }
            }
        }
    }

    val player: OfflinePlayer
        get() = Bukkit.getOfflinePlayer(uuid)

    infix fun consume(price: Int): Boolean{
        return contribution.takeUnless { it < price }?.let { contribution -= price }?.let { true } ?: false
    }

    fun leave(): Boolean = LegendGuild.guildPlayerController.delete(uuid)

}