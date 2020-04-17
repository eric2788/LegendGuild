package com.ericlam.mc.legendguild.command

import com.ericlam.mc.kotlib.command.BukkitCommand
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.ui.factory.ShopUI
import com.ericlam.mc.legendguild.ui.factory.request.YourRequestUI

object AdminCommand : BukkitCommand(
        name = "admin",
        description = "管理員指令",
        permission = "guild.admin",
        child = arrayOf(
                BukkitCommand(
                        name = "bank",
                        description = "查看指定玩家所在的宗門商店內容狀況",
                        placeholders = arrayOf("player")
                ) { commandSender, strings ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    val target = strings[0].toPlayer() ?: run {
                        player.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    if (target.guild == null) {
                        player.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    ShopUI.adminOperate[player.uniqueId] = ShopUI.Operation.BANK
                    ShopUI.getUI(target)?.let { player.openInventory(it) } ?: run {
                        player.sendMessage(Lang["failed"])
                    }
                },
                BukkitCommand(
                        name = "setbank",
                        description = "編輯指定玩家所在的宗門商店內容 可以沒收商品 沒收商品會發送通知給上架者沒收訊息"
                ) { commandSender, strings ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    val target = strings[0].toPlayer() ?: run {
                        player.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    if (target.guild == null) {
                        player.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    ShopUI.adminOperate[player.uniqueId] = ShopUI.Operation.SET_BANK
                    ShopUI.getUI(target)?.let { player.openInventory(it) } ?: run {
                        player.sendMessage(Lang["failed"])
                    }
                },
                BukkitCommand(
                        name = "join",
                        description = "將指定玩家強制加入到某個宗門",
                        placeholders = arrayOf("player", "宗門名稱")
                ) { commandSender, strings ->
                    val target = strings[0].toPlayer() ?: run {
                        commandSender.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    val guild = LegendGuild.guildController.findById(strings[1]) ?: run {
                        commandSender.sendMessage(Lang["unknown-guild"])
                        return@BukkitCommand
                    }
                    if (target.guild != null) {
                        commandSender.sendMessage(Lang["joined-guild"].mFormat(target.name))
                        return@BukkitCommand
                    }
                    target.joinGuild(guild.name)
                    commandSender.tellSuccess()
                },
                BukkitCommand(
                        name = "kick",
                        description = "將指定玩家踢出目前所屬的宗門",
                        placeholders = arrayOf("player")
                ) { commandSender, strings ->
                    val target = strings[0].toPlayer() ?: run {
                        commandSender.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    commandSender.sendMessage(Lang[if (target.leaveGuild()) "success" else "failed"])
                    target.notify(Lang["kicked"])
                },
                BukkitCommand(
                        name = "setlv",
                        description = "增加或減少宗門當前的等級",
                        placeholders = arrayOf("player", "(+/-)等級")
                ) { commandSender, strings ->
                    val target = strings[0].toPlayer() ?: run {
                        commandSender.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    val num = strings[1].toIntOrNull() ?: run {
                        commandSender.sendMessage(Lang["not-number"].mFormat(strings[1]))
                        return@BukkitCommand
                    }
                    val set = !(strings[1].startsWith("+") || num < 0)
                    val g = target.guild ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    LegendGuild.guildController.update(g.name) {
                        this level if (set) num - currentLevel else num
                    }?.run {
                        commandSender.tellSuccess()
                    } ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                    }
                },
                BukkitCommand(
                        name = "exp",
                        description = "增加或減少宗門經驗",
                        placeholders = arrayOf("player", "<(+/-)經驗>")
                ) { commandSender, strings ->
                    val target = strings[0].toPlayer() ?: run {
                        commandSender.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    val num = strings[1].toDoubleOrNull() ?: run {
                        commandSender.sendMessage(Lang["not-number"].mFormat(strings[1]))
                        return@BukkitCommand
                    }
                    val set = !(strings[1].startsWith("+") || num < 0)
                    val g = target.guild ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    LegendGuild.guildController.update(g.name) {
                        this exp if (set) num - this.currentExp else num
                    }?.run {
                        commandSender.tellSuccess()
                    } ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                    }
                },
                BukkitCommand(
                        name = "money",
                        description = "給予指定玩家所屬的宗門=>增加或減少宗門資金",
                        placeholders = arrayOf("player", "(+/-)遊戲幣")
                ) { commandSender, strings ->
                    val target = strings[0].toPlayer() ?: run {
                        commandSender.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    val num = strings[1].toDoubleOrNull() ?: run {
                        commandSender.sendMessage(Lang["not-number"].mFormat(strings[1]))
                        return@BukkitCommand
                    }
                    val set = !(strings[1].startsWith("+") || num < 0)
                    val g = target.guild ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    LegendGuild.guildController.update(g.name) {
                        this.resource.money += if (set) num - this.resource.money else num
                    }?.run {
                        commandSender.tellSuccess()
                    } ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                    }
                },
                BukkitCommand(
                        name = "skills",
                        description = "設定指定玩家所屬的宗門指定的技能=>增加或減少等級",
                        placeholders = arrayOf("player", "skills", "(+/-)level")
                ) { commandSender, strings ->
                    val target = strings[0].toPlayer() ?: run {
                        commandSender.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    val skills = GuildSkill.fromName(strings[1]) ?: run {
                        commandSender.sendMessage(Lang["unknown-skill"])
                        return@BukkitCommand
                    }
                    val num = strings[2].toIntOrNull() ?: run {
                        commandSender.sendMessage(Lang["not-number"].mFormat(strings[1]))
                        return@BukkitCommand
                    }
                    val set = !(strings[1].startsWith("+") || num < 0)
                    val g = target.guild ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    LegendGuild.guildController.update(g.name) {
                        this.setSkillLevel(skills, if (set) num - (this.currentSkills[skills] ?: num) else num)
                    }?.run {
                        commandSender.tellSuccess()
                    } ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                    }
                },
                BukkitCommand(
                        name = "gd",
                        description = "給予指定玩家增加或減少貢獻值",
                        placeholders = arrayOf("player", "(+/-)貢獻值")
                ) { commandSender, strings ->
                    val target = strings[0].toPlayer() ?: run {
                        commandSender.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    val num = strings[1].toIntOrNull() ?: run {
                        commandSender.sendMessage(Lang["not-number"].mFormat(strings[1]))
                        return@BukkitCommand
                    }
                    val set = !(strings[1].startsWith("+") || num < 0)
                    val g = target.guild ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    LegendGuild.guildPlayerController.update(target.uniqueId) {
                        this.contribution += if (set) num - this.contribution else num
                    }?.run {
                        commandSender.tellSuccess()
                    } ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                    }

                },
                BukkitCommand(
                        name = "phelp",
                        description = "查看指定玩家所在的宗門委託任務區",
                        placeholders = arrayOf("player")
                ) { commandSender, strings ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    val target = strings[0].toPlayer() ?: run {
                        commandSender.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }

                    YourRequestUI.getUI(target)?.let { player.openInventory(it) } ?: run {
                        player.sendMessage(Lang["failed"])
                    }
                }
        )
)