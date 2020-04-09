package com.ericlam.mc.legendguild.command

import com.ericlam.mc.kotlib.command.BukkitCommand
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.ui.factory.ShopUI
import com.ericlam.mc.legendguild.ui.factory.request.RequestListUI
import de.tr7zw.nbtapi.NBTEntity

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
                    val nbtPlayer = NBTEntity(player)
                    nbtPlayer.setString("guild.admin.operate", "shop.check")
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
                    val nbtPlayer = NBTEntity(player)
                    nbtPlayer.setString("guild.admin.operate", "shop.set")
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
                        commandSender.sendMessage(Lang["joined-guild"].format(target))
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
                        commandSender.sendMessage(Lang["not-number"].format(strings[1]))
                        return@BukkitCommand
                    }
                    target.guild?.level(num) ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    commandSender.tellSuccess()
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
                        commandSender.sendMessage(Lang["not-number"].format(strings[1]))
                        return@BukkitCommand
                    }
                    target.guild?.exp(num) ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    commandSender.tellSuccess()
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
                        commandSender.sendMessage(Lang["not-number"].format(strings[1]))
                        return@BukkitCommand
                    }
                    val res = target.guild?.resource ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    res.money += num
                    commandSender.tellSuccess()
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
                        commandSender.sendMessage(Lang["not-number"].format(strings[1]))
                        return@BukkitCommand
                    }
                    target.guild?.setSkillLevel(skills, num)
                    commandSender.tellSuccess()
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
                        commandSender.sendMessage(Lang["not-number"].format(strings[1]))
                        return@BukkitCommand
                    }
                    LegendGuild.guildPlayerController.update(target.uniqueId) {
                        this.contribution += num
                    } ?: run {
                        commandSender.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    commandSender.tellSuccess()
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
                    RequestListUI.getUI(target)?.let { player.openInventory(it) } ?: run {
                        player.sendMessage(Lang["failed"])
                    }
                }
        )
)