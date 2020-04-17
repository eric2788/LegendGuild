package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.msgFormat
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.TimeUnit

object PvPUI : UIFactoryPaginated {

    private val inventories: MutableList<Inventory> = mutableListOf()

    val invites: MutableMap<Guild, Invite> = mutableMapOf()

    data class Invite(val guild: Guild, val small: Boolean)

    data class War(val g1: Guild, val g2: Guild, var g1Score: Int = 0, var g2Score: Int = 0, val target: Int, val bossBar: BossBar)

    val warList = LinkedList<War>()

    init {
        UIManager.p.schedule(delay = 1, period = 30, unit = TimeUnit.MINUTES) { updateInv() }
        UIManager.p.listen<PlayerDeathEvent> {
            val victim = it.entity
            val killer = (it.entity.lastDamageCause as? EntityDamageByEntityEvent)?.damager?.playerKiller
                    ?: return@listen
            val war = warList.find { war -> (killer.guild == war.g1 || killer.guild == war.g2) && (victim.guild == war.g1 || victim.guild == war.g2) }
                    ?: return@listen
            if (victim.guild == war.g1 && killer.guild == war.g2) {
                war.g2Score++
            } else if (victim.guild == war.g2 && killer.guild == war.g1) {
                war.g1Score++
            }

            if (war.g2Score >= war.target) {
                endWar(war.g2, war.g1, war)
            } else if (war.g1Score >= war.target) {
                endWar(war.g1, war.g2, war)
            } else {
                war.bossBar.title = "§b§l${war.g1.name} §r§b${war.g1Score} §7- §6§l${war.target}§r §7- §c${war.g2Score} §c§l${war.g1.name}"
            }
        }

        UIManager.p.listen<PlayerJoinEvent> {
            warList.find { war -> it.player.guild == war.g1 || it.player.guild == war.g2 }?.bossBar?.addPlayer(it.player)
        }
    }

    fun launchWar(sent: Guild, accept: Guild, small: Boolean, force: Boolean = false) {
        val target = if (small) LegendGuild.config.war.small else LegendGuild.config.war.big
        val bar = Bukkit.createBossBar("???", BarColor.PINK, BarStyle.SOLID)
        bar.title = "§b§l${sent.name} §r§b0 §7- §6§l$target§r §7- §c0 §c§l${accept.name}"
        val war = War(sent, accept, target = target, bossBar = bar)
        warList.add(war)
        (sent.members + accept.members).map { it.player }.forEach {
            it.notify(Lang.PvP["war-start"].msgFormat(target))
            if (force) it.notify(Lang.PvP["force-launched"].msgFormat(sent.name))
            it.player?.let { p -> bar.addPlayer(p) }
        }
        UIManager.p.schedule(delay = 1, unit = TimeUnit.DAYS) {
            warList.remove(war)
            drawWar(war)
        }
    }

    private fun endWar(winner: Guild, loser: Guild, war: War) {
        fun calculate(g: Guild): Double = (JavaScript.eval(LegendGuild.config.lossExp.replace("%level%", g.currentExp.toString()).trim()) as Number).toDouble()
        val winExp = calculate(loser)
        val loseExp = calculate(winner)
        winner exp +winExp
        loser exp -loseExp
        war.bossBar.removeAll()
        (winner.members + loser.members).map { it.player }.forEach { it.notify(Lang.PvP["war-end"].msgFormat(winner.name)) }
        LegendGuild.guildController.save { winner }
        LegendGuild.guildController.save { loser }
    }

    private fun drawWar(war: War) {
        war.bossBar.removeAll()
        (war.g1.members + war.g2.members).map { it.player }.forEach { it.notify(Lang.PvP["war-draw"]) }
    }

    private fun updateInv(): List<Inventory> {
        inventories.clear()
        val inv = createPage()
        inventories.add(inv)
        BukkitPlugin.plugin.debug("updating ${this::class} info")
        GuildManager.guildMap.forEach { g -> addGuild(g) }
        return inventories
    }

    fun addGuild(g: Guild) {
        var inv = inventories.lastOrNull() ?: let {
            BukkitPlugin.plugin.debug("pvp inventory is empty, creating new one now")
            inventories.add(createPage())
            return addGuild(g)
        }
        if (inv.firstEmpty() == -1) {
            inv = createPage()
            inventories.add(inv)
        }
        val lore = listOf(
                "&e等級: Lv${g.currentLevel}",
                "&e人數: ${g.members.size}",
                "&a左鍵大型右鍵小型"
        )
        BukkitPlugin.plugin.debug("preparing to add ${g.name} to pvp ui")
        val item = UIManager.p.itemStack(Material.PAPER, display = "&b${g.name}", lore = lore)
        val nbItem = NBTItem(item)
        nbItem.setString("guild.name", g.name)
        inv.addItem(nbItem.item)

    }

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        if (inventories.isEmpty()) updateInv()
        BukkitPlugin.plugin.debug("pvp inventory list current size: ${inventories.size}")
        return inventories
    }

    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = mutableMapOf()

    override fun createPage(): Inventory {
        BukkitPlugin.plugin.debug("Creating new page of ${this::class.simpleName}")
        pageCache.clear()
        BukkitPlugin.plugin.debug("${this::class.simpleName} new page, so clear pageCache")
        return UIManager.p.createGUI(6, "&b宗門戰爭",
                fills = mapOf(
                        0..53 to Clicker(ItemStack(Material.AIR)) { p, stack ->
                            val sentGuild = p.guild ?: let {
                                p.sendMessage(Lang["not-in-guild"])
                                return@Clicker
                            }
                            if (warList.any { w -> sentGuild.name in listOf(w.g1.name, w.g2.name) }) {
                                p.sendMessage(Lang.PvP["in-war"])
                                return@Clicker
                            }
                            val name = NBTItem(stack).getString("guild.name") ?: kotlin.run {
                                BukkitPlugin.plugin.debug("unknown guild name, skipped pvp")
                                return@Clicker
                            }
                            val small = when (click) {
                                ClickType.RIGHT, ClickType.SHIFT_RIGHT -> true
                                ClickType.LEFT, ClickType.SHIFT_LEFT -> false
                                else -> {
                                    BukkitPlugin.plugin.debug("unknown click type, skipped pvp")
                                    return@Clicker
                                }
                            }

                            val targetGuild = LegendGuild.guildController.findById(name) ?: let {
                                p.sendMessage(Lang["unknown-guild"].msgFormat(name))
                                return@Clicker
                            }

                            if (sentGuild.name == targetGuild.name) {
                                p.sendMessage(Lang.PvP["pvp-self"])
                                return@Clicker
                            }

                            if (click.isShiftClick) {
                                if (LegendGuild.pointsAPI.take(p.uniqueId, LegendGuild.config.war.forcePoints)) {
                                    launchWar(sentGuild, targetGuild, small, true)
                                }
                            } else {
                                p.sendMessage(Lang.PvP["invite-sent"])
                                if (!invites.containsKey(sentGuild)) {
                                    invites[sentGuild] = Invite(targetGuild, small)
                                    targetGuild.members.filter { g -> g.role hasPower GuildPlayer.Role.CO_ELDER }.map { it.player }.forEach {
                                        it.player?.tellPvPInvite()
                                    }
                                }
                            }
                        },
                        (6 row 2)..(6 row 8) to Clicker(UIFactoryPaginated.decorate)
                )
        ) {
            pageOperator
        }
    }
}