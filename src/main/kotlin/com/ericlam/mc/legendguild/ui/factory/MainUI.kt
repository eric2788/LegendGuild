package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.UIManager.p
import com.ericlam.mc.legendguild.ui.factory.request.RequestUI
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

object MainUI : UIFactory {

    override val invCaches: MutableMap<OfflinePlayer, Inventory> = ConcurrentHashMap()

    val backMainButton = Clicker(p.itemStack(Material.ACACIA_DOOR, display = "&e返回主選單")) { p, _ ->
        p.closeInventory()
        UIManager.openUI(p, MainUI)
    }

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
            display = "&e宗門戰爭"
    )

    private val salaryGet = p.itemStack(
            material = Material.PAPER,
            display = "&e領取薪資",
            lore = listOf("點擊領取")
    )

    private val request = makeHead(display = "&a委託與接單", lore = listOf("點擊打開"))

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

        val promote = makeHead(display = "&e晉升宗門成員", lore = listOf("點擊打開"))

        val publicStatus = p.itemStack(
                material = Material.EMERALD_BLOCK,
                display = "&e公會招募狀態",
                lore = listOf("&a開放所有人申請", "點擊設置")
        )

        val privateStatus = p.itemStack(
                material = Material.REDSTONE_BLOCK,
                display = "&e公會招募狀態",
                lore = listOf("&c僅限被邀請", "點擊設置")
        )

        val upgradeSkill = p.itemStack(
                material = Material.BOOK,
                display = "&a技能升級",
                lore = listOf("點擊打開")
        )
    }

    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        debugDetails()
        return invCaches[bPlayer] ?: let {
            val player = bPlayer.guildPlayer ?: return null

            LegendGuild.debug("${bPlayer.name} guild player is exist = { $player }")

            return p.createGUI(6, "&e宗門界面",
                    fills = mapOf(
                            (3 row 1)..(3 row 9) to Clicker(p.itemStack(materialGlassPane))
                    )) {
                mutableMapOf(
                        4 row 1 to Clicker(contribute) { player, _ -> UIManager.openUI(player, ContributeUI) },
                        4 row 2 to Clicker(postResources) { player, _ -> UIManager.openUI(player, ResourcesUI) },
                        4 row 3 to Clicker(quest) { player, _ ->
                            UIManager.openUI(player, QuestUI)
                        },
                        4 row 4 to Clicker(shop) { player, _ ->
                            UIManager.openUI(player, ShopUI)
                        },
                        4 row 5 to Clicker(leave) { player, _ ->
                            val msg = if (player.leaveGuild()) "success" else "failed"
                            player.sendMessage(Lang[msg])
                        },
                        4 row 6 to Clicker(salaryGet) { player, _ ->
                            val res = GuildManager.sendSalary(player)
                            player.sendMessage(Lang[res.path])
                        },
                        4 row 7 to Clicker(request) { p, _ ->
                            UIManager.openUI(p, RequestUI)
                        }
                ).apply {
                    if (player.role hasPower GuildPlayer.Role.ELDER) {
                        this += 4 row 8 to Clicker(Admin.joinerList) { p, _ ->
                            UIManager.openUI(p, JoinerUI)
                        }
                        this += 4 row 9 to Clicker(Admin.promote) { p, _ ->
                            UIManager.openUI(p, PromoteUI)
                        }
                        this += 5 row 1 to Clicker(pvp) { p, _ ->
                            UIManager.openUI(p, PvPUI)
                        }
                        this += 5 row 2 to Clicker(Admin.publicStatus) { player, _ ->
                            val guild = player.guild ?: let {
                                player.sendMessage(Lang["not-in-guild"])
                                return@Clicker
                            }
                            guild.public = !guild.public
                            player.tellSuccess()
                            clickedInventory?.setItem(5 row 2, if (guild.public) Admin.publicStatus else Admin.privateStatus)
                        }
                    }
                    if (player.role hasPower GuildPlayer.Role.CO_POPE) {
                        this += 5 row 3 to Clicker(Admin.salarySet) { p, _ ->
                            UIManager.openUI(p, SalaryUI)
                        }
                        this += 5 row 4 to Clicker(Admin.upgradeSkill) { p, _ ->
                            UIManager.openUI(p, SkillUI)
                        }
                    }
                }
            }.also {
                updateInfo(bPlayer, it)
                invCaches[bPlayer] = it
            }
        }
    }

    private val GuildManager.SalaryResponse.path: String
        get() {
            return when (this) {
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
                material = materialLeash,
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
        val member = makeHead(
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
                lore = guild.resource.items.map { "${Lang.Item[it.key]}: ${it.value} 個" }.toList()
        )
        LegendGuild.debug("updating ${this::class} info for ${guild.name}")
        mutableMapOf(
                1 row 5 to describe, //info
                2 row 2 to member, //info
                2 row 4 to level, //info
                2 row 6 to money, //info
                2 row 8 to resources //info
        ).apply {
            if (player.guildPlayer?.role?.hasPower(GuildPlayer.Role.ELDER) == true) {
                this += 5 row 2 to if (guild.public) Admin.publicStatus else Admin.privateStatus
            }
        }.forEach { (slot, stack) ->
            inventory.setItem(slot, stack)
        }
    }
}