package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.legendguild.guild.Guild
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

interface UIFactoryPaginated : UIFactory {

    fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory>

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        return getPaginatedUI(bPlayer).getOrNull(0)
    }

    fun createPage(): Inventory

    fun addPlayer(player: OfflinePlayer)

    val paginatedCaches: MutableMap<Guild, MutableList<Inventory>>
        get() = ConcurrentHashMap()

    fun updatePaginatedInfo(guild: Guild, inventories: MutableList<Inventory>) {
        inventories.replaceAll { inv ->
            inv.contents = inv.filterNotNull()
                    .filter { it.type != Material.AIR }
                    .filter { guild.members.map { m -> m.name }.contains(it.itemMeta?.displayName?.removePrefix("§e")) }
                    .distinctBy { it.itemMeta?.displayName }.toTypedArray()
            inv
        }
        inventories.removeIf { it.firstEmpty() == 0 }
    }

}