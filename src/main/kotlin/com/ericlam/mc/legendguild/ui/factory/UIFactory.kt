package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.legendguild.dao.Guild
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory

interface UIFactory {
    val invCaches: MutableMap<OfflinePlayer, Inventory>
        get() = mutableMapOf()

    val guildInvCaches: MutableMap<Guild, Inventory>
        get() = mutableMapOf()

    fun getUI(bPlayer: OfflinePlayer): Inventory?

    fun updateInfo(player: OfflinePlayer, inventory: Inventory) {}

    fun updateGInfo(guild: Guild, inventory: Inventory) {}
}