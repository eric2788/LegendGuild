package com.ericlam.mc.legendguild

import com.ericlam.mc.kotlib.KotLib
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.kClassOf
import com.ericlam.mc.legendguild.config.Config
import com.ericlam.mc.legendguild.config.Items
import com.ericlam.mc.legendguild.config.Lang
import com.ericlam.mc.legendguild.guild.Guild
import com.ericlam.mc.legendguild.guild.GuildController
import com.ericlam.mc.legendguild.guild.GuildPlayer
import com.ericlam.mc.legendguild.guild.GuildPlayerController
import net.milkbowl.vault.economy.Economy
import org.black_ixx.playerpoints.PlayerPoints
import org.black_ixx.playerpoints.PlayerPointsAPI


class LegendGuild : BukkitPlugin() {


    companion object {
        private lateinit var _config: Config
        private lateinit var _lang: Lang
        private lateinit var _item: Items
        private lateinit var _econmony: Economy
        private lateinit var _gcontroller: GuildController
        private lateinit var _gpcontroller: GuildPlayerController
        private lateinit var _pointsApi: PlayerPointsAPI
        val config: Config
                get() = _config
        val lang: Lang
                get() = _lang
        val item: Items
                get() = _item
        val economy: Economy
                get() = _econmony
        val guildController: GuildController
                get() = _gcontroller
        val guildPlayerController: GuildPlayerController
                get() = _gpcontroller
        val pointsAPI: PlayerPointsAPI
                get() = _pointsApi
    }

    override fun enable() {
        val manager = KotLib.getConfigFactory(this)
                .register(Config::class).register(Items::class).register(Lang::class)
                .registerDao(kClassOf(), GuildPlayerController::class)
                .registerDao(kClassOf(), GuildPlayerController::class)
                .dump()
        _config = manager.getConfig(kClassOf())
        _lang = manager.getConfig(kClassOf())
        _item = manager.getConfig(kClassOf())
        _gcontroller = manager.getDao(kClassOf())
        _gpcontroller = manager.getDao(kClassOf())
        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        _econmony = rsp.provider
        _pointsApi = getPlugin(PlayerPoints::class.java).api
    }


}