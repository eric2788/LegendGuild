package com.ericlam.mc.legendguild.ui

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.legendguild.Lang
import com.ericlam.mc.legendguild.ui.factory.*
import com.ericlam.mc.legendguild.ui.factory.request.JobInfoUI
import com.ericlam.mc.legendguild.ui.factory.request.YourRequestUI
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

object UIManager {

    val p: BukkitPlugin
        get() = BukkitPlugin.plugin

    private val cachesList: MutableList<UIFactory> = mutableListOf()

    fun addUI(ui: UIFactory) {
        cachesList.add(ui)
    }

    init {

        addUI(MainUI)
        addUI(ContributeUI)
        addUI(ResourcesUI)
        addUI(JoinerUI)
        addUI(PromoteUI)
        addUI(SalaryUI)
        addUI(ShopUI)
        addUI(JobInfoUI)
        addUI(YourRequestUI)

        p.schedule(period = 10) {
            cachesList.forEach { factory ->
                factory.invCaches.forEach(factory::updateInfo)
                factory.guildInvCaches.forEach(factory::updateGInfo)
                if (factory is UIFactoryPaginated) {
                    factory.paginatedCaches.forEach(factory::updatePaginatedInfo)
                }
            }
        }
    }

    fun clearCache(p: OfflinePlayer) {
        cachesList.filter { ui -> ui.invCaches.containsKey(p) }.forEach { ui -> ui.invCaches.remove(p) }
    }

    fun openUI(p: Player, inv: UIFactory) {
        val ui = inv.getUI(p) ?: let {
            p.sendMessage(Lang["not-in-guild"])
            return
        }
        this.openUI(p, ui)
    }

    fun openUI(p: Player, inv: Inventory) {
        this.p.schedule { p.openInventory(inv) }
    }


}