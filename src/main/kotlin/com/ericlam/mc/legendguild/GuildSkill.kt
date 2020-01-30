package com.ericlam.mc.legendguild

enum class GuildSkill(val ch: String) {
    AZURE_DRAGON("青龍之詩"),
    BLACK_TORTOISE("玄武之詩"),
    VERMILION_BIRD("朱雀之詩"),
    WHITE_TIGER("白虎之詩");

    companion object Factory{
        fun fromName(ch: String): GuildSkill?{
            return values().find { it.ch == ch || it.name == ch }
        }
    }
}