package com.ericlam.mc.legendguild.command

import com.ericlam.mc.kotlib.command.BukkitCommand
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.JoinUI
import com.ericlam.mc.legendguild.ui.factory.JoinerUI
import com.ericlam.mc.legendguild.ui.factory.LeaderUI
import com.ericlam.mc.legendguild.ui.factory.MainUI

object GuildCommand : BukkitCommand(
        name = "guild",
        description = "基本指令",
        child = arrayOf(
                BukkitCommand(
                        name = "open",
                        description = "開啟宗門功能介面"
                ) { commandSender, _ ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    UIManager.openUI(player, MainUI)
                },
                BukkitCommand(
                        name = "top",
                        description = "查看宗門排行榜"
                ) { commandSender, _ ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    UIManager.openUI(player, LeaderUI)
                },
                BukkitCommand(
                        name = "check",
                        description = "查看申請列表",
                        permission = "guild.pass"
                ) { commandSender, strings ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    UIManager.openUI(player, JoinerUI)
                },
                BukkitCommand(
                        name = "pvp",
                        description = "查看宗門戰爭的資訊",
                        permission = "guild.war.info"
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "create",
                        description = "名稱不能特殊符號",
                        placeholders = arrayOf("宗門名稱"),
                        permission = "guild.create"
                ) { commandSender, strings ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    val res = GuildManager.createGuild(player, strings[0])
                    player.sendMessage(Lang[res.path])
                },
                BukkitCommand(
                        name = "invite",
                        description = "邀請玩家加入你的宗門 限定長老以上的身份者才有資格使用",
                        permission = "guild.invite",
                        placeholders = arrayOf("player")
                ) { commandSender, strings ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    val target = strings[0].toPlayer() ?: run {
                        player.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    val guild = player.guild ?: run {
                        player.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    if (target.guild != null) {
                        player.sendMessage(Lang["joined-guild"].format(target.name))
                        return@BukkitCommand
                    }
                    guild.invites.add(target.uniqueId)
                    target.notify(Lang["invited"].format(guild.name))
                    player.tellSuccess()
                },
                BukkitCommand(
                        name = "join",
                        description = "開啟申請宗門列表 玩家可以選擇有開放招募玩家狀",
                        permission = "guild.join"
                ) { commandSender, strings ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    UIManager.openUI(player, JoinUI)
                },
                BukkitCommand(
                        name = "kick",
                        description = "踢出指定玩家離開宗門 限定副宗主以上的",
                        placeholders = arrayOf("player"),
                        permission = "guild.kick"
                ) { commandSender, strings ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    val target = strings[0].toPlayer() ?: run {
                        player.sendMessage(Lang["player-not-found"])
                        return@BukkitCommand
                    }
                    if (player.guild == null) {
                        player.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    if (target.guild != player.guild) {
                        player.sendMessage(Lang["not-same-guild"])
                        return@BukkitCommand
                    }
                    if (target.guildPlayer?.role?.hasPower(GuildPlayer.Role.CO_POPE) == true) {
                        player.sendMessage(Lang["failed"])
                        return@BukkitCommand
                    }
                    player.sendMessage(Lang[if (target.leaveGuild()) "success" else "failed"])
                },
                BukkitCommand(
                        name = "leave",
                        description = "離開宗門 或 解散宗門"
                ) { commandSender, _ ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    player.sendMessage(Lang[if (player.leaveGuild()) "success" else "not-in-guild"])
                },
                AdminCommand
        )
)