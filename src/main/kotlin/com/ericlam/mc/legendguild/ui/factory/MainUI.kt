package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.guild.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.UIManager.p
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

object MainUI : UIFactory {

    override val invCaches: MutableMap<OfflinePlayer, Inventory> = ConcurrentHashMap()

    val backMainButton = p.itemStack(Material.ACACIA_DOOR,
            display = "&e返回主選單"
    )

    private val contribute = p.itemStack(
            material = Material.SUGAR,
            display = "&e每日澆水",
            lore = listOf("點擊打開")
    )

    private val postResources = p.itemStack(
            material = Material.EMERALD,
            display = "&e捐贈資源",
            lore = listOf("點擊打開")
    )

    private val quest = p.itemStack(
            material = Material.NAME_TAG,
            display = "&e每日任務",
            lore = listOf("點擊打開")
    )

    private val leave = p.itemStack(
            material = Material.BARRIER,
            display = "&e離開宗門",
            lore = listOf("點擊打開", "若你的身份為宗主，則此宗門會自動解散")
    )

    private val shop = p.itemStack(
            material = Material.BEACON,
            display = "&e宗門商店",
            lore = listOf("點擊打開")
    )

    private val pvp = p.itemStack(
            material = Material.DIAMOND_SWORD,
            display = "&e宗門戰爭資訊"
    )

    private val salaryGet = p.itemStack(
            material = Material.PAPER,
            display = "&e領取薪資",
            lore = listOf("點擊領取")
    )

    private object Admin {
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
    }

    override fun getUI(bPlayer: Player): Inventory? {
        return invCaches[bPlayer] ?: let {
            val player = bPlayer.guildPlayer ?: return null

            return p.createGUI(6, "&e宗門界面",
                    fills = mapOf(
                            (3 row 1)..(3 row 9) to Clicker(p.itemStack(Material.STAINED_GLASS_PANE))
                    )) {
                mutableMapOf(
                        4 row 1 to Clicker(contribute) { player, _ -> UIManager.openUI(player, ContributeUI) },
                        4 row 2 to Clicker(postResources) { player, _ -> UIManager.openUI(player, ResourcesUI) },
                        4 row 3 to Clicker(quest),
                        4 row 4 to Clicker(shop),
                        4 row 5 to Clicker(pvp),
                        4 row 6 to Clicker(leave) { player, _ ->
                            val msg = if (player.leaveGuild()) "success" else "failed"
                            player.sendMessage(LegendGuild.lang[msg])
                        },
                        4 row 7 to Clicker(salaryGet) { player, _ ->
                            val res = GuildManager.sendSalary(player)
                            player.sendMessage(LegendGuild.lang[getSalaryResponse(res)])
                        }
                ).apply {
                    if (player.role hasPower GuildPlayer.Role.ELDER) {
                        this += 4 row 8 to Clicker(Admin.joinerList) { p, _ ->
                            val inv = SettingUI.getGuildGui(p, "joiner")
                                    ?: let {
                                        whoClicked.sendMessage(LegendGuild.lang["not-in-guild"])
                                        return@Clicker
                                    }
                            UIManager.openUI(p, inv)
                        }
                    }
                    if (player.role hasPower GuildPlayer.Role.CO_POPE) {
                        this += 4 row 9 to Clicker(Admin.salarySet) { p, _ ->
                            val inv = SettingUI.getGuildGui(p, "salary")
                                    ?: let {
                                        whoClicked.sendMessage(LegendGuild.lang["not-in-guild"])
                                        return@Clicker
                                    }
                            UIManager.openUI(p, inv)
                        }
                    }
                }
            }.also {
                updateInfo(bPlayer, it)
                invCaches[bPlayer] = it
            }
        }
    }

    private fun getSalaryResponse(res: GuildManager.SalaryResponse): String {
        return when (res) {
            GuildManager.SalaryResponse.FAILED -> "failed"
            GuildManager.SalaryResponse.SUCCESS -> "success"
            GuildManager.SalaryResponse.SUCCESS_NEGATIVE -> "success-negative"
            GuildManager.SalaryResponse.NOT_IN_GUILD -> "not-in-guild"
            GuildManager.SalaryResponse.ALREADY_GET_TODAY -> "did-today"
            GuildManager.SalaryResponse.ROLE_NO_SALARIES -> "role-no-salary"
        }
    }


    override fun updateInfo(player: OfflinePlayer, inventory: Inventory) {
        val guild = player.guild ?: return
        val guildPlayer = player.guildPlayer ?: return
        val describe = p.itemStack(
                material = Material.LEASH,
                display = "&e公會名稱:&c ${guild.name}",
                lore = listOf(
                        "&a你的身份: ${guildPlayer.role.ch}, 貢獻值: ${guildPlayer.contribution}",
                        "&7===================",
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
        mapOf(
                1 row 5 to describe, //info
                2 row 2 to member, //info
                2 row 4 to level, //info
                2 row 6 to money, //info
                2 row 8 to resources //info
        ).forEach { (slot, stack) ->
            inventory.setItem(slot, stack)
        }
    }
}