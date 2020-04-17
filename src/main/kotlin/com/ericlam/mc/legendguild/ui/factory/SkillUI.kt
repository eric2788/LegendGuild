package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.ui.UIManager
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

object SkillUI : UIFactory {

    override val guildInvCaches: MutableMap<Guild, Inventory> = ConcurrentHashMap()

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        val g = bPlayer.guild ?: return null
        return guildInvCaches[g] ?: let {
            UIManager.p.createGUI(1, "&e技能升級") {
                (listOf(1, 3, 5, 7) zip GuildSkill.values()).map { (slot, skill) ->
                    slot to Clicker(UIManager.p.itemStack(Material.PAPER, display = "???")) { p, _ ->
                        val res = GuildManager.upgradeSkill(skill, p)
                        p.sendMessage(Lang[res.path])
                        if (res == UpgradeResponse.SUCCESS) updateGInfo(p.guild!!, inventory)
                    }
                }.toMap()
            }
        }.also {
            updateGInfo(g, it)
            guildInvCaches[g] = it
        }
    }


    override fun updateGInfo(guild: Guild, inventory: Inventory) {
        val items = guild.currentSkills.map { (skill, level) ->
            val requirement = LegendGuild.config.skills[skill] ?: let {
                BukkitPlugin.plugin.warning("無法找到 技能 ${skill.ch} 的升級需求！ 已略過。")
                return@map UIManager.p.itemStack(Material.BARRIER, display = "&c找不到升級需求: ${skill.ch}")
            }
            UIManager.p.itemStack(
                    material = Material.PAPER,
                    display = "&e${skill.ch}",
                    lore = listOf(
                            "&a目前等級: &7Lv$level",
                            "&e升級需求:",
                            "  - &f金錢: &6$${requirement.money}"
                    ) + requirement.items.map { (item, amount) -> "  - &f${Lang.Item[item]} &cx$amount" }
            )
        }
        (listOf(1, 3, 5, 7) zip items).forEach { inventory.setItem(it.first, it.second) }
    }
}