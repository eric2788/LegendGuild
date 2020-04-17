package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.catch
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

object PromoteUI : UIFactoryPaginated {

    override val paginatedCaches: MutableMap<Guild, MutableList<Inventory>> = ConcurrentHashMap()
    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = ConcurrentHashMap()
    private val roleSetter: MutableMap<Player, OfflinePlayer> = ConcurrentHashMap()


    override fun customFilter(guild: Guild, item: ItemStack): Boolean {
        return guild.members.map { it.uuid.toString() }.contains(NBTItem(item.also { it.amount = 1 }).getString("guild.head.owner"))
    }

    init {
        UIManager.p.listen<AsyncPlayerChatEvent> {
            roleSetter.remove(it.player)?.also { p ->
                catch<IllegalArgumentException>({ _ ->
                    it.player.sendMessage()
                }) {
                    it.isCancelled = true
                    val newRole = GuildPlayer.Role.fromName(it.message)
                            ?: GuildPlayer.Role.values().find { r -> r.name.equals(it.message, ignoreCase = true) }
                            ?: let { _ ->
                                it.player.sendMessage(Lang["no-role"].mFormat(it.message))
                                return@also
                            }

                    LegendGuild.guildPlayerController.update(p.uniqueId) {
                        this.role = newRole
                    }
                    UIManager.p.schedule {
                        p.player?.refreshPermissions(LegendGuild.attachment(p.player))
                        p.player?.closeInventory()
                        UIManager.clearCache(p)
                        it.player.tellSuccess()
                    }
                }
            }
        }
    }

    override fun createPage(): Inventory {
        BukkitPlugin.plugin.debug("Creating new page of ${this::class.simpleName}")
        pageCache.clear()
        BukkitPlugin.plugin.debug("${this::class.simpleName} new page, so clear pageCache")
        return UIManager.p.createGUI(
                rows = 6, title = "&a晉升成員",
                fills = mapOf(
                        0..53 to Clicker(UIManager.p.itemStack(Material.AIR)) { player, stack ->
                            val uuid = NBTItem(stack).getString("guild.head.owner") ?: return@Clicker
                            val offlinePlayer = UUID.fromString(uuid)?.let { Bukkit.getOfflinePlayer(it) }
                                    ?: let {
                                        player.sendMessage(Lang["player-not-found"])
                                        return@Clicker
                                    }
                            player.closeInventory()
                            roleSetter[player] = offlinePlayer
                            player.sendMessage(Lang.Setter["role"])
                        },
                        (6 row 2)..(6 row 8) to Clicker(UIFactoryPaginated.decorate)
                )
        ) {
            pageOperator
        }
    }

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        val guild = bPlayer.guild ?: return emptyList()
        return paginatedCaches[guild] ?: let {
            val inventories = mutableListOf<Inventory>()
            var currentInv = createPage()
            inventories.add(currentInv)
            val queue = ConcurrentLinkedDeque(guild.members)
            while (queue.isNotEmpty()) {
                val gPlayer = queue.poll()
                val skull = gPlayer.toSkull {
                    listOf(
                            "&e貢獻值:&7 $contribution",
                            "&e身份:&7 ${role.ch}",
                            "點擊以輸入晉升職位"
                    )
                }
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

    override fun addPlayer(player: OfflinePlayer) {
        val g = player.guild ?: let {
            BukkitPlugin.plugin.debug("cannot find guild of ${player.name}")
            return
        }
        val gPlayer = player.guildPlayer ?: let {
            BukkitPlugin.plugin.debug("cannot find guildPlayer of ${player.name}")
            return
        }
        val inventories = paginatedCaches[g] ?: let {
            BukkitPlugin.plugin.debug("empty inventory list, creating new one")
            val i = getPaginatedUI(player)
            if (i.isNotEmpty()) {
                addPlayer(player)
                return
            } else {
                BukkitPlugin.plugin.debug("promote inventory of guild ${g.name} is empty")
                return
            }
        }
        var inv = inventories.lastOrNull() ?: let {
            BukkitPlugin.plugin.debug("promote inventory of guild ${g.name} is empty")
            return
        }
        BukkitPlugin.plugin.debug("${this::class.simpleName} adding player ${player.name} in guild inventory ${g.name}")
        if (inv.firstEmpty() == -1) {
            BukkitPlugin.plugin.debug("last inventory fulled, creating new one for promoteUI")
            inv = createPage()
            inventories.add(inv)
        }
        val skull = gPlayer.toSkull {
            listOf(
                    "&e貢獻值:&7 $contribution",
                    "&e身份:&7 ${role.ch}",
                    "點擊以輸入晉升職位"
            )
        }
        BukkitPlugin.plugin.debug("adding skull item to promoteUI: $skull")
        inv.addItem(skull)
        BukkitPlugin.plugin.debug("inv item details: ${inv.map { it?.toString() ?: "null" }}")
        debugDetails()
    }
}