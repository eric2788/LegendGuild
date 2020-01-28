package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object LeaderUI : UIFactoryPaginated {

    private val inventories: MutableList<Inventory> = LinkedList()

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        return inventories
    }

    init {
        UIManager.p.schedule(delay = LegendGuild.config.leaderUpdate) { updateInv() }
    }

    private fun updateInv() {
        inventories.forEach { it.viewers.forEach { p -> p.closeInventory() } }
        inventories.clear()
        var inv = createPage()
        inventories.add(inv)
        GuildManager.leaderBoard.forEach { guild ->
            if (inv.firstEmpty() == -1) {
                inv = createPage()
                inventories.add(inv)
            }
            val lore = listOf(
                    "&e等級: ${guild.currentLevel}",
                    "&b總共貢獻值: ${guild.members.map { it.contribution }.sum()}"
            )
            val pope = guild.members.find { it.role == GuildPlayer.Role.POPE }
            val item = UIManager.p.itemStack(LegendGuild.config.materialHead,
                    display = "&a${guild.name}",
                    lore = lore
            ).apply {
                itemMeta = itemMeta.toSkullMeta(pope?.skinValue ?: steveSkin)
            }
            inv.addItem(item)
        }
    }

    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = ConcurrentHashMap()

    override fun createPage(): Inventory {
        return UIManager.p.createGUI(6, "&b宗門排行榜",
                fills = mapOf((6 row 2)..(6 row 8) to Clicker(UIFactoryPaginated.decorate))
        ) {
            mapOf(
                    6 row 1 to Clicker(UIFactoryPaginated.previous) { player, _ ->
                        val iterator = getIterator(player)
                        if (iterator.hasPrevious()) {
                            UIManager.openUI(player, iterator.previous())
                        } else {
                            player.sendMessage(Lang.Page["no-previous"])
                        }
                    },
                    6 row 9 to Clicker(UIFactoryPaginated.next) { player, _ ->
                        val iterator = getIterator(player)
                        if (iterator.hasNext()) {
                            UIManager.openUI(player, iterator.next())
                        } else {
                            player.sendMessage(Lang.Page["no-next"])
                        }
                    }
            )
        }
    }
}