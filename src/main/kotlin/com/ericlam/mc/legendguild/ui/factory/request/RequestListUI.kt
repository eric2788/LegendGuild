package com.ericlam.mc.legendguild.ui.factory.request

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.dao.QuestPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.UIFactoryPaginated
import de.tr7zw.changeme.nbtapi.NBTItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

object RequestListUI : UIFactoryPaginated {

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        val guild = bPlayer.guild ?: return emptyList()
        return paginatedCaches[guild] ?: let {
            val inventories = mutableListOf<Inventory>()
            var currentInv = createPage()
            inventories.add(currentInv)
            val requests = LegendGuild.questPlayerController.find { guild[user] != null }.mapNotNull { p -> Bukkit.getOfflinePlayer(p.user) }
            val queue = ConcurrentLinkedDeque<OfflinePlayer>(requests)
            while (queue.isNotEmpty()) {
                val gPlayer = queue.poll()
                val skull = gPlayer.skullItem
                currentInv.addItem(skull)
                if (currentInv.firstEmpty() == -1) {
                    currentInv = createPage()
                    inventories.add(currentInv)
                }
            }
            inventories
        }.also {
            updatePaginatedInfo(guild, it)
            paginatedCaches[guild] = it
        }
    }

    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>>
        get() = ConcurrentHashMap()

    override val paginatedCaches: MutableMap<Guild, MutableList<Inventory>>
        get() = ConcurrentHashMap()

    override fun createPage(): Inventory {
        return UIManager.p.createGUI(
                rows = 6,
                title = "委託列表一覽",
                fills = mapOf(
                        0..53 to Clicker(ItemStack(Material.AIR)) { player, stack ->
                            val id = NBTItem(stack).getString("guild.head.owner")?.let { UUID.fromString(it) } ?: let {
                                player.sendMessage(Lang["player-not-found"])
                                return@Clicker
                            }
                            val requestItem = QuestPlayer.RequestItem(stack.lore?.toList() ?: emptyList(), id)
                            val b = LegendGuild.questPlayerController.update(player.uniqueId) { job = requestItem } == null
                            if (b) LegendGuild.questPlayerController.save { QuestPlayer(player.uniqueId, job = requestItem) }
                            player.sendMessage(Lang.Request["got-job"])
                            Bukkit.getOfflinePlayer(id)?.notify(Lang.Request["accepted"].format(player.name))
                        }
                )
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