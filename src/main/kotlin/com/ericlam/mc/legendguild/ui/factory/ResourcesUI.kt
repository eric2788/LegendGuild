package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.GuildManager
import com.ericlam.mc.legendguild.Lang
import com.ericlam.mc.legendguild.LegendGuild
import com.ericlam.mc.legendguild.guild
import com.ericlam.mc.legendguild.guild.Guild
import com.ericlam.mc.legendguild.ui.UIManager.p
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

object ResourcesUI : UIFactory {

    private val money = p.itemStack(
            material = Material.GOLD_BLOCK,
            display = "&6金錢捐贈",
            lore = with(LegendGuild.config.postResources) {
                listOf(
                        "將花費 $$money",
                        "來獲得 $money_contribute 貢獻值"
                ).map { "&e$it" }
            }
    )

    private val item = p.itemStack(
            material = Material.STONE,
            display = "&e捐贈道具",
            lore = with(LegendGuild.config.postResources) {
                listOf(
                        "&c&o捐贈你鼠標拖帶的物品。",
                        "&e可捐贈道具及貢獻值獎勵如下:"
                ) + items.map { "&b${it.key} &7- &6$${it.value}" }
            }
    )

    override val guildInvCaches: MutableMap<Guild, Inventory> = ConcurrentHashMap()

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        val guild = bPlayer.guild ?: return null
        return guildInvCaches[guild] ?: let {
            p.createGUI(
                    rows = 3, title = "&c資源界面"
            ) {
                mapOf(
                        2 row 3 to Clicker(money) { player, _ ->
                            val res = GuildManager.postResource(player, GuildManager.ResourceType.MONEY)
                            player.sendMessage(Lang[getPath(res)])
                        },
                        2 row 7 to Clicker(item) { player, _ ->
                            val res = GuildManager.postResource(player, GuildManager.ResourceType.ITEM)
                            player.sendMessage(Lang[getPath(res)])
                        }
                )
            }
        }.also {
            updateGInfo(guild, it)
            guildInvCaches[guild] = it
        }
    }

    private fun getPath(res: GuildManager.ResourceResponse): String {
        return when (res) {
            GuildManager.ResourceResponse.SUCCESS -> "success"
            GuildManager.ResourceResponse.INVALID_ITEM -> "invalid-item"
            GuildManager.ResourceResponse.NOT_ENOUGH_MONEY -> "no-money"
            GuildManager.ResourceResponse.NOT_IN_GUILD -> "not-in-guild"
        }
    }

    override fun updateGInfo(guild: Guild, inventory: Inventory) {
        val info = p.itemStack(
                material = Material.EMERALD,
                display = "&a目前公會資源",
                lore = listOf(
                        "&6金錢 $${guild.resource.money}",
                        "&e道具資源:"
                ) + guild.resource.items.map { "${Lang.Item[it.key]}: ${it.value} 個" }.toList()
        )
        inventory.setItem(1 row 5, info)
    }
}