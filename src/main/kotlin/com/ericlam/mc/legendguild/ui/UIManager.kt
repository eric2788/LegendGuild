package com.ericlam.mc.legendguild.ui

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.legendguild.LegendGuild
import com.ericlam.mc.legendguild.ui.factory.ContributeUI
import com.ericlam.mc.legendguild.ui.factory.MainUI
import com.ericlam.mc.legendguild.ui.factory.ResourcesUI
import com.ericlam.mc.legendguild.ui.factory.UIFactory
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

object UIManager {

    val p: BukkitPlugin
        get() = BukkitPlugin.plugin

    private val cachesList: MutableList<UIFactory> = mutableListOf()

    init {
        cachesList.add(MainUI)
        cachesList.add(ContributeUI)
        cachesList.add(ResourcesUI)

        p.schedule(period = 10) {
            cachesList.forEach { factory ->
                factory.invCaches.forEach(factory::updateInfo)
                factory.guildInvCaches.forEach(factory::updateGInfo)
            }
        }
    }

    fun openUI(p: Player, inv: UIFactory) {
        val ui = inv.getUI(p) ?: let {
            p.sendMessage(LegendGuild.lang["not-in-guild"])
            return
        }
        this.p.schedule { p.openInventory(ui) }
    }

    fun openUI(p: Player, inv: Inventory) {
        this.p.schedule { p.openInventory(inv) }
    }


}