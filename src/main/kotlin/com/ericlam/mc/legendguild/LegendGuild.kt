package com.ericlam.mc.legendguild

import com.ericlam.mc.kotlib.KotLib
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.kClassOf
import com.ericlam.mc.legendguild.config.Config
import com.ericlam.mc.legendguild.config.Items
import com.ericlam.mc.legendguild.config.Lang
import com.ericlam.mc.legendguild.guild.GuildController
import com.ericlam.mc.legendguild.guild.GuildPlayerController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.milkbowl.vault.economy.Economy
import org.black_ixx.playerpoints.PlayerPoints
import org.black_ixx.playerpoints.PlayerPointsAPI
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import java.text.MessageFormat


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

        registerListeners()
    }


    private fun registerListeners() {
        listen<EntityDamageByEntityEvent> {
            val killer = when (it.entity) {
                is Player -> it.entity as Player
                is Projectile -> (it.entity as Projectile).shooter as? Player ?: return@listen
                is TNTPrimed -> (it.entity as TNTPrimed).source as? Player ?: return@listen
                else -> return@listen
            }
            val guild = killer.guild ?: return@listen
            val damagePlus = guild.percentage(GuildSkill.AZURE_DRAGON)
            it.damage += it.damage * damagePlus
            val critical = guild.percentage(GuildSkill.VERMILION_BIRD) * 100
            if ((0..100).random() < critical) {
                val extra = it.damage * guild.percentage(GuildSkill.WHITE_TIGER)
                killer.sendMessage(MessageFormat.format(lang["damage-critical"], extra))
                it.damage += extra
            }
        }

        listen<EntityDamageEvent> {
            val player = it.entity as? Player ?: return@listen
            val guild = player.guild ?: return@listen
            it.damage -= it.damage * guild.percentage(GuildSkill.BLACK_TORTOISE)
        }

        listen<PlayerJoinEvent> {
            if (skinCache.contains(it.player.uniqueId)) return@listen
            GlobalScope.launch {
                val value = it.player.toSkinValue()
                it.player.guildPlayer?.skinValue = value
                skinCache[it.player.uniqueId] = value
            }
        }
    }


}