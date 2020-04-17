package com.ericlam.mc.legendguild.ui.factory.request

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.not
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
                if (request.taken != null) continue
                val skull = Bukkit.getOfflinePlayer(requestItem.user).toSkull { listOf("&e委託內容:") + request.goal + listOf("&c===========", "&b貢獻值獎勵: ${request.contribute}") }
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

    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = ConcurrentHashMap()

    override val paginatedCaches: MutableMap<Guild, MutableList<Inventory>> = ConcurrentHashMap()

    override fun addPlayer(player: OfflinePlayer) {
        val request = LegendGuild.questPlayerController.findById(player.uniqueId)?.request ?: return
        val g = player.guild ?: let {
            BukkitPlugin.plugin.debug("cannot find guild of ${player.name}")
            return
        }
        val inventories = paginatedCaches[g] ?: let {
            BukkitPlugin.plugin.debug("empty inventory list, creating new one")
            val i = getPaginatedUI(player)
            if (i.isNotEmpty()) {
                addPlayer(player)
                return
            } else {
                BukkitPlugin.plugin.debug("request list inventory of guild ${g.name} is empty")
                return
            }
        }
        var currentInv = inventories.lastOrNull() ?: let {
            BukkitPlugin.plugin.debug("request list inventory of guild ${g.name} is empty")
            return
        }
        BukkitPlugin.plugin.debug("${this::class.simpleName} adding player ${player.name}")
        val item = player.toSkull { listOf("&e委託內容:") + request.goal + listOf("&c===========", "&b貢獻值獎勵: ${request.contribute}") }
        if (currentInv.firstEmpty() == -1) {
            currentInv = createPage()
            inventories.add(currentInv)
        }
        currentInv.addItem(item)
    }

    override fun customFilter(guild: Guild, item: ItemStack): Boolean {
        return guild.members.map { it.uuid.toString() }.contains(NBTItem(item).getString("guild.head.owner"))
    }

    override fun createPage(): Inventory {
        BukkitPlugin.plugin.debug("Creating new page of ${this::class.simpleName}")
        pageCache.clear()
        BukkitPlugin.plugin.debug("${this::class.simpleName} new page, so clear pageCache")
        return UIManager.p.createGUI(
                rows = 6,
                title = "委託列表一覽",
                fills = mapOf(
                        0..53 to Clicker(ItemStack(Material.AIR)) { player, stack ->
                            val id = NBTItem(stack).getString("guild.head.owner")?.let { UUID.fromString(it) } ?: let {
                                player.sendMessage(Lang["player-not-found"])
                                return@Clicker
                            }
                            id.not(player.uniqueId) ?: let {
                                player.sendMessage(Lang.Request["job-self"])
                                return@Clicker
                            }
                            val request = LegendGuild.questPlayerController.findById(id)?.request ?: let {
                                player.sendMessage(Lang.Request["job-not-found"])
                                return@Clicker
                            }
                            val jobItem = QuestPlayer.JobItem(request, id)

                            LegendGuild.questPlayerController.update(player.uniqueId) { job = jobItem } ?: also {
                                LegendGuild.questPlayerController.save { QuestPlayer(player.uniqueId, job = jobItem) }
                            }
                            LegendGuild.questPlayerController.update(id) {
                                this.request ?: also {
                                    BukkitPlugin.plugin.error(IllegalStateException("requestItem of $id not exist!!!"))
                                    return@update
                                }
                                this.request?.taken = player.uniqueId
                            }
                            player.sendMessage(Lang.Request["got-job"])
                            Bukkit.getOfflinePlayer(id)?.notify(Lang.Request["accepted"].mFormat(player.name))
                            inventory.remove(stack)
                        }
                )
        ) {
            pageOperator
        }
    }
}