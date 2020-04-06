package com.ericlam.mc.legendguild.command

import com.ericlam.mc.kotlib.command.BukkitCommand
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.*
import de.tr7zw.nbtapi.NBTItem
import java.util.*

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
                        description = "宗門戰爭接受或拒絕指令",
                        permission = "guild.war.handle",
                        optionalPlaceholders = arrayOf("decline | accept")
                ) { commandSender, strings ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    val guild = player.guild ?: run {
                        player.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    if (strings.isEmpty()) {
                        UIManager.openUI(player, PvPUI)
                        return@BukkitCommand
                    }
                    val accept = strings[0].equals("accept", ignoreCase = true)
                    val inv = player.guild?.let { PvPUI.invites[it] } ?: run {
                        player.sendMessage(Lang.PvP["no-invite"])
                        return@BukkitCommand
                    }
                    if (accept) {
                        player.sendMessage(Lang.PvP["accepted"].format(inv.guild.name))
                        PvPUI.launchWar(inv.guild, guild, inv.small)
                    } else {
                        player.sendMessage(Lang.PvP["declined"].format(inv.guild.name))
                        inv.guild.members.find { p -> p.role.hasPower(GuildPlayer.Role.ELDER) }?.player?.notify(Lang.PvP["be-declined"].format(guild.name))
                    }
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
                    target.player?.tellInvite()
                    player.tellSuccess()
                },
                BukkitCommand(
                        name = "join",
                        description = "開啟申請宗門列表 玩家可以選擇有開放招募玩家狀",
                        permission = "guild.join"
                ) { commandSender, _ ->
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
                BukkitCommand(
                        name = "response",
                        description = "回應邀請",
                        child = arrayOf(
                                BukkitCommand(
                                        name = "accept",
                                        description = "答應請求",
                                        placeholders = arrayOf("guild")
                                ) { sender, arr ->
                                    val player = sender.toPlayer ?: return@BukkitCommand
                                    val g = LegendGuild.guildController.findById(arr[0]) ?: run {
                                        player.sendMessage(Lang["unknown-guild"])
                                        return@BukkitCommand
                                    }
                                    g.invites.remove(player.uniqueId).takeIf { true }?.run {
                                        player.joinGuild(arr[0])
                                        player.tellSuccess()
                                    } ?: player.sendMessage(Lang["not-invited"])
                                },
                                BukkitCommand(
                                        name = "decline",
                                        description = "拒絕請求",
                                        placeholders = arrayOf("guild")
                                ) { sender, arr ->
                                    val player = sender.toPlayer ?: return@BukkitCommand
                                    val g = LegendGuild.guildController.findById(arr[0]) ?: run {
                                        player.sendMessage(Lang["unknown-guild"])
                                        return@BukkitCommand
                                    }
                                    g.invites.remove(player.uniqueId).takeIf { true }?.run {
                                        player.tellSuccess()
                                    } ?: player.sendMessage(Lang["not-invited"])
                                }
                        )
                ),
                BukkitCommand(
                        name = "shop",
                        description = "宗門商店指令",
                        child = arrayOf(
                                BukkitCommand(
                                        name = "upload",
                                        description = "售賣物品",
                                        permission = "guild.shop.sell",
                                        placeholders = arrayOf("item", "price")
                                ) { sender, args ->
                                    val player = sender.toPlayer ?: return@BukkitCommand
                                    val price = args[1].toIntOrNull() ?: run {
                                        sender.sendMessage(Lang["not-number"].format(args[1]))
                                        return@BukkitCommand
                                    }
                                    val itemC = LegendGuild.item
                                    if (itemC.items.containsKey(args[0])) {
                                        sender.sendMessage(Lang.Shop["same-name"])
                                        return@BukkitCommand
                                    }
                                    val item = player.inventory.itemInMainHand
                                    player.addItem(item, price)
                                    itemC.items[args[0]] = item
                                    itemC.save()
                                },
                                BukkitCommand(
                                        name = "remove",
                                        description = "刪除物品",
                                        permission = "guild.shop.remove",
                                        placeholders = arrayOf("name")
                                ) { sender, args ->
                                    val player = sender.toPlayer ?: return@BukkitCommand
                                    val name = args[0]
                                    val itemC = LegendGuild.item
                                    val item = itemC.items[name] ?: run {
                                        sender.sendMessage(Lang.Shop["unknown-item"])
                                        return@BukkitCommand
                                    }
                                    val nbtI = NBTItem(item).getString("guild.shop.seller")
                                    val owner = nbtI?.let {
                                        val uuid = UUID.fromString(it)
                                        uuid == player.uniqueId
                                    } ?: false
                                    if (owner) {
                                        if (player.removeItem(item)) player.tellSuccess().also {
                                            itemC.items.remove(name)
                                            itemC.save()
                                        } else player.tellFailed()
                                    } else {
                                        player.sendMessage(Lang.Shop["not-owner"])
                                    }
                                }
                        )
                ),
                AdminCommand
        )
)