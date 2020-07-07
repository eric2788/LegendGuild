package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.QuestPlayer
import com.ericlam.mc.legendguild.dao.QuestType
import com.ericlam.mc.legendguild.ui.UIManager
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

object QuestUI : UIFactory {

    private val selectUI: Inventory = UIManager.p.createGUI(1, "&c選擇你的任務") {
        mapOf(
                1 to QuestType.EASY.clicker,
                3 to QuestType.NORMAL.clicker,
                5 to QuestType.HARD.clicker,
                7 to QuestType.NIGHTMARE.clicker
        )
    }

    private val cancel: ItemStack = UIManager.p.itemStack(Material.BARRIER, display = "&c取消目前任務")
    private val tryFinish: ItemStack = UIManager.p.itemStack(Material.EMERALD_BLOCK, display = "&e點擊完成", lore = listOf("點擊以完成任務"))

    override val invCaches: MutableMap<OfflinePlayer, Inventory> = ConcurrentHashMap()

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        val questPlayer = LegendGuild.questPlayerController.findById(bPlayer.uniqueId)
        val quest = questPlayer?.item ?: let {
            LegendGuild.debug("quest item for ${bPlayer.name} is null, removing cache")
            invCaches.remove(bPlayer)
            return selectUI
        }
        return invCaches[bPlayer] ?: let {
            UIManager.p.createGUI(1, "目前任務資訊") {
                mapOf(
                        0 to Clicker(quest.questType.item()),
                        6 to Clicker(tryFinish) { player, _ ->
                            LegendGuild.questPlayerController.update(bPlayer.uniqueId) {
                                val result = tryFinish()
                                player.sendMessage(Lang[result.path])
                                if (result in listOf(QuestPlayer.QuestResult.SUCCESS_AND_REWARDED, QuestPlayer.QuestResult.DEADLINED)) {
                                    player.closeInventory()
                                    questPlayer.item = null
                                }
                            }

                        },
                        7 to Clicker(cancel) { player, _ ->
                            val b = LegendGuild.questPlayerController.update(bPlayer.uniqueId) {
                                item = null
                            } != null
                            if (b) player.tellSuccess() else player.tellFailed()
                            player.closeInventory()
                        },
                        8 to MainUI.backMainButton
                )
            }
        }.also {
            updateInfo(bPlayer, it)
            invCaches[bPlayer] = it
        }
    }

    override fun updateInfo(player: OfflinePlayer, inventory: Inventory) {
        val quest = LegendGuild.questPlayerController.findById(player.uniqueId)?.item ?: let {
            LegendGuild.debug("quest item for ${player.name} is null, removing cache")
            invCaches.remove(player)
            return
        }
        val progress: ItemStack = UIManager.p.itemStack(Material.PAPER,
                display = "&e目前進度",
                lore = listOf(
                        "&e擊殺怪物:&f ${quest.progress.first}/${quest.progress.second}",
                        "&b還有 ${(quest.progress.second - quest.progress.first).coerceAtLeast(0)} 隻怪物需要擊殺。",
                        "&7任務完成: ${if (quest.matchGoal) "&a是" else "&c否"}"
                ))
        LegendGuild.debug("updating ${this::class} info for ${player.name}")
        inventory.setItem(1, progress)
    }
}