package com.ericlam.mc.legendguild.ui.factory.request

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.dao.QuestPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.UIFactoryPaginated
import de.tr7zw.nbtapi.NBTItem
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
            val requestItems = LegendGuild.questPlayerController.find { guild[user] != null }
            val queue = ConcurrentLinkedDeque(requestItems)
            while (queue.isNotEmpty()) {
                val requestItem = queue.poll()
                val request = requestItem?.request ?: continue
                val skull = Bukkit.getOfflinePlayer(requestItem.user).skullItem
                skull.lore = listOf("&e委託內容:") + request.goal + listOf("&c===========", "&b貢獻值獎勵: ${request.contribute}")
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

    override fun addPlayer(player: OfflinePlayer) {
        val request = LegendGuild.questPlayerController.findById(player.uniqueId)?.request ?: return
        val inventories = paginatedCaches[player.guild] ?: return
        var currentInv = inventories.last()
        val item = player.skullItem
        item.lore = listOf("&e委託內容:") + request.goal + listOf("&c===========", "&b貢獻值獎勵: ${request.contribute}")
        if (currentInv.firstEmpty() == -1) {
            currentInv = createPage()
            inventories.add(currentInv)
        }
        currentInv.addItem(item)
    }

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
                            val request = LegendGuild.questPlayerController.findById(id)?.request ?: let {
                                player.sendMessage(Lang.Request["job-not-found"])
                                return@Clicker
                            }
                            val jobItem = QuestPlayer.JobItem(request, player.uniqueId)
                            val b = LegendGuild.questPlayerController.update(player.uniqueId) { job = jobItem } == null
                            if (b) LegendGuild.questPlayerController.save { QuestPlayer(player.uniqueId, job = jobItem) }
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