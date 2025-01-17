package com.ericlam.mc.legendguild.command

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.command.BukkitCommand
import com.ericlam.mc.kotlib.msgFormat
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.config.Items
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.dao.QuestPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.*
import com.ericlam.mc.legendguild.ui.factory.request.RequestListUI
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
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
                ) { commandSender, _ ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    UIManager.openUI(player, JoinerUI)
                },
                BukkitCommand(
                        name = "pvp",
                        description = "宗門戰爭接受或拒絕指令",
                        permission = "guild.war.handle",
                        optionalPlaceholders = arrayOf("decline | accept", "guild")
                ) { commandSender, strings ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    val targetGuild = player.guild ?: run {
                        player.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    if (strings.size < 2) {
                        UIManager.openUI(player, PvPUI)
                        return@BukkitCommand
                    }
                    val sentGuild = LegendGuild.guildController.findById(strings[1]) ?: run {
                        player.sendMessage(Lang["unknown-guild"].msgFormat(strings[1]))
                        return@BukkitCommand
                    }

                    if (PvPUI.warList.any { sentGuild.name in listOf(it.g1.name, it.g2.name) }) {
                        player.sendMessage(Lang.PvP["in-war-guild"].mFormat(sentGuild.name))
                        return@BukkitCommand
                    } else if (PvPUI.warList.any { targetGuild.name in listOf(it.g1.name, it.g2.name) }) {
                        player.sendMessage(Lang.PvP["in-war"])
                        return@BukkitCommand
                    }

                    val accept = strings[0].equals("accept", ignoreCase = true)
                    val inv = PvPUI.invites[sentGuild]?.takeIf { inv -> inv.guild.name == targetGuild.name } ?: run {
                        player.sendMessage(Lang.PvP["no-invite"].mFormat(sentGuild.name))
                        return@BukkitCommand
                    }

                    if (accept) {
                        player.sendMessage(Lang.PvP["accepted"].mFormat(sentGuild.name))
                        PvPUI.launchWar(sentGuild, targetGuild, inv.small)
                    } else {
                        player.sendMessage(Lang.PvP["declined"].mFormat(sentGuild.name))
                        sentGuild.members.find { p -> p.role hasPower GuildPlayer.Role.ELDER }?.player?.notify(Lang.PvP["be-declined"].mFormat(targetGuild.name))
                    }
                    PvPUI.invites.remove(sentGuild)
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
                        player.sendMessage(Lang["joined-guild"].mFormat(target.name))
                        return@BukkitCommand
                    }
                    if (guild.invites.add(target.uniqueId)) target.player?.tellInvite()
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
                    target.notify(Lang["kicked"])
                },
                BukkitCommand(
                        name = "leave",
                        description = "離開宗門 或 解散宗門"
                ) { commandSender, _ ->
                    val player = commandSender.toPlayer ?: return@BukkitCommand
                    player.sendMessage(Lang[if (player.leaveGuild()) "success" else "not-in-guild"])
                },
                BukkitCommand(
                        name = "request",
                        description = "創建委託",
                        placeholders = arrayOf("contribute", "goal")
                ) { sender, args ->
                    val player = sender.toPlayer ?: return@BukkitCommand
                    val con = args[0].toIntOrNull() ?: run {
                        player.sendMessage(Lang["not-number"].mFormat(args[0]))
                        return@BukkitCommand
                    }
                    val goal = args.drop(1).toList()
                    if (player.guild == null) {
                        player.sendMessage(Lang["not-in-guild"])
                        return@BukkitCommand
                    }
                    val item = QuestPlayer.RequestItem(goal, con)
                    LegendGuild.questPlayerController.findById(player.uniqueId)?.request?.run {
                        player.sendMessage(Lang.Request["request-exist"])
                        return@BukkitCommand
                    }
                    val b = LegendGuild.questPlayerController.update(player.uniqueId) {
                        this.request = item
                    } == null
                    if (b) LegendGuild.questPlayerController.save { QuestPlayer(player.uniqueId, request = item) }
                    RequestListUI.addPlayer(player)
                    player.tellSuccess()
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
                                    if (player.guild != null) {
                                        player.sendMessage(Lang["in-guild"])
                                        return@BukkitCommand
                                    }
                                    val g = LegendGuild.guildController.findById(arr[0]) ?: run {
                                        player.sendMessage(Lang["unknown-guild"])
                                        return@BukkitCommand
                                    }
                                    g.invites.remove(player.uniqueId).takeIf { true }?.run {
                                        player.joinGuild(arr[0])
                                    } ?: player.sendMessage(Lang["not-invited"])
                                },
                                BukkitCommand(
                                        name = "decline",
                                        description = "拒絕請求",
                                        placeholders = arrayOf("guild")
                                ) { sender, arr ->
                                    val player = sender.toPlayer ?: return@BukkitCommand
                                    if (player.guild != null) {
                                        player.sendMessage(Lang["in-guild"])
                                        return@BukkitCommand
                                    }
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
                                        sender.sendMessage(Lang["not-number"].mFormat(args[1]))
                                        return@BukkitCommand
                                    }
                                    val hand = player.inventory.itemInMainHand
                                    if (hand?.type ?: Material.AIR == Material.AIR) {
                                        sender.sendMessage(Lang["invalid-item"])
                                        return@BukkitCommand
                                    }
                                    fun find(item: ItemStack?): Boolean = item?.let { stack -> NBTItem(stack).getString("guild.sell.name") == args[0] }
                                            ?: false
                                    if (ShopUI.getPaginatedUI(player).any { inv -> inv.any(::find) }) {
                                        sender.sendMessage(Lang.Shop["same-name"])
                                        return@BukkitCommand
                                    }
                                    val nbt = NBTItem(hand)
                                    nbt.setString("guild.sell.name", args[0])
                                    player.addItem(nbt.item, price)
                                },
                                BukkitCommand(
                                        name = "remove",
                                        description = "刪除物品",
                                        permission = "guild.shop.remove",
                                        placeholders = arrayOf("name")
                                ) { sender, args ->
                                    val player = sender.toPlayer ?: return@BukkitCommand
                                    val name = args[0]
                                    fun find(item: ItemStack?): Boolean = item?.let { stack -> NBTItem(stack).getString("guild.sell.name") == name }
                                            ?: false

                                    val item = ShopUI.getPaginatedUI(player).find { inv -> inv.any(::find) }?.find(::find)
                                            ?: run {
                                                BukkitPlugin.plugin.debug("cannot find item $name")
                                                player.sendMessage(Lang.Shop["unknown-item"])
                                                return@BukkitCommand
                                            }
                                    val nbtI = NBTItem(item).getString("guild.shop.seller")
                                    val owner = nbtI?.let {
                                        val uuid = UUID.fromString(it)
                                        uuid == player.uniqueId
                                    } ?: false
                                    if (owner) {
                                        if (player.removeItem(item)) player.tellSuccess() else player.tellFailed()
                                    } else {
                                        player.sendMessage(Lang.Shop["not-owner"])
                                    }
                                }
                        )
                ),
                BukkitCommand(
                        name = "item",
                        description = "物品管理",
                        child = arrayOf(
                                BukkitCommand(
                                        name = "upload",
                                        description = "上傳物品",
                                        placeholders = arrayOf("name"),
                                        permission = "guild.item.upload"
                                ) { sender, args ->
                                    val player = sender.toPlayer ?: return@BukkitCommand
                                    val itemC = LegendGuild.item
                                    val name = args[0]
                                    if (itemC.items.containsKey(name)) {
                                        player.sendMessage(Lang.Shop["same-name"])
                                        return@BukkitCommand
                                    }
                                    itemC.items[name] = Items.ItemWrapper(player.inventory.itemInMainHand.asOne().toBukkitItemStack)
                                    itemC.save()
                                    player.tellSuccess()
                                },
                                BukkitCommand(
                                        name = "remove",
                                        description = "刪除物品",
                                        placeholders = arrayOf("name"),
                                        permission = "guild.item.remove"
                                ) { sender, args ->
                                    val player = sender.toPlayer ?: return@BukkitCommand
                                    val name = args[0]
                                    val itemC = LegendGuild.item
                                    itemC.items.remove(name)?.also {
                                        player.tellSuccess()
                                    } ?: run {
                                        player.sendMessage(Lang.Shop["unknown-item"])
                                    }
                                }
                        )
                ),
                AdminCommand
        )
)