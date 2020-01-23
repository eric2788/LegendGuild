package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.legendguild.LegendGuild
import com.ericlam.mc.legendguild.guild
import com.ericlam.mc.legendguild.guild.Guild
import com.ericlam.mc.legendguild.guild.GuildPlayer
import com.ericlam.mc.legendguild.tellSuccess
import com.ericlam.mc.legendguild.ui.UIManager.p
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import java.text.MessageFormat
import java.util.concurrent.ConcurrentHashMap

object SettingUI {

    private val guildCaches: MutableMap<Guild, Map<String, Inventory>> = ConcurrentHashMap()
    private val salarySetter: MutableMap<Player, GuildPlayer.Role> = ConcurrentHashMap()

    init {
        p.schedule(period = 10) {
            guildCaches.forEach(::updateGInfo)
        }

        p.listen<AsyncPlayerChatEvent> {
            val role = salarySetter.remove(it.player) ?: return@listen
            it.isCancelled = true
            val salary = it.message.toDoubleOrNull() ?: let { _ ->
                it.player.sendMessage(MessageFormat.format(LegendGuild.lang["not-number"], it.message))
                return@listen
            }
            val g = it.player.guild ?: let { _ ->
                it.player.sendMessage(LegendGuild.lang["not-in-guild"])
                return@listen
            }
            if (salary < LegendGuild.config.default_salaries[role] ?: 0.0) {
                it.player.sendMessage(LegendGuild.lang["lower-than-default"])
                return@listen
            }
            g.salaries[role] = salary
            it.player.tellSuccess()
        }
    }

    private fun updateGInfo(guild: Guild, map: Map<String, Inventory>) {
        map["salary"]?.also { inv ->
            val roleMap = GuildPlayer.Role.values() zip (0..8 step 2)
            roleMap.forEach { (role, slot) ->
                val salary = guild.salaries[role] ?: return@forEach
                val item = p.itemStack(Material.EMERALD_BLOCK,
                        display = ChatColor.YELLOW.toString() + role.ch,
                        lore = listOf(
                                "&b目前薪資: $salary",
                                "&c點我以設定薪資"
                        ))
                inv.setItem(slot, item)
            }
        }
    }

    fun getGuildGui(player: OfflinePlayer, key: String): Inventory? {
        val guild = player.guild ?: return null
        return guildCaches[guild]?.get(key) ?: let {
            createGuildGui(guild)
            return guildCaches[guild]!![key]
        }
    }

    private fun createGuildGui(guild: Guild) {
        val salary = p.createGUI(
                rows = 1, title = "&e薪資設定"
        ) {
            (GuildPlayer.Role.values().dropLast(1) zip (0..8 step 2)).map {
                val item = p.itemStack(Material.EMERALD_BLOCK,
                        display = ChatColor.YELLOW.toString() + it.first.ch,
                        lore = listOf(
                                "&b目前薪資: $guild.sa",
                                "&c點我以設定薪資"
                        ))
                it.second to Clicker(item) { player, _ ->
                    salarySetter[player] = it.first
                }
            }.toMap()
        }
        val joiners = p.createGUI(
                rows = 6, title = "&a申請者列表",
                fills = mapOf(
                        0..53 to Clicker(p.itemStack(Material.AIR)) { player, stack ->
                            val playerName = stack.itemMeta?.displayName?.removePrefix("§e") ?: return@Clicker
                            val uuid = Bukkit.getPlayerUniqueId(playerName) ?: let {
                                player.sendMessage(LegendGuild.lang["player-not-found"])
                                return@Clicker
                            }
                            with(LegendGuild.guildPlayerController) {
                                if (findById(uuid) != null) {
                                    player.sendMessage(MessageFormat.format(LegendGuild.lang["joined-guild"], playerName))
                                    return@Clicker
                                }
                                save { GuildPlayer(uuid, Bukkit.getOfflinePlayer(uuid).name, guild.name) }
                                player.tellSuccess()
                            }
                        }
                )
        ) { mapOf() }
        val map = mapOf("salary" to salary, "joiner" to joiners)
        updateGInfo(guild, map)
        guildCaches[guild] = map
    }

}