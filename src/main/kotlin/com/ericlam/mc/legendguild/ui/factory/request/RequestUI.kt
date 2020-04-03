package com.ericlam.mc.legendguild.ui.factory.request

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.legendguild.Lang
import com.ericlam.mc.legendguild.LegendGuild
import com.ericlam.mc.legendguild.materialHead
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.MainUI
import com.ericlam.mc.legendguild.ui.factory.UIFactory
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

object RequestUI : UIFactory {

    override val invCaches: MutableMap<OfflinePlayer, Inventory> = ConcurrentHashMap()

    private val list = UIManager.p.itemStack(Material.LADDER, display = "&e查看委託列表")

    private val request = UIManager.p.itemStack(Material.PAPER, display = "&e你的委託")

    private val job = UIManager.p.itemStack(materialHead, display = "&e你的接單")

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        return invCaches[bPlayer] ?: let {
            UIManager.p.createGUI(1, "&e任務界面") {
                mapOf(
                        0 to Clicker(request) { p, _ ->
                            LegendGuild.questPlayerController.findById(p.uniqueId)?.request ?: let {
                                p.sendMessage(Lang.Request["request-not-found"])
                                return@Clicker
                            }

                            UIManager.openUI(p, RequestUI)
                        },
                        1 to Clicker(job) { p, _ ->
                            LegendGuild.questPlayerController.findById(p.uniqueId)?.job ?: let {
                                p.sendMessage(Lang.Request["job-not-found"])
                                return@Clicker
                            }
                            UIManager.openUI(p, JobInfoUI)
                        },
                        7 to Clicker(list) { p, _ ->
                            UIManager.openUI(p, RequestListUI)
                        },
                        8 to MainUI.backMainButton
                )
            }
        }
    }
}