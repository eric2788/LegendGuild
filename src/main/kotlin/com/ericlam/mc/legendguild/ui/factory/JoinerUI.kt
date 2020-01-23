package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.guild.Guild
import com.ericlam.mc.legendguild.guild.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import java.text.MessageFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

object JoinerUI : UIFactoryPaginated {

    override val paginatedCaches: MutableMap<Guild, MutableList<Inventory>> = ConcurrentHashMap()


    override fun createPage(): Inventory {
        return UIManager.p.createGUI(
                rows = 6, title = "&a申請者列表",
                fills = mapOf(
                        0..53 to Clicker(UIManager.p.itemStack(Material.AIR)) { player, stack ->
                            val guild = player.guild ?: let {
                                player.sendMessage(Lang["not-in-guild"])
                                return@Clicker
                            }
                            val playerName = stack.itemMeta?.displayName?.removePrefix("§e") ?: return@Clicker
                            val uuid = Bukkit.getPlayerUniqueId(playerName) ?: let {
                                player.sendMessage(Lang["player-not-found"])
                                return@Clicker
                            }

                            when {
                                isLeftClick -> {
                                    with(LegendGuild.guildPlayerController) {
                                        if (findById(uuid) != null) {
                                            player.sendMessage(MessageFormat.format(Lang["joined-guild"], playerName))
                                            return@Clicker
                                        }
                                        val offline = Bukkit.getOfflinePlayer(uuid)
                                        GlobalScope.launch {
                                            val skin = offline.toSkinValue()
                                            save { GuildPlayer(uuid, offline.name, guild.name, skin) }
                                        }.invokeOnCompletion {
                                            it?.printStackTrace().also { player.tellFailed() }
                                                    ?: player.tellSuccess().also { PromoteUI.addPlayer(player) }
                                        }
                                    }
                                }
                                isRightClick -> {
                                    player.tellSuccess()
                                }
                                else -> {
                                    return@Clicker
                                }
                            }

                            guild.wannaJoins.remove(uuid)
                            clickedInventory?.remove(stack)
                        }
                )
        ) { mapOf() }
    }

    override fun addPlayer(player: OfflinePlayer) {
        val inventories = paginatedCaches.keys.find { g -> g.wannaJoins.contains(player.uniqueId) }?.let { paginatedCaches[it] }
                ?: return
        var inv = inventories.lastOrNull() ?: return
        while (inv.firstEmpty() == -1) {
            inv = createPage()
            inventories.add(inv)
        }
        inv.addItem(player.skullItem)
    }

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        val guild = bPlayer.guild ?: return emptyList()
        return paginatedCaches[guild] ?: let {
            val inventories = mutableListOf<Inventory>()
            var currentInv = createPage()
            inventories.add(currentInv)
            val queue = ConcurrentLinkedDeque<OfflinePlayer>(guild.wannaJoins.mapNotNull { Bukkit.getOfflinePlayer(it) })
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
}