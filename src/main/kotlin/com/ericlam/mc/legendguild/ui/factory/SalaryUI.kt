package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.legendguild.Lang
import com.ericlam.mc.legendguild.LegendGuild
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.guild
import com.ericlam.mc.legendguild.tellSuccess
import com.ericlam.mc.legendguild.ui.UIManager
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

object SalaryUI : UIFactory {

    private val salarySetter: MutableMap<Player, GuildPlayer.Role> = ConcurrentHashMap()

    init {
        UIManager.p.listen<AsyncPlayerChatEvent> {
            salarySetter.remove(it.player)?.also { role ->
                it.isCancelled = true
                val salary = it.message.toDoubleOrNull() ?: let { _ ->
                    it.player.sendMessage(Lang["not-number"].format(it.message))
                    return@also
                }
                val g = it.player.guild ?: let { _ ->
                    it.player.sendMessage(Lang["not-in-guild"])
                    return@also
                }
                if (salary < LegendGuild.config.default_salaries[role] ?: 0.0) {
                    it.player.sendMessage(Lang["lower-than-default"])
                    return@also
                }
                g.salaries[role] = salary
                it.player.tellSuccess()
                LegendGuild.guildController.save { g }
            }
        }
    }

    override val guildInvCaches: MutableMap<Guild, Inventory>
        get() = super.guildInvCaches

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        val guild = bPlayer.guild ?: return null
        return guildInvCaches[guild] ?: let {
            UIManager.p.createGUI(
                    rows = 1, title = "&e薪資設定"
            ) {
                (GuildPlayer.Role.values().drop(1) zip (0..8 step 2)).map {
                    val item = UIManager.p.itemStack(Material.EMERALD_BLOCK,
                            display = ChatColor.YELLOW.toString() + it.first.ch,
                            lore = listOf(
                                    "&b目前薪資: ${guild.salaries[it.first] ?: "NONE"}",
                                    "&c點我以設定薪資"
                            ))
                    it.second to Clicker(item) { player, _ ->
                        salarySetter[player] = it.first
                        player.closeInventory()
                        player.sendMessage(Lang.Setter["salary"])
                    }
                }.toMap()
            }
        }.also {
            updateGInfo(guild, it)
            guildInvCaches[guild] = it
        }
    }

    override fun updateGInfo(guild: Guild, inventory: Inventory) {
        val roleMap = GuildPlayer.Role.values().drop(1) zip (0..8 step 2)
        BukkitPlugin.plugin.debug("updating ${this::class} info for ${guild.name}")
        roleMap.forEach { (role, slot) ->
            val salary = guild.salaries[role] ?: return@forEach
            val item = UIManager.p.itemStack(Material.EMERALD_BLOCK,
                    display = ChatColor.YELLOW.toString() + role.ch,
                    lore = listOf(
                            "&b目前薪資: $salary",
                            "&c點我以設定薪資"
                    ))
            inventory.setItem(slot, item)
        }
    }
}