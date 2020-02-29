package com.ericlam.mc.legendguild.ui.factory.request

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.legendguild.Lang
import com.ericlam.mc.legendguild.LegendGuild
import com.ericlam.mc.legendguild.notify
import com.ericlam.mc.legendguild.tellSuccess
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.UIFactory
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

object JobInfoUI : UIFactory {

    private val tryFinish: ItemStack = UIManager.p.itemStack(
            Material.DIAMOND_PICKAXE,
            display = "&e告知委託者為完成",
            lore = listOf("&b點擊告知")
    )

    private val barrier: ItemStack = UIManager.p.itemStack(Material.BARRIER, display = "&c你目前沒有工作。")

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        return invCaches[bPlayer] ?: let {
            UIManager.p.createGUI(1, "&a你的工作") {
                mapOf(
                        1 to Clicker(barrier),
                        8 to Clicker(tryFinish) { player, _ ->
                            LegendGuild.questPlayerController.findById(player.uniqueId)?.job?.also { item ->
                                Bukkit.getOfflinePlayer(item.owner)?.notify(Lang.Request["request-finish"].format(player.name))?.also {
                                    player.tellSuccess()
                                } ?: also {
                                    player.sendMessage(Lang["player-not-found"])
                                }
                            } ?: also {
                                player.sendMessage(Lang.Request["job-not-found"])
                                player.closeInventory()
                            }
                        }
                )
            }
        }.also {
            updateInfo(bPlayer, it)
            invCaches[bPlayer] = it
        }
    }

    override val invCaches: MutableMap<OfflinePlayer, Inventory>
        get() = ConcurrentHashMap()


    override fun updateInfo(player: OfflinePlayer, inventory: Inventory) {
        val job = LegendGuild.questPlayerController.findById(player.uniqueId)?.job ?: let {
            invCaches.remove(player)
            return
        }
        val infoItem: ItemStack = UIManager.p.itemStack(Material.PAPER,
                display = "&e委託內容",
                lore = job.goal
        )
        inventory.setItem(1, infoItem)
    }

}