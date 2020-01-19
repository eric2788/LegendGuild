package com.ericlam.mc.legendguild

import com.ericlam.mc.legendguild.guild.Guild
import com.ericlam.mc.legendguild.guild.GuildPlayer
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

fun OfflinePlayer.leaveGuild(): Boolean {
    val gp = this.guildPlayer ?: return false
    return if (gp.role != GuildPlayer.Role.POPE) {
        LegendGuild.guildPlayerController.delete(this.uniqueId)
        true
    } else {
        return if (LegendGuild.guildController.delete(gp.guild)) {
            LegendGuild.guildPlayerController.deleteSome {
                guild == name
            }.forEach {
                Bukkit.getPlayer(it)?.sendMessage(LegendGuild.lang["guild-deleted"])
            }
            true
        } else {
            false
        }
    }
}

val OfflinePlayer.guildPlayer: GuildPlayer?
    get() = LegendGuild.guildPlayerController.findById(this.uniqueId)

val OfflinePlayer.guild: Guild?
    get() = GuildManager[this.uniqueId]
