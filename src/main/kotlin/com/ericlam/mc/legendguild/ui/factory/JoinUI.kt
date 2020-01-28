package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import java.util.*
import java.util.concurrent.TimeUnit

object JoinUI : UIFactoryPaginated {

    private val inventories: MutableList<Inventory> = LinkedList()

    init {
        UIManager.p.schedule(delay = 3, unit = TimeUnit.MINUTES) { updateInv() }
    }

    private fun updateInv() {
        inventories.forEach { it.viewers.forEach { p -> p.closeInventory() } }
        inventories.clear()
        var inv = LeaderUI.createPage()
        inventories.add(inv)
        GuildManager.leaderBoard.forEach { guild ->
            if (inv.firstEmpty() == -1) {
                inv = LeaderUI.createPage()
                inventories.add(inv)
            }
            val lore = listOf(
                    "&e擁有者: ${guild.members.find { it.role == GuildPlayer.Role.POPE }?.name}",
                    "&b等級: ${guild.currentLevel}"
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

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        return inventories
    }

    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = mutableMapOf()

    override fun createPage(): Inventory {
        return UIManager.p.createGUI(
                rows = 6, title = "&a宗門列表一覽 (三分鐘更新一次)",
                fills = mapOf(
                        0..53 to Clicker(UIManager.p.itemStack(Material.AIR)) { player, stack ->
                            val guild = stack.itemMeta?.displayName?.removePrefix("§e") ?: return@Clicker
                            val res = player.join(guild)
                            player.sendMessage(Lang[res.path].format(guild))
                        },
                        (6 row 2)..(6 row 8) to Clicker(UIFactoryPaginated.decorate)
                )
        ) {
            mapOf(
                    6 row 1 to Clicker(UIFactoryPaginated.previous) { player, _ ->
                        val iterator = JoinerUI.getIterator(player)
                        if (iterator.hasPrevious()) {
                            UIManager.openUI(player, iterator.previous())
                        } else {
                            player.sendMessage(Lang.Page["no-previous"])
                        }
                    },
                    6 row 9 to Clicker(UIFactoryPaginated.next) { player, _ ->
                        val iterator = JoinerUI.getIterator(player)
                        if (iterator.hasNext()) {
                            UIManager.openUI(player, iterator.next())
                        } else {
                            player.sendMessage(Lang.Page["no-next"])
                        }
                    }
            )
        }
    }

    private val JoinResponse.path: String
        get() {
            return when (this) {
                JoinResponse.NOT_INVITED -> "not-invited"
                JoinResponse.UNKNOWN_GUILD -> "unknown-guild"
                JoinResponse.FULL -> "full"
                JoinResponse.ALREADY_IN_SAME_GUILD -> "same-guild"
                JoinResponse.ALREADY_IN_OTHER_GUILD -> "in-guild"
                JoinResponse.SUCCESS -> "success"
            }
        }
}