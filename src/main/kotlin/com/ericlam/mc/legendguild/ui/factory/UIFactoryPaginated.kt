package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.materialGlassPane
import com.ericlam.mc.legendguild.materialHead
import com.ericlam.mc.legendguild.toSkullMeta
import com.ericlam.mc.legendguild.ui.UIManager
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

interface UIFactoryPaginated : UIFactory {

    fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory>

    val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>>

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        val ui = getPaginatedUI(bPlayer)
        BukkitPlugin.plugin.debug("UI ${this::class.simpleName} size: ${ui.size}")
        BukkitPlugin.plugin.debug("UI ${this::class.simpleName} firstPage exist: ${ui.firstOrNull() != null}")
        return ui.firstOrNull()
    }

    fun getIterator(bPlayer: OfflinePlayer): ListIterator<Inventory> {
        return pageCache[bPlayer] ?: getPaginatedUI(bPlayer).listIterator().also { pageCache[bPlayer] = it }
    }

    fun createPage(): Inventory

    fun addPlayer(player: OfflinePlayer) {}

    val paginatedCaches: MutableMap<Guild, MutableList<Inventory>>
        get() = ConcurrentHashMap()

    fun updatePaginatedInfo(guild: Guild, inventories: MutableList<Inventory>) {
        val invs = inventories.takeIf { it.isNotEmpty() } ?: return
        invs.takeIf { it.size > 1 }?.reduce { inv1, inv2 ->
            val iterator = inv2.iterator()
            while (inv1.firstEmpty() != -1 && iterator.hasNext()) {
                inv1.addItem(iterator.next())
            }
            inv2
        }
        invs.replaceAll { inv ->
            inv.contents = inv.filterNotNull()
                    .filter { it.type != Material.AIR }
                    .filter { guild.members.map { m -> m.name }.contains(it.itemMeta?.displayName?.removePrefix("§e")) }
                    .distinctBy { it.itemMeta?.displayName }.toTypedArray()
            inv
        }
        invs.takeIf { it.size > 1 }?.removeIf { it.firstEmpty() == 0 }
    }

    companion object Utils {
        val previous = UIManager.p.itemStack(materialHead, display = "&e上一頁").apply {
            itemMeta = itemMeta.toSkullMeta("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWI2MTQwYWYzMmNiMzY0ZDliZTNiOTRlOTMwODFkNmNmYzhjMjdkM2NmZTBiNGRkNDVlNzg1MjI1ZWIifX19")
        }

        val next = UIManager.p.itemStack(materialHead, display = "&e下一頁").apply {
            itemMeta = itemMeta.toSkullMeta("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmNhODQyNjdjYjVhMzdkNjk5YWJlN2Q2YTAzMTc4ZGUwODlkN2NmMmU3MjZmMzdkYTNmZTk5N2ZkNyJ9fX0=")
        }

        val decorate = UIManager.p.itemStack(materialGlassPane)
    }
}