package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.ui.UIManager
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

object JoinerUI : UIFactoryPaginated {

    override val paginatedCaches: MutableMap<Guild, MutableList<Inventory>> = ConcurrentHashMap()

    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = ConcurrentHashMap()

    override fun customFilter(guild: Guild, item: ItemStack): Boolean {
        return !guild.members.map { m -> m.name }.contains(NBTItem(item).getString("guild.head.owner"))
    }

    override fun createPage(): Inventory {
        return UIManager.p.createGUI(
                rows = 6, title = "&a申請者列表",
                fills = mapOf(
                        0..53 to Clicker(UIManager.p.itemStack(Material.AIR)) { player, stack ->
                            val guild = player.guild ?: let {
                                player.sendMessage(Lang["not-in-guild"])
                                return@Clicker
                            }
                            val uuid = NBTItem(stack).getString("guild.head.owner")?.let { UUID.fromString(it) }
                                    ?: let {
                                        player.sendMessage(Lang["player-not-found"])
                                        return@Clicker
                                    }

                            val playerName = Bukkit.getOfflinePlayer(uuid).name ?: "[找不到名稱]"
                            BukkitPlugin.plugin.debug("${player.name} just clicked a button for accepting $playerName !")
                            when {
                                isLeftClick -> {
                                    if (LegendGuild.guildPlayerController.findById(uuid) != null) {
                                        player.sendMessage(Lang["joined-guild"].format(playerName))
                                        return@Clicker
                                    }
                                    val offline = Bukkit.getOfflinePlayer(uuid)
                                    BukkitPlugin.plugin.debug("the accept of ${player.name} is left click")
                                    offline.joinGuild(guild.name)
                                }
                                isRightClick -> {
                                    BukkitPlugin.plugin.debug("the accept of ${player.name} is right click")
                                    player.tellSuccess()
                                }
                                else -> {
                                    return@Clicker
                                }
                            }

                            guild.wannaJoins.remove(uuid)
                            clickedInventory?.remove(stack)
                        },
                        (6 row 2)..(6 row 8) to Clicker(UIFactoryPaginated.decorate)
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

    override fun addPlayer(player: OfflinePlayer) {
        val inventories = paginatedCaches.keys.find { g -> g.wannaJoins.contains(player.uniqueId) }?.let { paginatedCaches[it] }
                ?: let {
                    BukkitPlugin.plugin.debug("joiner: cannot find any guild inventories, use getPaginatedUI method")
                    getPaginatedUI(player)
                    return
                }
        var inv = inventories.lastOrNull() ?: let {
            BukkitPlugin.plugin.debug("joiner inventories is empty")
            return
        }
        BukkitPlugin.plugin.debug("${this::class.simpleName} adding player ${player.name}")
        while (inv.firstEmpty() == -1) {
            inv = createPage()
            inventories.add(inv)
        }
        inv.addItem(player.joinerSkull)
    }

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        val guild = bPlayer.guild ?: return emptyList()
        BukkitPlugin.plugin.debug("joiner current paginatedCaches size: ${paginatedCaches.size}")
        BukkitPlugin.plugin.debug("joiner current paginatedCaches details: ${paginatedCaches.map { "${it.key.name} => ${it.value.size}" }}")
        return paginatedCaches[guild] ?: let {
            BukkitPlugin.plugin.debug("initializing joiner inventory list for ${guild.name}")
            val inventories = mutableListOf<Inventory>()
            var currentInv = createPage()
            inventories.add(currentInv)
            val queue = ConcurrentLinkedDeque(guild.wannaJoins.mapNotNull { Bukkit.getOfflinePlayer(it) })
            while (queue.isNotEmpty()) {
                val gPlayer = queue.poll()
                val skull = gPlayer.joinerSkull
                currentInv.addItem(skull)
                if (currentInv.firstEmpty() == -1) {
                    currentInv = createPage()
                    inventories.add(currentInv)
                }
            }
            BukkitPlugin.plugin.debug("joiner inventory list initial size: ${inventories.size}")
            inventories
        }.also {
            updatePaginatedInfo(guild, it)
            paginatedCaches[guild] = it
        }
    }
}