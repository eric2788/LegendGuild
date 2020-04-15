package com.ericlam.mc.legendguild

import com.ericlam.mc.kotlib.KotLib
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.kClassOf
import com.ericlam.mc.legendguild.command.GuildCommand
import com.ericlam.mc.legendguild.config.Config
import com.ericlam.mc.legendguild.config.Items
import com.ericlam.mc.legendguild.config.Lang
import com.ericlam.mc.legendguild.config.Perms
import com.ericlam.mc.legendguild.dao.GuildController
import com.ericlam.mc.legendguild.dao.GuildPlayerController
import com.ericlam.mc.legendguild.dao.GuildShopItemController
import com.ericlam.mc.legendguild.dao.QuestPlayerController
import com.ericlam.mc.legendguild.ui.factory.ShopUI
import com.ericlam.mc.legendguild.ui.factory.request.YourRequestUI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.milkbowl.vault.economy.Economy
import org.black_ixx.playerpoints.PlayerPoints
import org.black_ixx.playerpoints.PlayerPointsAPI
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.PermissionAttachment
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class LegendGuild : BukkitPlugin() {

    companion object {
        private val attachMap: MutableMap<UUID, PermissionAttachment> = ConcurrentHashMap()
        fun attachment(player: Player): PermissionAttachment = attachMap[player.uniqueId]
                ?: player.addAttachment(plugin).also { attachMap[player.uniqueId] = it }
        private lateinit var _config: Config
        private lateinit var _lang: Lang
        private lateinit var _item: Items
        private lateinit var _perms: Perms
        private lateinit var _econmony: Economy
        private lateinit var _gcontroller: GuildController
        private lateinit var _gpcontroller: GuildPlayerController
        private lateinit var _pointsApi: PlayerPointsAPI
        private lateinit var _gsicontroller: GuildShopItemController
        private lateinit var _qpcontroller: QuestPlayerController
        val config: Config
            get() = _config
        val lang: Lang
            get() = _lang
        val item: Items
            get() = _item
        val perms: Perms
            get() = _perms
        val economy: Economy
            get() = _econmony
        val guildController: GuildController
            get() = _gcontroller
        val guildPlayerController: GuildPlayerController
            get() = _gpcontroller
        val pointsAPI: PlayerPointsAPI
            get() = _pointsApi
        val guildShopController: GuildShopItemController
            get() = _gsicontroller
        val questPlayerController: QuestPlayerController
            get() = _qpcontroller
    }

    override fun enable() {
        val manager = KotLib.getConfigFactory(this)
                .register(Config::class).register(Items::class)
                .register(Lang::class).register(Perms::class)
                .registerDao(kClassOf(), GuildController::class)
                .registerDao(kClassOf(), GuildPlayerController::class)
                .registerDao(kClassOf(), GuildShopItemController::class)
                .registerDao(kClassOf(), QuestPlayerController::class)
                .dump()
        _config = manager.getConfig(kClassOf())
        _lang = manager.getConfig(kClassOf())
        _item = manager.getConfig(kClassOf())
        _perms = manager.getConfig(kClassOf())
        _gcontroller = manager.getDao(kClassOf())
        _gpcontroller = manager.getDao(kClassOf())
        _gsicontroller = manager.getDao(kClassOf())
        _qpcontroller = manager.getDao(kClassOf())
        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        _econmony = rsp?.provider ?: throw IllegalStateException("找不到有效的經濟插件")
        _pointsApi = getPlugin(PlayerPoints::class.java).api
        registerListeners()
        registerCmd(GuildCommand)
    }


    private fun registerListeners() {
        listen<EntityDamageByEntityEvent> {
            val killer = it.entity.playerKiller ?: return@listen
            val guild = killer.guild ?: return@listen
            val damagePlus = guild.percentage(GuildSkill.AZURE_DRAGON)
            it.damage += it.damage * damagePlus
            val critical = guild.percentage(GuildSkill.VERMILION_BIRD) * 100
            if ((0..100).random() < critical) {
                val extra = it.damage * guild.percentage(GuildSkill.WHITE_TIGER)
                killer.sendMessage(lang["damage-critical"].format(extra))
                it.damage += extra
            }
        }

        listen<EntityDamageEvent> {
            val player = it.entity as? Player ?: return@listen
            val guild = player.guild ?: return@listen
            it.damage -= it.damage * guild.percentage(GuildSkill.BLACK_TORTOISE)
        }

        listen<PlayerJoinEvent> {
            val attch = attachMap[it.player.uniqueId] ?: it.player.addAttachment(this)
            val player = it.player
            player.refreshPermissions(attch)
            attachMap[player.uniqueId] = attch
            if (!skinCache.contains(player.uniqueId)) {
                GlobalScope.launch {
                    val value = player.toSkinValue()
                    player.guildPlayer?.skinValue = value
                    skinCache[player.uniqueId] = value
                }
            }
            queue[player.uniqueId]?.forEach { msg -> player.sendMessage(msg) }
            player.tellInvite()
        }

        listen<InventoryCloseEvent> {
            plugin.debug("Remove admin operation for ${it.player.name}")
            ShopUI.adminOperate.remove(it.player.uniqueId)
            YourRequestUI.checkAdmin.remove(it.player.uniqueId)
        }

        listen<EntityDeathEvent> {
            val victim = it.entity
            val killer = (it.entity.lastDamageCause as? EntityDamageByEntityEvent)?.damager?.playerKiller
                    ?: return@listen
            questPlayerController.update(killer.uniqueId) {
                item?.killed?.add(victim.type)
            }
        }
    }


}