package com.ericlam.mc.legendguild

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.guild.Guild
import com.ericlam.mc.legendguild.guild.GuildPlayer
import org.bukkit.Material
import org.bukkit.inventory.Inventory

object UIManager {

    private val p: BukkitPlugin
        get() = BukkitPlugin.plugin

    private object SingleItem {
        val backMainButton = p.itemStack(Material.ACACIA_DOOR,
                display = "&e返回主選單"
        )

        val salarySet = p.itemStack(
                material = Material.IRON_INGOT,
                display = "&e薪資設定",
                lore = listOf("點擊打開")
        )

        val joinerList = p.itemStack(
                material = Material.ANVIL,
                display = "&e查看申請者",
                lore = listOf("點擊打開")
        )

        val contribute = p.itemStack(
                material = Material.PAPER,
                display = "&e每日澆水",
                lore = listOf("點擊打開")
        )

        object Contribute {
            val money = p.itemStack(
                    material = Material.GOLD_BLOCK,
                    display = "&e金錢貢獻",
                    lore = with(LegendGuild.config.dailyContribution.money) {
                        listOf(
                                "將花費金錢 $$need",
                                "以獲得 $contribute 貢獻",
                                "及 $exp 經驗值"
                        )
                    }
            )

            val points = p.itemStack(
                    material = Material.PAPER,
                    display = "&b點卷貢獻",
                    lore = with(LegendGuild.config.dailyContribution.points) {
                        listOf(
                                "將花費點卷 $need",
                                "以獲得 $contribute 貢獻",
                                "及 $exp 經驗值"
                        ).map { "&e$it" }
                    }
            )
        }

        val postResources = p.itemStack(
                material = Material.EMERALD,
                display = "&e捐贈資源",
                lore = listOf("點擊打開")
        )

        object Resources {
            val money = p.itemStack(
                    material = Material.GOLD_BLOCK,
                    display = "&6金錢捐贈",
                    lore = with(LegendGuild.config.postResources) {
                        listOf(
                                "將花費 $$money",
                                "來獲得 $money_contribute 貢獻值"
                        ).map { "&e$it" }
                    }
            )

            val item = p.itemStack(
                    material = Material.STONE,
                    display = "&e捐贈道具",
                    lore = with(LegendGuild.config.postResources) {
                        listOf("&e可捐贈道具及貢獻值獎勵如下:") + items.map { "&b${it.key} &7- &6$${it.value}" }
                    }
            )
        }

        val quest = p.itemStack(
                material = Material.NAME_TAG,
                display = "&e每日任務",
                lore = listOf("點擊打開")
        )

        val leave = p.itemStack(
                material = Material.BARRIER,
                display = "&e離開宗門",
                lore = listOf("點擊打開", "若你的身份為宗主，則此宗門會自動解散")
        )

        val shop = p.itemStack(
                material = Material.BEACON,
                display = "&e宗門商店",
                lore = listOf("點擊打開")
        )

        val pvp = p.itemStack(
                material = Material.DIAMOND_SWORD,
                display = "&e宗門戰爭資訊"
        )
    }

    private fun Guild.findRole(role: GuildPlayer.Role): String {
        return this.members.find { it.role == role }?.name ?: "NONE"
    }

    fun mainUI(guild: Guild, player: GuildPlayer): Inventory {
        val describe = p.itemStack(
                material = Material.LEASH,
                display = "&e公會名稱:&c ${guild.name}",
                lore = listOf(
                        "&e宗主:&c ${guild.findRole(GuildPlayer.Role.POPE)}",
                        "&e副宗主:&c ${guild.findRole(GuildPlayer.Role.CO_POPE)}",
                        "&e大長老:&c ${guild.findRole(GuildPlayer.Role.ELDER)}",
                        "&e長老:&c ${guild.findRole(GuildPlayer.Role.CO_ELDER)}",
                        "&b>> 旗下弟子",
                        "&e內門弟子:&f ${guild.members.filter { it.role == GuildPlayer.Role.DISCIPLE }.size} 個",
                        "&e外門弟子:&f ${guild.members.filter { it.role == GuildPlayer.Role.OUT_DISCIPLE }.size} 個"
                )
        )
        val member = p.itemStack(
                material = Material.SKULL,
                display = "&e公會總人數",
                lore = listOf("&7${guild.members.size} 人")
        )

        val level = p.itemStack(
                material = Material.STICK,
                display = "&e公會等級",
                lore = listOf("&7Lv.${guild.currentLevel}(${guild.currentExp}/${guild.maxExp})")
        )

        val money = p.itemStack(
                material = Material.GOLD_INGOT,
                display = "&6公會金錢",
                lore = listOf("&7$${guild.resource.money}")
        )

        val resources = p.itemStack(
                material = Material.DIAMOND,
                display = "&b公會資源",
                lore = guild.resource.items.map { "${LegendGuild.lang["item-translate.${it.key}"]}: ${it.value} 個" }.toList()
        )



        return p.createGUI(6, "&e宗門界面",
                fills = mapOf(
                        (3 row 1)..(3 row 9) to Clicker(p.itemStack(Material.STAINED_GLASS_PANE))
                )) {
            mapOf(
                    1 row 5 to Clicker(describe),
                    2 row 2 to Clicker(member),
                    2 row 4 to Clicker(level),
                    2 row 6 to Clicker(money),
                    2 row 8 to Clicker(resources),
                    4 row 1 to Clicker(SingleItem.contribute) { player, itemStack ->
                        TODO()

                    },
                    4 row 2 to Clicker(SingleItem.postResources) { player, itemStack ->
                        TODO()
                    },
                    4 row 3 to Clicker(SingleItem.quest) { player, itemStack ->
                        TODO()
                    },
                    4 row 4 to Clicker(SingleItem.shop),
                    4 row 5 to Clicker(SingleItem.pvp),
                    4 row 6 to Clicker(SingleItem.leave)
            ).toMutableMap().apply {
                if (player.role hasPower GuildPlayer.Role.ELDER) {
                    this += 4 row 7 to Clicker(SingleItem.joinerList)
                }
                if (player.role hasPower GuildPlayer.Role.CO_POPE) {
                    this += 4 row 8 to Clicker(SingleItem.salarySet)
                }
            }
        }
    }

}