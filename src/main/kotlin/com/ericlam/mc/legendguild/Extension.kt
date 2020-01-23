package com.ericlam.mc.legendguild

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.catch
import com.ericlam.mc.kotlib.not
import com.ericlam.mc.legendguild.guild.Guild
import com.ericlam.mc.legendguild.guild.GuildPlayer
import com.ericlam.mc.legendguild.ui.factory.PromoteUI
import com.google.gson.Gson
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun OfflinePlayer.leaveGuild(): Boolean {
    val gp = this.guildPlayer ?: return false
    return if (gp.role != GuildPlayer.Role.POPE) {
        LegendGuild.guildPlayerController.delete(this.uniqueId)
        true
    } else {
        return if (LegendGuild.guildController.delete(gp.guild)) {
            LegendGuild.guildPlayerController.deleteSome {
                guild == name
            }.forEach {
                Bukkit.getPlayer(it)?.sendMessage(Lang["guild-deleted"])
            }
            true
        } else {
            false
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

fun OfflinePlayer.join(gName: String): JoinResponse {
    val targetGuild = GuildManager[gName] ?: return JoinResponse.UNKNOWN_GUILD
    if (!targetGuild.public && !targetGuild.invites.contains(this.uniqueId)) {
        return JoinResponse.NOT_INVITED
    }
    return when {
        this.guild?.name == name -> JoinResponse.ALREADY_IN_SAME_GUILD
        this.guild != null -> JoinResponse.ALREADY_IN_OTHER_GUILD
        targetGuild.memberMax <= targetGuild.members.size -> JoinResponse.FULL
        else -> {
            targetGuild.wannaJoins.add(this.uniqueId)
            PromoteUI.addPlayer(this)
            JoinResponse.SUCCESS
        }
    }
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
    NOT_INVITED
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