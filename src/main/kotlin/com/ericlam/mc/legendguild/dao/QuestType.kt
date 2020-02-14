package com.ericlam.mc.legendguild.dao

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.legendguild.LegendGuild
import com.ericlam.mc.legendguild.tellSuccess
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.QuestUI
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.Monster
import org.bukkit.inventory.ItemStack

enum class QuestType(val exp: Double,
                     val contribution: Int,
                     val item: ItemStack) {
    EASY(10.0, 100, Item.easy) {
        override fun progress(killed: List<Entity>): Pair<Int, Int> {
            return killed.filterIsInstance<Monster>().size to 100
        }
    },
    NORMAL(30.0, 300, Item.normal) {
        override fun progress(killed: List<Entity>): Pair<Int, Int> {
            return killed.filterIsInstance<Monster>().size to 500
        }
    },
    HARD(60.0, 600, Item.hard) {
        override fun progress(killed: List<Entity>): Pair<Int, Int> {
            return killed.filterIsInstance<Monster>().size to 1000
        }
    },
    NIGHTMARE(120.0, 1200, Item.nightmare) {
        override fun progress(killed: List<Entity>): Pair<Int, Int> {
            return killed.filterIsInstance<Monster>().size to 1500
        }
    };

    open fun goal(killed: List<Entity>): Boolean = progress(killed).first >= progress(killed).second

    abstract fun progress(killed: List<Entity>): Pair<Int, Int>

    val material: Material
        get() = LegendGuild.config.questItems[this] ?: Material.STONE

    val clicker: Clicker by lazy {
        Clicker(item) { player, _ ->
            LegendGuild.questPlayerController.save {
                QuestPlayer(player.uniqueId, QuestPlayer.QuestItem(this@QuestType))
            }
            player.tellSuccess()
            player.closeInventory()
            UIManager.openUI(player, QuestUI)
        }
    }

    private object Item {
        val easy: ItemStack = UIManager.p.itemStack(EASY.material,
                display = "&e簡單難度",
                lore = listOf(
                        "每日殺100隻怪物",
                        "完成獲得10點宗門經驗與100點貢獻"
                ))

        val normal: ItemStack = UIManager.p.itemStack(NORMAL.material,
                display = "&e普通難度",
                lore = listOf(
                        "每日殺500隻怪物",
                        "完成獲得30點宗門經驗與300點貢獻"
                ))

        val hard: ItemStack = UIManager.p.itemStack(HARD.material,
                display = "&e困難難度",
                lore = listOf(
                        "每日殺1000隻怪物",
                        "完成獲得60點宗門經驗與600點貢獻"
                ))

        val nightmare: ItemStack = UIManager.p.itemStack(NIGHTMARE.material,
                display = "&e噩夢難度",
                lore = listOf(
                        "每日殺1500隻怪物",
                        "完成獲得120點宗門經驗與1200點貢獻值"
                ))

    }
}