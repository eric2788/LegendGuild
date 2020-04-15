package com.ericlam.mc.legendguild.ui

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.legendguild.Lang
import com.ericlam.mc.legendguild.LegendGuild
import com.ericlam.mc.legendguild.guild
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
        addUI(QuestUI)
        addUI(YourRequestUI)
        addUI(SkillUI)

        p.schedule(period = 10) {
            cachesList.forEach { factory ->
                factory.invCaches.forEach(factory::updateInfo)
                factory.guildInvCaches.forEach { (g, inv) -> LegendGuild.guildController.findById(g.name)?.let { factory.updateGInfo(it, inv) } }
                if (factory is UIFactoryPaginated) {
                    factory.paginatedCaches.forEach { (g, inv) -> LegendGuild.guildController.findById(g.name)?.let { factory.updatePaginatedInfo(it, inv) } }
                }
            }
        }
    }

    fun clearCache(p: OfflinePlayer) {
        cachesList.filter { ui -> ui.invCaches.containsKey(p) }.forEach { ui -> ui.invCaches.remove(p) }
        p.guild?.also { g -> cachesList.filter { ui -> ui.guildInvCaches.containsKey(g) }.forEach { ui -> ui.guildInvCaches.remove(g) } }
        p.guild?.also { g -> cachesList.filter { ui -> ui is UIFactoryPaginated && ui.paginatedCaches.containsKey(g) }.forEach { ui -> (ui as UIFactoryPaginated).paginatedCaches.remove(g) } }
        cachesList.filter { ui -> ui is UIFactoryPaginated && ui.pageCache.containsKey(p) }.forEach { ui -> (ui as UIFactoryPaginated).pageCache.remove(p) }
    }

    fun openUI(p: Player, inv: UIFactory) {
        BukkitPlugin.plugin.debug("open ui ${inv::class.simpleName} for ${p.name}")
        val ui = inv.getUI(p) ?: let {
            BukkitPlugin.plugin.debug("${inv::class.simpleName} is null for ${p.name}")
            p.sendMessage(Lang["not-in-guild"])
            return
        }
        BukkitPlugin.plugin.debug("Successfully open ${inv::class.simpleName} for ${p.name}")
        this.openUI(p, ui)
    }

    fun openUI(p: Player, inv: Inventory) {
        this.p.schedule { p.openInventory(inv) }
    }


}