package com.ericlam.mc.legendguild

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.catch
import com.ericlam.mc.kotlib.not
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
import de.tr7zw.changeme.nbtapi.NBTItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import java.text.MessageFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun OfflinePlayer.leaveGuild(): Boolean {
    val gp = this.guildPlayer ?: return false
    return if (gp.role != GuildPlayer.Role.POPE) {
        LegendGuild.guildPlayerController.delete(this.uniqueId)
        true
    } else {
        val list = LegendGuild.guildPlayerController.find { guild == name }.mapNotNull { Bukkit.getOfflinePlayer(it.uuid) }
        return if (LegendGuild.guildController.delete(gp.guild)) {
            list.forEach {
                UIManager.clearCache(it)
                it.notify(Lang["guild-deleted"])
            }
            true
        } else {
            false
        }
    }
}


fun String.toPlayer(): OfflinePlayer? {
    return Bukkit.getPlayerUniqueId(this)?.let { Bukkit.getOfflinePlayer(it) }
}

fun Player.refreshPermissions() {
    this.removeAttachment(PermissionAttachment(BukkitPlugin.plugin, player))
    this.guildPlayer?.role?.permissions?.forEach {
        this.addAttachment(BukkitPlugin.plugin, it, true)
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
    get() = LegendGuild.guildPlayerController.findById(this.uniqueId)

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
    return skullMap[this.uuid] ?: let {
        val item = BukkitPlugin.plugin.itemStack(LegendGuild.config.materialHead,
                display = "§e$name",
                lore = lore.invoke(this)
        )
        item.itemMeta = item.itemMeta.toSkullMeta(this.skinValue)
        item
    }.also { skullMap[uuid] = it }
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

fun String.format(vararg o: Any): String {
    return MessageFormat.format(this, o)
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

fun Player.addItem(stack: ItemStack, price: Int) {
    val guild = this.guild ?: let {
        this.sendMessage(Lang["not-in-guild"])
        return
    }
    guildPlayer?.role?.not(GuildPlayer.Role.ELDER) ?: let {
        this.sendMessage(Lang["no-perm"])
        return
    }
    val inventories = ShopUI.paginatedCaches.keys.find { it == guild }?.let { ShopUI.paginatedCaches[it] } ?: return
    var inv = inventories.lastOrNull() ?: return
    while (inv.firstEmpty() == -1) {
        inv = JoinerUI.createPage()
        inventories.add(inv)
    }
    val item = UIManager.p.itemStack(stack.type, display = stack.itemMeta?.displayName ?: stack.type.toString(),
            lore = (stack.itemMeta?.lore ?: emptyList()) + listOf(
                    "&7==========================",
                    "&e擁有人: &f$displayName",
                    "&e價格: &f$price 貢獻值"
            ))
    val id = UUID.randomUUID()
    LegendGuild.guildShopController.update(guild.name) {
        items[id] = GuildShopItems.ShopItem(price, stack, uniqueId)
    }
    val nbtItem = NBTItem(item)
    nbtItem.setString("guild.shop", id.toString())
    inv.addItem(nbtItem.item)
}

val OfflinePlayer.skullItem: ItemStack
    get() {
        return skullMap[uniqueId] ?: let {
            val item = BukkitPlugin.plugin.itemStack(LegendGuild.config.materialHead,
                    display = "§e$name",
                    lore = listOf(
                            "&eUUID: &f$uniqueId",
                            "&a左鍵予以允許",
                            "&c右鍵予以移除"
                    )
            )
            item.itemMeta = item.itemMeta.toSkullMeta(skinCache[uniqueId] ?: steveSkin)
            item
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

val queue: MutableMap<UUID, MutableSet<String>> = ConcurrentHashMap()

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
            PromoteUI.addPlayer(this)
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


val materialHead: Material
    get() = LegendGuild.config.materialHead

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