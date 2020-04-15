package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
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
import kotlin.text.format

object PvPUI : UIFactoryPaginated {

    private val inventories: MutableList<Inventory> = mutableListOf()

    val invites: MutableMap<Guild, Invite> = mutableMapOf()

    data class Invite(val guild: Guild, val small: Boolean)

    data class War(val g1: Guild, val g2: Guild, var g1Score: Int = 0, var g2Score: Int = 0, val target: Int, val bossBar: BossBar)

    val warList = LinkedList<War>()

    init {
        UIManager.p.schedule(period = 30, unit = TimeUnit.MINUTES) { updateInv() }
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
                war.bossBar.title = "§b§l${war.g1.name} §r§b${war.g1Score} §7- §c${war.g2Score} §c§l${war.g1.name}"
            }
        }

        UIManager.p.listen<PlayerJoinEvent> {
            warList.find { war -> it.player.guild == war.g1 || it.player.guild == war.g2 }?.bossBar?.addPlayer(it.player)
        }
    }

    fun launchWar(sent: Guild, accept: Guild, small: Boolean) {
        val target = if (small) 25 else 50
        val bar = Bukkit.createBossBar("???", BarColor.PINK, BarStyle.SOLID)
        bar.title = "§b§l${sent.name} §r§b0 §7- §c0 §c§l${accept.name}"
        val war = War(sent, accept, target = target, bossBar = bar)
        warList.add(war)
        (sent.members + accept.members).map { it.player }.forEach {
            it.notify(Lang.PvP["war-start"].format(target))
            it.player?.let { p -> bar.addPlayer(p) }
        }
        UIManager.p.schedule(delay = 1, unit = TimeUnit.DAYS) {
            warList.remove(war)
            drawWar(war)
        }
    }

    private fun endWar(winner: Guild, loser: Guild, war: War) {
        val exp = JavaScript.eval(LegendGuild.config.lossExp.trim()) as Double
        winner exp +exp
        loser exp -exp
        war.bossBar.removeAll()
        (winner.members + loser.members).map { it.player }.forEach { it.notify(Lang.PvP["war-end"].format(winner.name)) }
    }

    private fun drawWar(war: War) {
        war.bossBar.removeAll()
        (war.g1.members + war.g2.members).map { it.player }.forEach { it.notify(Lang.PvP["war-draw"]) }
    }

    private fun updateInv(): List<Inventory> {
        inventories.clear()
        var inv = createPage()
        inventories.add(inv)
        GuildManager.guildMap.forEach { g ->
            if (inv.firstEmpty() == -1) {
                inv = createPage()
                inventories.add(inv)
            }
            val lore = listOf(
                    "&e等級: Lv${g.currentLevel}",
                    "&e人數: ${g.members.size}",
                    "&a左鍵大型右鍵小型"
            )
            BukkitPlugin.plugin.debug("updating ${this::class} info for ${g.name}")
            val item = UIManager.p.itemStack(Material.PAPER, display = "&b${g.name}", lore = lore)
            val nbItem = NBTItem(item)
            nbItem.setString("guild.name", g.name)
            inv.addItem(nbItem.item)
        }
        return inventories
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
                        (6 row 2)..(6 row 8) to Clicker(ItemStack(Material.AIR)) { p, stack ->
                            val sentGuild = p.guild ?: let {
                                p.sendMessage(Lang["not-in-guild"])
                                return@Clicker
                            }
                            val name = NBTItem(stack).getString("guild.name") ?: return@Clicker
                            val small = when (click) {
                                ClickType.RIGHT, ClickType.SHIFT_RIGHT -> true
                                ClickType.LEFT, ClickType.SHIFT_LEFT -> false
                                else -> return@Clicker
                            }

                            val targetGuild = LegendGuild.guildController.findById(name) ?: let {
                                p.sendMessage(Lang["unknown-guild"].format(name))
                                return@Clicker
                            }

                            invites[sentGuild] = Invite(targetGuild, small)
                            p.sendMessage(Lang.PvP["invite-sent"])
                            targetGuild.members.filter { it.role.hasPower(GuildPlayer.Role.CO_ELDER) }.map { it.player }.forEach { off ->
                                off.notify(Lang.PvP["get-invite"].format(sentGuild.name))
                            }
                        }
                )
        ) {
            pageOperator
        }
    }
}