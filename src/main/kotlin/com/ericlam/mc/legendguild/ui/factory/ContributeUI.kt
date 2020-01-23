package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.not
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.GuildManager
import com.ericlam.mc.legendguild.LegendGuild
import com.ericlam.mc.legendguild.guildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

//澆水
object ContributeUI : UIFactory {

    override val invCaches: MutableMap<OfflinePlayer, Inventory> = ConcurrentHashMap()


    private val money = UIManager.p.itemStack(
            material = Material.GOLD_BLOCK,
            display = "&e金錢貢獻",
            lore = with(LegendGuild.config.dailyContribution.money) {
                listOf(
                        "將花費金錢 $$need",
                        "以獲得 $contribute 貢獻",
                        "及 $exp 經驗值"
                ).map { "&e$it" }
            }
    )

    private val points = UIManager.p.itemStack(
            material = Material.PAPER,
            display = "&b點卷貢獻",
            lore = with(LegendGuild.config.dailyContribution.points) {
                listOf(
                        "將花費點卷 $need",
                        "以獲得 $contribute 貢獻",
                        "及 $exp 經驗值"
                ).map { "&e$it" }
            }
    )

    override fun getUI(bPlayer: Player): Inventory? {
        return invCaches[bPlayer] ?: let {
            UIManager.p.createGUI(
                    rows = 3, title = "&c貢獻界面"
            ) {
                mapOf(
                        2 row 3 to Clicker(money) { player, _ ->
                            val response = GuildManager.contributeMoney(player)
                            player.sendMessage(LegendGuild.lang[getPath(response)])
                        },
                        2 row 7 to Clicker(points) { player, _ ->
                            val response = GuildManager.contributePoints(player)
                            player.sendMessage(LegendGuild.lang[getPath(response).not("no-money") ?: "no-points"])
                        }
                )
            }
        }.also {
            updateInfo(bPlayer, it)
            invCaches[bPlayer] = it
        }
    }

    override fun updateInfo(player: OfflinePlayer, inventory: Inventory) {
        val gp = player.guildPlayer ?: return
        val contribute = UIManager.p.itemStack(
                material = Material.EMERALD,
                display = "&b你的貢獻值: ${gp.contribution}"
        )
        inventory.setItem(1 row 5, contribute)
    }

    private fun getPath(response: GuildManager.ContributeResponse): String {
        return when (response) {
            GuildManager.ContributeResponse.NOT_ENOUGH_MONEY -> "no-money"
            GuildManager.ContributeResponse.NOT_IN_GUILD -> "not-in-guild"
            GuildManager.ContributeResponse.ALREADY_DID_TODAY -> "did-today"
            GuildManager.ContributeResponse.SUCCESS -> "success"
        }
    }

}