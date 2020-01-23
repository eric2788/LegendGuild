package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.legendguild.guild.Guild
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

interface UIFactory {
    val invCaches: MutableMap<OfflinePlayer, Inventory>
        get() = mutableMapOf()

    val guildInvCaches: MutableMap<Guild, Inventory>
        get() = mutableMapOf()

    fun getUI(bPlayer: Player): Inventory?

    fun updateInfo(player: OfflinePlayer, inventory: Inventory) {}

    fun updateGInfo(guild: Guild, inventory: Inventory) {}
}