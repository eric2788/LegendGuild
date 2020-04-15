package com.ericlam.mc.legendguild

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.catch
import com.ericlam.mc.kotlib.msgFormat
import com.ericlam.mc.kotlib.not
import com.ericlam.mc.kotlib.translateColorCode
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.dao.GuildShopItems
import com.ericlam.mc.legendguild.dao.QuestPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.JoinerUI
import com.ericlam.mc.legendguild.ui.factory.PromoteUI
import com.ericlam.mc.legendguild.ui.factory.ShopUI
import com.google.gson.Gson
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import de.tr7zw.nbtapi.NBTItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.permissions.PermissionAttachment
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

fun OfflinePlayer.leaveGuild(): Boolean {
    val gp = this.guildPlayer ?: return false
    return if (gp.role != GuildPlayer.Role.POPE) {
        UIManager.clearCache(this)
        LegendGuild.guildPlayerController.delete(this.uniqueId)
        player?.closeInventory()
        true
    } else {
        val list = LegendGuild.guildPlayerController.find { guild == name }.mapNotNull { Bukkit.getOfflinePlayer(it.uuid) }
        list.forEach { UIManager.clearCache(it) }
        return if (LegendGuild.guildController.delete(gp.guild)) {
            list.forEach {
                it.player?.closeInventory()
                it.notify(Lang["guild-deleted"])
            }
            true
        } else {
            false
        }
    }
}

infix fun <T, R : Comparable<R>> Map<T, R>.inside(map: Map<T, R>): Boolean {
    return map.all { (ob, amount) -> this[ob]?.let { it >= amount } ?: false }
}


fun String.toPlayer(): OfflinePlayer? {
    return Bukkit.getPlayerUniqueId(this)?.let { Bukkit.getOfflinePlayer(it) }
}

fun Player.refreshPermissions(attachment: PermissionAttachment) {
    attachment.permissions.filter { it.key.startsWith("guild.") }.forEach { p -> attachment.unsetPermission(p.key) }
    this.guildPlayer?.role?.permissions?.forEach {
        attachment.setPermission(it, true)
    }
}

val Entity.playerKiller: Player?
    get() {
        return when (this) {
            is Player -> this
            is Projectile -> this.shooter as? Player
            is TNTPrimed -> this.source as? Player
            else -> return null
        }
    }

val GuildPlayer.Role.permissions: List<String>
    get() {
        return LegendGuild.perms.permissions.entries.flatMap {
            if (this hasPower it.key) {
                it.value
            } else {
                emptyList()
            }
        }
    }

val OfflinePlayer.guildPlayer: GuildPlayer?
    get() = LegendGuild.guildPlayerController.findById(this.uniqueId).also {
        it ?: BukkitPlugin.plugin.debug("cannot find $name in guild player")
    }?.also { BukkitPlugin.plugin.debug("successfully find $name in guild player") }

val OfflinePlayer.guild: Guild?
    get() = GuildManager[this.uniqueId]

fun CommandSender.tellSuccess() {
    this.sendMessage(Lang["success"])
}

fun CommandSender.tellFailed() {
    this.sendMessage(Lang["failed"])
}

fun Guild.findRole(role: GuildPlayer.Role): String {
    return this.members.find { it.role == role }?.name ?: "NONE"
}

private val skullMap: MutableMap<UUID, ItemStack> = ConcurrentHashMap()
val skinCache: MutableMap<UUID, String> = ConcurrentHashMap()

fun GuildPlayer.toSkull(lore: GuildPlayer.() -> List<String> = { listOf() }): ItemStack {
    return player.toSkull { lore() }
}

fun OfflinePlayer.toSkull(lore: OfflinePlayer.() -> List<String> = { listOf() }): ItemStack {
    return this.skullItem.apply {
        this.lore = lore().map { it.translateColorCode() }
    }
}

fun ItemMeta.toSkullMeta(skin: String): ItemMeta {
    val profile = GameProfile(UUID.randomUUID(), null)
    profile.properties.put("textures", Property("textures", skin))
    catch<Exception>({
        Bukkit.getLogger().warning("Create Skull Item with skin ($skin) failed")
    }) {
        this::class.java.getDeclaredField("profile").also {
            it.isAccessible = true
            it.set(this, profile)
        }
    }
    return this
}

fun String.format(vararg o: Any?): String {
    return this.msgFormat(*o)
}

val QuestPlayer.QuestResult.path: String
    get() {
        return when (this) {
            QuestPlayer.QuestResult.SUCCESS_AND_REWARDED -> "quest-result.success"
            QuestPlayer.QuestResult.DEADLINED -> "quest-result.deadlined"
            QuestPlayer.QuestResult.NOT_STARTED_ANY -> "quest-result.no-quest"
            QuestPlayer.QuestResult.FAILED -> "quest-result.failed"
        }
    }

object Lang {
    operator fun get(path: String): String {
        return LegendGuild.lang[path]
    }

    object Setter {
        operator fun get(path: String): String {
            return LegendGuild.lang["setter.$path"]
        }
    }

    object Item {
        operator fun get(path: String): String {
            return LegendGuild.lang["item-translate.$path"]
        }
    }

    object Page {
        operator fun get(path: String): String {
            return LegendGuild.lang["page.$path"]
        }
    }

    object Shop {
        operator fun get(path: String): String {
            return LegendGuild.lang["shop.$path"]
        }
    }

    object Request {
        operator fun get(path: String): String {
            return LegendGuild.lang["request.$path"]
        }
    }

    object PvP {
        operator fun get(path: String): String {
            return LegendGuild.lang["pvp.$path"]
        }
    }
}

val GuildManager.CreateResponse.path: String
    get() {
        return when (this) {
            GuildManager.CreateResponse.NAME_EXIST -> "name-exist"
            GuildManager.CreateResponse.IN_GUILD -> "in-guild"
            GuildManager.CreateResponse.ILLEGAL_CHAR -> "illegal-char"
            GuildManager.CreateResponse.OVER_CHAR -> "over-char"
            GuildManager.CreateResponse.SUCCESS -> "success"
        }
    }

val ItemStack.toBukkitItemStack: ItemStack
    get() = ItemStack(this)

fun Player.addItem(stack: ItemStack, price: Int) {
    val guild = this.guild ?: let {
        this.sendMessage(Lang["not-in-guild"])
        return
    }
    guildPlayer?.role?.not(GuildPlayer.Role.ELDER) ?: let {
        this.sendMessage(Lang["no-perm"])
        return
    }
    val id = UUID.randomUUID()
    val shopItem = GuildShopItems.ShopItem(price, stack, uniqueId)
    LegendGuild.guildShopController.update(guild.name) {
        items[id] = shopItem
    }
    val nbtItem = shopItem.toShopItem(player.uniqueId, id)
    BukkitPlugin.plugin.debug("prepare to add item into ShopUI: $nbtItem")
    ShopUI.addProduct(this, nbtItem.item)
    this.tellSuccess()
    player.inventory.itemInMainHand = null
}

fun GuildShopItems.ShopItem.toShopItem(uniqueId: UUID, id: UUID): NBTItem {
    val nbtItem = NBTItem(this.item.clone())
    val name = Bukkit.getOfflinePlayer(uniqueId)?.name ?: "UNKNOWN USER"
    nbtItem.setString("guild.shop", id.toString())
    nbtItem.setString("guild.shop.seller", uniqueId.toString())
    val desp = listOf(
            "&7==========================",
            "&e擁有人: &f$name",
            "&e價格: &f$price 貢獻值"
    ).map { it.translateColorCode() }
    BukkitPlugin.plugin.debug("$name uploaded an item $id with price $price")
    val item = nbtItem.item
    item.itemMeta?.run {
        this.lore = (item.lore ?: emptyList()) + desp
        item.itemMeta = this
    } ?: kotlin.run {
        val display = nbtItem.getCompound("display")
        display.setString("Lore", desp.toString())
    }
    return nbtItem
}

fun Player.removeItem(stack: ItemStack): Boolean {
    val guild = this.guild ?: let {
        this.sendMessage(Lang["not-in-guild"])
        return false
    }
    guildPlayer?.role?.not(GuildPlayer.Role.ELDER) ?: let {
        this.sendMessage(Lang["no-perm"])
        return false
    }
    val inventories = ShopUI.paginatedCaches.keys.find { it == guild }?.let { ShopUI.paginatedCaches[it] }
            ?: throw IllegalStateException("cannot find ui for ${guild.name}")
    val inv = inventories.find { inv -> inv.contains(stack) } ?: return false
    inv.remove(stack)
    val id = NBTItem(stack).getString("guild.shop")?.let { UUID.fromString(it) }
    return LegendGuild.guildShopController.update(guild.name) {
        this.items.remove(id)
    }?.let {
        inventory.addItem(stack)
        true
    } ?: false
}

val OfflinePlayer.joinerSkull: ItemStack
    get() = this.toSkull {
        listOf(
                "&eUUID: &f$uniqueId",
                "&a左鍵予以允許",
                "&c右鍵予以移除"
        )
    }

private val OfflinePlayer.skullItem: ItemStack
    get() {
        return skullMap[uniqueId] ?: let {
            val item = BukkitPlugin.plugin.itemStack(LegendGuild.config.materials.head,
                    display = "§e$name")
            item.itemMeta = item.itemMeta.toSkullMeta(skinCache[uniqueId] ?: steveSkin)
            val nbtItem = NBTItem(item)
            nbtItem.setString("guild.head.owner", uniqueId.toString())
            nbtItem.item
        }.also {
            skullMap[uniqueId] = it
        }
    }

suspend fun OfflinePlayer.toSkinValue(): String {
    return withContext(Dispatchers.Default) { httpGet("https://sessionserver.mojang.com/session/minecraft/profile/${uniqueId.toString().replace("-", "")}") }
}

fun OfflinePlayer.notify(msg: String) {
    if (this.isOnline) {
        this.player.sendMessage(msg)
    } else {
        val list = queue[this.uniqueId] ?: mutableSetOf<String>().also { queue[this.uniqueId] = it }
        list.add(msg)
    }
}

fun Player.tellInvite() {
    BukkitPlugin.plugin.debug("tell invite to $name")
    GuildManager.guildMap.filter { g -> g.invites.contains(this.uniqueId) }.forEach { g ->
        this.sendMessage(Lang["invited"].format(g.name))
        val yes = ComponentBuilder("答應請求").color(ChatColor.GREEN).underlined(true).event(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/guild response accept ${g.name}")).event(HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("點擊以選擇")))
        val no = ComponentBuilder("拒絕請求").color(ChatColor.RED).underlined(true).event(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/guild response decline ${g.name}")).event(HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("點擊以選擇")))
        val text = ComponentBuilder("[").color(ChatColor.GRAY).append(yes.create()).color(ChatColor.GRAY).append("|").append(no.create()).color(ChatColor.GRAY).append("]").create()
        this.spigot().sendMessage(*text)
        BukkitPlugin.plugin.debug("tell invite to $name for joining guild ${g.name}")
    }
}

val queue: MutableMap<UUID, MutableSet<String>> = ConcurrentHashMap()


fun OfflinePlayer.joinGuild(gName: String) {
    BukkitPlugin.plugin.debug("${this.name} prepare to join guild $gName")
    val con = LegendGuild.guildPlayerController
    if (LegendGuild.guildController.findById(gName) == null) {
        BukkitPlugin.plugin.debug("${this.name} already has guild, so skipped")
        return
    }
    GlobalScope.async {
        val skin = toSkinValue()
        con.save { GuildPlayer(uniqueId, name, gName, skin) }
    }.invokeOnCompletion {
        it?.printStackTrace()?.also {
            BukkitPlugin.plugin.debug("error appeared, so failed")
            player?.tellFailed()
        }
                ?: player?.tellSuccess().also {
                    BukkitPlugin.plugin.debug("successfully save $name to guild $gName")
                    LegendGuild.guildController.update(gName) {
                        wannaJoins.remove(uniqueId)
                    }
                    val off = this
                    UIManager.p.schedule { // run in main thread
                        BukkitPlugin.plugin.debug("preparing to add $name in PromoteUI")
                        PromoteUI.addPlayer(off)
                        player?.closeInventory()
                        off.player?.refreshPermissions(LegendGuild.attachment(off.player))
                    }
                }
    }
}

fun OfflinePlayer.join(gName: String): JoinResponse {
    val targetGuild = GuildManager[gName] ?: return JoinResponse.UNKNOWN_GUILD
    if (!targetGuild.public && !targetGuild.invites.contains(this.uniqueId)) {
        return JoinResponse.NOT_INVITED
    }
    return when {
        this.guild?.name == name -> JoinResponse.ALREADY_IN_SAME_GUILD
        this.guild != null -> JoinResponse.ALREADY_IN_OTHER_GUILD
        targetGuild.memberMax <= targetGuild.members.size -> JoinResponse.FULL
        targetGuild.wannaJoins.contains(this.uniqueId) -> JoinResponse.REQUEST_SENT
        else -> {
            targetGuild.wannaJoins.add(this.uniqueId)
            JoinerUI.addPlayer(this)
            JoinResponse.SUCCESS
        }
    }
}

inline val CommandSender.toPlayer: Player?
    get() = this as? Player ?: let {
        this.sendMessage(Lang["not-player"])
        null
    }

fun <E> MutableList<E>.removeLast(): Boolean {
    val index = (this.size - 1).not(-1) ?: return false
    this.removeAt(index)
    return true
}

enum class JoinResponse {
    FULL,
    ALREADY_IN_SAME_GUILD,
    ALREADY_IN_OTHER_GUILD,
    SUCCESS,
    UNKNOWN_GUILD,
    NOT_INVITED,
    REQUEST_SENT
}

enum class UpgradeResponse {
    NOT_IN_GUILD,
    UNKNOWN_REQUIREMENT,
    SUCCESS,
    CONDITION_INSUFFICIENT
}

val JoinResponse.path: String
    get() {
        return when (this) {
            JoinResponse.NOT_INVITED -> "not-invited"
            JoinResponse.UNKNOWN_GUILD -> "unknown-guild"
            JoinResponse.FULL -> "full"
            JoinResponse.ALREADY_IN_SAME_GUILD -> "same-guild"
            JoinResponse.ALREADY_IN_OTHER_GUILD -> "in-guild"
            JoinResponse.SUCCESS -> "success"
            JoinResponse.REQUEST_SENT -> "invite-sent"
        }
    }

val UpgradeResponse.path: String
    get() {
        return when (this) {
            UpgradeResponse.UNKNOWN_REQUIREMENT -> "unknown-requirement"
            UpgradeResponse.NOT_IN_GUILD -> "not-in-guild"
            UpgradeResponse.CONDITION_INSUFFICIENT -> "requirement-insufficient"
            UpgradeResponse.SUCCESS -> "success"
        }
    }

val materialHead: Material
    get() = LegendGuild.config.materials.head

val materialGlassPane: Material
    get() = LegendGuild.config.materials.glassPane

val materialLeash: Material
    get() = LegendGuild.config.materials.leash

fun httpGet(urlStr: String): String {
    return try {
        val url = URL(urlStr)
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            val gson = Gson()
            val map = gson.fromJson(inputStream.bufferedReader(), Map::class.java)
            return@with ((map["properties"] as List<*>)[0] as Map<*, *>)["value"] as String
        }
    } catch (e: IOException) {
        return steveSkin
    }
}

const val steveSkin = "eyJ0aW1lc3RhbXAiOjE1Nzk3ODMwMTU1MzYsInByb2ZpbGVJZCI6Ijg2NjdiYTcxYjg1YTQwMDRhZjU0NDU3YTk3MzRlZWQ3IiwicHJvZmlsZU5hbWUiOiJTdGV2ZSIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kYzFjNzdjZThlNTQ5MjVhYjU4MTI1NDQ2ZWM1M2IwY2RkM2QwY2EzZGIyNzNlYjkwOGQ1NDgyNzg3ZWY0MDE2In0sIkNBUEUiOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85NTNjYWM4Yjc3OWZlNDEzODNlNjc1ZWUyYjg2MDcxYTcxNjU4ZjIxODBmNTZmYmNlOGFhMzE1ZWE3MGUyZWQ2In19fQ=="