package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.catch
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

object PromoteUI : UIFactoryPaginated {

    override val paginatedCaches: MutableMap<Guild, MutableList<Inventory>> = ConcurrentHashMap()
    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = ConcurrentHashMap()
    private val roleSetter: MutableMap<Player, GuildPlayer> = ConcurrentHashMap()

    init {
        UIManager.p.listen<AsyncPlayerChatEvent> {
            roleSetter.remove(it.player)?.also { gplayer ->
                catch<IllegalArgumentException>({ e ->
                    it.player.sendMessage()
                }) {
                    val newRole = GuildPlayer.Role.fromName(it.message)
                            ?: GuildPlayer.Role.values().find { r -> r.name.equals(it.message, ignoreCase = true) }
                            ?: let { _ ->
                                it.player.sendMessage(Lang["no-role"].format(it.message))
                                return@also
                            }
                    gplayer.role = newRole
                    gplayer.player.player?.refreshPermissions()
                    gplayer.player.player?.closeInventory()
                    UIManager.clearCache(gplayer.player)
                    it.player.tellSuccess()
                }
            }
        }
    }

    override fun createPage(): Inventory {
        return UIManager.p.createGUI(
                rows = 6, title = "&a晉升成員",
                fills = mapOf(
                        0..53 to Clicker(UIManager.p.itemStack(Material.AIR)) { player, stack ->
                            val playerName = stack.itemMeta?.displayName?.removePrefix("§e") ?: return@Clicker
                            val gPlayer = Bukkit.getPlayerUniqueId(playerName)?.let { Bukkit.getOfflinePlayer(it).guildPlayer }
                                    ?: let {
                                        player.sendMessage(Lang["player-not-found"])
                                        return@Clicker
                                    }
                            player.closeInventory()
                            roleSetter[player] = gPlayer
                            player.sendMessage(Lang.Setter["role"])
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
        val g = player.guild ?: return
        val gPlayer = player.guildPlayer ?: return
        var inv = paginatedCaches[g]?.lastOrNull() ?: return
        while (inv.firstEmpty() == -1) {
            inv = createPage()
            paginatedCaches[g]!!.add(inv)
        }
        inv.addItem(gPlayer.toSkull {
            listOf(
                    "&e貢獻值:&7 $contribution",
                    "&e身份:&7 ${role.ch}",
                    "點擊以輸入晉升職位"
            )
        })
    }
}