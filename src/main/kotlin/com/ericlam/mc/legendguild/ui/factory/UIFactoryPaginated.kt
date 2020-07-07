package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.ui.UIManager
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

interface UIFactoryPaginated : UIFactory {

    fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory>

    val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>>

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        val ui = getPaginatedUI(bPlayer)
        LegendGuild.debug("UI ${this::class.simpleName} size: ${ui.size}")
        LegendGuild.debug("UI ${this::class.simpleName} firstPage exist: ${ui.firstOrNull() != null}")
        debugDetails()
        return ui.firstOrNull()
    }

    fun getIterator(bPlayer: OfflinePlayer): ListIterator<Inventory> {
        return pageCache[bPlayer] ?: getPaginatedUI(bPlayer).toList().let {
            LegendGuild.debug("iterator current size: ${it.size}")
            it.listIterator().also { iter ->
                iter.next()
                pageCache[bPlayer] = iter
            }
        }
    }

    fun createPage(): Inventory

    fun addPlayer(player: OfflinePlayer) {}

    val paginatedCaches: MutableMap<Guild, MutableList<Inventory>>
        get() = ConcurrentHashMap()

    fun updatePaginatedInfo(guild: Guild, inventories: MutableList<Inventory>) {
        val invs = inventories.takeIf { it.isNotEmpty() } ?: return
        LegendGuild.debug("updating ${this::class} paginated info for ${guild.name}")
        invs.takeIf { it.size > 1 }?.reduce { inv1, inv2 ->
            val iterator = inv2.iterator()
            while (inv1.firstEmpty() != -1 && iterator.hasNext()) {
                inv1.addItem(iterator.next())
            }
            inv2
        }
        invs.replaceAll { inv ->
            inv.contents = inv.contents.filterNotNull()
                    .filter { it.type != Material.AIR }
                    .filter { customFilter(guild, it).also { b -> LegendGuild.debug("$it customFilter not remove: $b") } }
                    .filter { !it.unmovable }
                    .toList().toTypedArray()
            inv.setItem(6 row 1, previous)
            inv.setItem(6 row 9, next)
            for (i in (6 row 2)..(6 row 8)) {
                inv.setItem(i, decorate)
            }
            inv
        }
        invs.takeIf { it.size > 1 }?.removeIf { it.firstEmpty() == 0 }
    }

    val ItemStack.unmovable: Boolean
        get() = NBTItem(this).getBoolean("unmovable")


    override fun debugDetails() {
        LegendGuild.debug("total details: ${paginatedCaches.map { (g, l) -> "${g.name} => ${l.map { i -> i.contents.map { it?.toString() ?: "null" } }}" }}")
    }

    fun customFilter(guild: Guild, item: ItemStack): Boolean = true

    val pageOperator: Map<Int, Clicker>
        get() = mapOf(
                6 row 1 to Clicker(previous) { player, _ ->
                    val iterator = getIterator(player)
                    if (iterator.hasPrevious() && iterator.previousIndex() > 0) {
                        LegendGuild.debug("jumped to previous page")
                        UIManager.openUI(player, iterator.previous())
                    } else {
                        player.sendMessage(Lang.Page["no-previous"])
                    }
                },
                6 row 9 to Clicker(next) { player, _ ->
                    val iterator = getIterator(player)
                    if (iterator.hasNext()) {
                        LegendGuild.debug("jumped to next page")
                        UIManager.openUI(player, iterator.next())
                    } else {
                        player.sendMessage(Lang.Page["no-next"])
                    }
                }
        )


    companion object Utils {
        val previous = UIManager.p.itemStack(materialHead, display = "&e上一頁").apply {
            itemMeta = itemMeta.toSkullMeta("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWI2MTQwYWYzMmNiMzY0ZDliZTNiOTRlOTMwODFkNmNmYzhjMjdkM2NmZTBiNGRkNDVlNzg1MjI1ZWIifX19")
        }.let {
            val nbt = NBTItem(it)
            nbt.setBoolean("unmovable", true)
            nbt.item!!
        }

        val next = UIManager.p.itemStack(materialHead, display = "&e下一頁").apply {
            itemMeta = itemMeta.toSkullMeta("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmNhODQyNjdjYjVhMzdkNjk5YWJlN2Q2YTAzMTc4ZGUwODlkN2NmMmU3MjZmMzdkYTNmZTk5N2ZkNyJ9fX0=")
        }.let {
            val nbt = NBTItem(it)
            nbt.setBoolean("unmovable", true)
            nbt.item!!
        }

        val decorate = UIManager.p.itemStack(materialGlassPane).let {
            val nbt = NBTItem(it)
            nbt.setBoolean("unmovable", true)
            nbt.item!!
        }
    }
}