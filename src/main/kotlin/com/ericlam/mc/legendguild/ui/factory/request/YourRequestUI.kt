package com.ericlam.mc.legendguild.ui.factory.request

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.translateColorCode
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.UIFactory
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object YourRequestUI : UIFactory {

    val finish: ItemStack = UIManager.p.itemStack(Material.BEACON, display = "宣告為完成")
    val delete: ItemStack = UIManager.p.itemStack(Material.BARRIER, display = "刪除委託任務")


    val checkAdmin: MutableSet<UUID> = mutableSetOf()

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        return invCaches[bPlayer] ?: let {
            UIManager.p.createGUI(1, "&a你的委託內容") {
                mapOf(
                        7 to Clicker(finish) { player, _ ->
                            if (checkAdmin.contains(player.uniqueId)) {
                                BukkitPlugin.plugin.debug("admin check for ${player.name}")
                                isCancelled = true
                                return@Clicker
                            }
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

                            val takerPlayer = Bukkit.getOfflinePlayer(taker)
                            LegendGuild.guildPlayerController.update(taker) {
                                this.contribution += playerRequest.request?.contribute ?: 0
                            }
                            LegendGuild.questPlayerController.update(player.uniqueId) {
                                this.request = null
                            }
                            LegendGuild.questPlayerController.update(taker) {
                                this.job = null
                            }
                            player.tellSuccess()
                            player.closeInventory()
                            takerPlayer?.notify(Lang.Request["request-done"].mFormat(player.name))
                            takerPlayer?.player?.closeInventory()
                            JobInfoUI.cooldown.remove(taker)
                        },
                        8 to Clicker(delete) { player, _ ->
                            if (checkAdmin.contains(player.uniqueId)) {
                                BukkitPlugin.plugin.debug("admin check for ${player.name}")
                                isCancelled = true
                                return@Clicker
                            }
                            val playerRequest = LegendGuild.questPlayerController.findById(player.uniqueId)
                            val request = playerRequest?.request ?: let {
                                player.sendMessage(Lang.Request["request-not-found"])
                                return@Clicker
                            }
                            val taker = request.taken
                            taker?.let {
                                LegendGuild.questPlayerController.update(it) {
                                    this.job = null
                                }
                            }
                            LegendGuild.questPlayerController.update(player.uniqueId) {
                                this.request = null
                            }
                            player.tellSuccess()
                            player.closeInventory()
                            Bukkit.getOfflinePlayer(taker)?.notify(Lang.Request["request-cancel"].mFormat(player.displayName))
                            Bukkit.getOfflinePlayer(taker)?.player?.closeInventory()
                            JobInfoUI.cooldown.remove(taker)
                        }
                )
            }
        }.also {
            updateInfo(bPlayer, it)
            invCaches[bPlayer] = it
        }
    }


    override val invCaches: MutableMap<OfflinePlayer, Inventory> = ConcurrentHashMap()

    override fun updateInfo(player: OfflinePlayer, inventory: Inventory) {
        val item = LegendGuild.questPlayerController.findById(player.uniqueId)?.request ?: return
        val info = UIManager.p.itemStack(Material.PAPER,
                display = "&e委託內容",
                lore = item.goal.map { it.translateColorCode() }
        )

        val owner = UIManager.p.itemStack(materialHead, display = "接手者: ${item.taken?.let { Bukkit.getOfflinePlayer(it) }?.name ?: "沒有人"}")
        inventory.setItem(0, info)
        inventory.setItem(6, owner)
    }
}