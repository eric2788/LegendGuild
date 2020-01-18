package com.ericlam.mc.legendguild

import com.ericlam.mc.legendguild.guild.Guild
import com.ericlam.mc.legendguild.guild.GuildPlayer
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

object GuildManager {

    enum class ContributeResponse {
        NOT_IN_GUILD,
        FAILED,
        SUCCESS
    }

    enum class ResourceResponse {
        NOT_IN_GUILD,
        INVALID_ITEM,
        NOT_ENOUGH_MONEY,
        SUCCESS
    }

    enum class ResourceType {
        MONEY,
        ITEM
    }

    private val guildMap: List<Guild>
        get() = LegendGuild.guildController.findAll()

    val ranking: SortedSet<Guild>
        get() = guildMap.toSortedSet()

    operator fun get(name: String): Guild? {
        return guildMap.find { it.name == name }
    }

    operator fun get(uuid: UUID): Guild? {
        return guildMap.find { it.isMember(uuid) }
    }

    fun getPlayer(uuid: UUID): GuildPlayer?{
        return LegendGuild.guildPlayerController.findById(uuid)
    }


    fun contributeMoney(player: OfflinePlayer): ContributeResponse {
        val g = this[player.uniqueId] ?: return ContributeResponse.NOT_IN_GUILD
        val p = g[player.uniqueId]!!
        with(LegendGuild) {
            return if (economy.withdrawPlayer(player, config.dailyContribution.money.need).transactionSuccess()) {
                p.contribution += config.dailyContribution.money.contribute
                g exp config.dailyContribution.money.exp
                ContributeResponse.SUCCESS
            } else {
                ContributeResponse.FAILED
            }
        }
    }

    fun contributePoints(player: OfflinePlayer): ContributeResponse {
        val g = this[player.uniqueId] ?: return ContributeResponse.NOT_IN_GUILD
        val p = g[player.uniqueId]!!
        with(LegendGuild) {
            return if (pointsAPI.take(player.uniqueId, config.dailyContribution.points.need.toInt())) {
                p.contribution += config.dailyContribution.points.contribute
                g exp config.dailyContribution.points.exp
                ContributeResponse.SUCCESS
            } else {
                ContributeResponse.FAILED
            }
        }
    }

    fun postResource(player: Player, type: ResourceType): ResourceResponse {
        val g = this[player.uniqueId] ?: return ResourceResponse.NOT_IN_GUILD
        val p = g[player.uniqueId]!!
        with(LegendGuild) {
            return when (type) {
                ResourceType.MONEY -> if (economy.withdrawPlayer(player, config.postResources.money).transactionSuccess()) {
                    g.resource.money += config.postResources.money
                    g[player.uniqueId]?.let { it.contribution += config.postResources.money_contribute }
                    ResourceResponse.SUCCESS
                } else {
                    ResourceResponse.NOT_ENOUGH_MONEY
                }
                ResourceType.ITEM -> {
                    val stack = player.inventory.itemInMainHand
                    if ((stack?.type ?: Material.AIR) == Material.AIR) {
                        ResourceResponse.INVALID_ITEM
                    } else {
                        val key = item.items.entries.find { it.value == stack }?.key
                                ?: return ResourceResponse.INVALID_ITEM
                        config.postResources.items[key]?.let {
                            p.contribution += it
                            g.resource.items[key] = (g.resource.items[key] ?: 0) + it
                        }?.let { ResourceResponse.SUCCESS } ?: ResourceResponse.INVALID_ITEM
                    }
                }
            }
        }
    }

}