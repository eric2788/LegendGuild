package com.ericlam.mc.legendguild.command

import com.ericlam.mc.kotlib.command.BukkitCommand

object GuildCommand : BukkitCommand(
        name = "guild",
        description = "基本指令",
        child = arrayOf(
                BukkitCommand(
                        name = "open",
                        description = "開啟宗門功能介面"
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "top",
                        description = "查看宗門排行榜"
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "check",
                        description = "查看申請列表"
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "pvp",
                        description = "查看宗門戰爭的資訊"
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "create",
                        description = "名稱不能特殊符號",
                        placeholders = arrayOf("宗門名稱")
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "invite",
                        description = "邀請玩家加入你的宗門 限定長老以上的身份者才有資格使用",
                        permission = "guild.invite",
                        placeholders = arrayOf("player")
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "join",
                        description = "開啟申請宗門列表 玩家可以選擇有開放招募玩家狀"
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "kick",
                        description = "踢出指定玩家離開宗門 限定副宗主以上的",
                        placeholders = arrayOf("player"),
                        permission = "guild.kick"
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "leave",
                        description = "離開宗門 或 解散宗門"
                ){ commandSender, strings ->
                    TODO()
                },
                AdminCommand
        )
)