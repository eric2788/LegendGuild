package com.ericlam.mc.legendguild

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.dao.GuildShopItems
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

object GuildManager {

    enum class ContributeResponse {
        NOT_IN_GUILD,
        NOT_ENOUGH_MONEY,
        SUCCESS,
        ALREADY_DID_TODAY
    }

    enum class ResourceResponse {
        NOT_IN_GUILD,
        INVALID_ITEM,
        NOT_ENOUGH_MONEY,
        SUCCESS,
    }

    enum class ResourceType {
        MONEY,
        ITEM
    }

    val guildMap: List<Guild>
        get() = LegendGuild.guildController.findAll()

    val leaderBoard: SortedSet<Guild>
        get() = guildMap.sortedDescending().toSortedSet()

    operator fun get(name: String): Guild? {
        return LegendGuild.guildController.findById(name)
    }

    operator fun get(uuid: UUID): Guild? {
        BukkitPlugin.plugin.debug("GuildManager[uuid] findById($uuid)")
        return LegendGuild.guildPlayerController.findById(uuid)?.guild?.let { this[it] }.also {
            it ?: also { BukkitPlugin.plugin.debug("cannot find guild for $uuid") }
        }
    }

    enum class SalaryResponse {
        NOT_IN_GUILD,
        ROLE_NO_SALARIES,
        ALREADY_GET_TODAY,
        SUCCESS,
        SUCCESS_NEGATIVE,
        FAILED
    }

    enum class ShopResponse {
        NOT_IN_GUILD,
        BUY_SUCCESS,
        INVALID_ITEM,
        NO_PRODUCT,
        NOT_ENOUGH_CONTRIBUTE,
        INVENTORY_FULL
    }

    enum class CreateResponse {
        NAME_EXIST,
        IN_GUILD,
        ILLEGAL_CHAR,
        OVER_CHAR,
        SUCCESS
    }

    fun createGuild(player: OfflinePlayer, name: String): CreateResponse {
        if (player.guild != null) return CreateResponse.IN_GUILD
        if (guildMap.any { it.name == name }) return CreateResponse.NAME_EXIST
        if (name.length > LegendGuild.config.maxChar) return CreateResponse.OVER_CHAR
        if (name.matches("\\p{L}*\\P{L}\\p{L}*".toRegex())) return CreateResponse.ILLEGAL_CHAR
        LegendGuild.guildController.save { Guild(name) }
        LegendGuild.guildPlayerController.save { GuildPlayer(player.uniqueId, player.name, name, role = GuildPlayer.Role.POPE) }
        LegendGuild.guildShopController.save { GuildShopItems(name) }
        return CreateResponse.SUCCESS
    }

    fun buyProduct(player: Player, stack: ItemStack): Pair<ShopResponse, GuildShopItems.ShopItem?> {
        val guild = player.guild ?: return ShopResponse.NOT_IN_GUILD to null
        val gPlayer = player.guildPlayer ?: return ShopResponse.NOT_IN_GUILD to null
        val shopItems = LegendGuild.guildShopController.findById(guild.name)
        val nbtItem = NBTItem(stack)
        val id = nbtItem.getString("guild.shop")?.let {
            try {
                UUID.fromString(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        } ?: return ShopResponse.INVALID_ITEM to null

        val shopItem = shopItems?.items?.filterKeys { it == id }?.map { it.value }?.singleOrNull()
                ?: return ShopResponse.NO_PRODUCT to null
        val price = shopItem.price
        return if (gPlayer consume price) {
            if (player.inventory.addItem(shopItem.item).isEmpty()) {
                shopItems.items.remove(id)
                ShopResponse.BUY_SUCCESS to shopItem
            } else {
                ShopResponse.INVENTORY_FULL to shopItem
            }.also {
                LegendGuild.guildShopController.save { shopItems }
                LegendGuild.guildPlayerController.save { gPlayer }
            }
        } else {
            ShopResponse.NOT_ENOUGH_CONTRIBUTE to shopItem
        }
    }

    fun sendSalary(player: OfflinePlayer): SalaryResponse {
        val g = player.guild ?: return SalaryResponse.NOT_IN_GUILD
        val p = player.guildPlayer ?: return SalaryResponse.NOT_IN_GUILD
        if (!p.canGetSalary) return SalaryResponse.ALREADY_GET_TODAY
        else {
            val salary = LegendGuild.config.salaries[p.role] ?: return SalaryResponse.ROLE_NO_SALARIES
            BukkitPlugin.plugin.debug("getting salary for ${player.name}: $$salary")
            return if (LegendGuild.economy.depositPlayer(player, salary).transactionSuccess()) {
                g.resource.money -= salary
                BukkitPlugin.plugin.debug("now guild have money: $${g.resource.money}")
                p.setLastSalary()
                if (g.resource.money < 0) SalaryResponse.SUCCESS_NEGATIVE else SalaryResponse.SUCCESS
            } else {
                SalaryResponse.FAILED
            }.also { LegendGuild.guildController.save { g } }
        }
    }

    fun upgradeSkill(skill: GuildSkill, p: OfflinePlayer): UpgradeResponse {
        val guild = p.guild ?: let {
            return UpgradeResponse.NOT_IN_GUILD
        }
        val res = guild.resource
        val requirement = LegendGuild.config.skills[skill] ?: let {
            return UpgradeResponse.UNKNOWN_REQUIREMENT
        }

        return if (res.money >= requirement.money && res.items inside requirement.items) {
            res.money -= requirement.money
            requirement.items.forEach { (ob, am) ->
                res.items[ob] = res.items[ob]!! - am
                if (res.items[ob]!! <= 0) res.items.remove(ob)

            }
            LegendGuild.guildController.save { guild }
            UpgradeResponse.SUCCESS
        } else {
            UpgradeResponse.CONDITION_INSUFFICIENT
        }
    }

    fun contributeMoney(player: OfflinePlayer): ContributeResponse {
        val g = player.guild ?: return ContributeResponse.NOT_IN_GUILD
        val p = player.guildPlayer ?: return ContributeResponse.NOT_IN_GUILD
        if (!p.canContribute) return ContributeResponse.ALREADY_DID_TODAY
        with(LegendGuild) {
            return if (economy.withdrawPlayer(player, config.dailyContribution.money.need).transactionSuccess()) {
                p.contribution += config.dailyContribution.money.contribute
                g exp config.dailyContribution.money.exp
                p.setLastContribute()
                ContributeResponse.SUCCESS
            } else {
                ContributeResponse.NOT_ENOUGH_MONEY
            }.also {
                guildPlayerController.save { p }
                guildController.save { g }
            }
        }
    }

    fun contributePoints(player: OfflinePlayer): ContributeResponse {
        val g = this[player.uniqueId] ?: return ContributeResponse.NOT_IN_GUILD
        val p = g[player.uniqueId] ?: return ContributeResponse.NOT_IN_GUILD
        if (!p.canContribute) return ContributeResponse.ALREADY_DID_TODAY
        with(LegendGuild) {
            return if (pointsAPI.take(player.uniqueId, config.dailyContribution.points.need.toInt())) {
                p.contribution += config.dailyContribution.points.contribute
                g exp config.dailyContribution.points.exp
                p.setLastContribute()
                ContributeResponse.SUCCESS
            } else {
                ContributeResponse.NOT_ENOUGH_MONEY
            }.also {
                guildPlayerController.save { p }
                guildController.save { g }
            }
        }
    }

    fun postResource(player: Player, type: ResourceType): ResourceResponse {
        val g = this[player.uniqueId] ?: return ResourceResponse.NOT_IN_GUILD
        val p = g[player.uniqueId] ?: return ResourceResponse.NOT_IN_GUILD
        with(LegendGuild) {
            return when (type) {
                ResourceType.MONEY -> if (economy.withdrawPlayer(player, config.postResources.money).transactionSuccess()) {
                    g.resource.money += config.postResources.money
                    p.contribution += config.postResources.money_contribute
                    ResourceResponse.SUCCESS.also {
                        guildController.save { g }
                        guildPlayerController.save { p }
                    }
                } else {
                    ResourceResponse.NOT_ENOUGH_MONEY
                }
                ResourceType.ITEM -> {
                    val stack = player.itemOnCursor
                    if ((stack?.type ?: Material.AIR) == Material.AIR) {
                        ResourceResponse.INVALID_ITEM
                    } else {
                        val key = item.items.entries.find { it.value.item == stack.asOne() }?.key
                                ?: return ResourceResponse.INVALID_ITEM
                        config.postResources.items[key]?.let {
                            p.contribution += it
                            g.resource.items[key] = (g.resource.items[key] ?: 0) + it
                            player.itemOnCursor = null
                        }?.let { ResourceResponse.SUCCESS }?.also {
                            guildController.save { g }
                            guildPlayerController.save { p }
                        } ?: ResourceResponse.INVALID_ITEM
                    }
                }
            }
        }
    }

}