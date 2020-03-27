package com.ericlam.mc.legendguild.ui.factory.request

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.UIFactory
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

object YourRequestUI : UIFactory {

    val finish: ItemStack = UIManager.p.itemStack(Material.BEACON, display = "宣告為完成")

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        return invCaches[bPlayer] ?: let {
            UIManager.p.createGUI(1, "&a你的委託內容") {
                mapOf(
                        7 to Clicker(finish) { player, _ ->
                            val playerRequest = LegendGuild.questPlayerController.findById(player.uniqueId)
                            val request = playerRequest?.request ?: let {
                                player.sendMessage(Lang.Request["request-not-found"])
                                return@Clicker
                            }

                            val taker = request.taken ?: let {
                                player.sendMessage(Lang.Request["noone-taken"])
                                return@Clicker
                            }

                            val takerJob = LegendGuild.questPlayerController.findById(taker)

                            if (takerJob?.job?.owner != player.uniqueId) {
                                player.tellFailed()
                                return@Clicker
                            }

                            takerJob?.job = null
                            val takerPlayer = Bukkit.getOfflinePlayer(taker)
                            takerPlayer?.guildPlayer?.let { it.contribution += playerRequest.request?.contribute ?: 0 }
                            playerRequest.request = null

                            player.tellSuccess()
                            takerPlayer?.notify(Lang.Request["request-done"].format(player.displayName))
                        }
                )
            }
        }
    }


    override val invCaches: MutableMap<OfflinePlayer, Inventory>
        get() = ConcurrentHashMap()

    override fun updateInfo(player: OfflinePlayer, inventory: Inventory) {
        val item = LegendGuild.questPlayerController.findById(player.uniqueId)?.request ?: return
        val info = UIManager.p.itemStack(Material.PAPER,
                display = "&e委託內容",
                lore = item.goal
        )

        val owner = UIManager.p.itemStack(LegendGuild.config.materialHead, display = "接手者: ${item.taken ?: "沒有人"}")
        inventory.setItem(0, info)
        inventory.setItem(7, owner)
    }
}