package com.ericlam.mc.legendguild.command

import com.ericlam.mc.kotlib.command.BukkitCommand

object AdminCommand : BukkitCommand(
        name = "admin",
        description = "管理員指令",
        permission = "guild.admin",
        child = arrayOf(
                BukkitCommand(
                        name = "bank",
                        description = "查看指定玩家所在的宗門商店內容狀況"
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "setbank",
                        description = "編輯指定玩家所在的宗門商店內容 可以沒收商品 沒收商品會發送通知給上架者沒收訊息"
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "join",
                        description = "將指定玩家強制加入到某個宗門",
                        placeholders = arrayOf("player", "宗門名稱")
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "kick",
                        description = "將指定玩家踢出目前所屬的宗門",
                        placeholders = arrayOf("player")
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "setlv",
                        description = "增加或減少宗門當前的等級",
                        placeholders = arrayOf("player", "(+/-)等級")
                ){ commandSender, strings ->

                },
                BukkitCommand(
                        name = "exp",
                        description = "增加或減少宗門經驗",
                        placeholders = arrayOf("player", "<(+/-)經驗>")
                ){commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "money",
                        description = "給予指定玩家所屬的宗門=>增加或減少宗門資金",
                        placeholders = arrayOf("player", "(+/-)遊戲幣")
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "skills",
                        description = "設定指定玩家所屬的宗門指定的技能=>增加或減少等級",
                        placeholders = arrayOf("player", "skills", "(+/-)level")
                ){commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "gd",
                        description = "給予指定玩家增加或減少貢獻值",
                        placeholders = arrayOf("player", "(+/-)貢獻值")
                ){ commandSender, strings ->
                    TODO()
                },
                BukkitCommand(
                        name = "phelp",
                        description = "查看指定玩家所在的宗門委託任務區",
                        placeholders = arrayOf("player")
                ){commandSender, strings ->
                    TODO()
                }
        )
)