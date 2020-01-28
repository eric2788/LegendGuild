package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.GuildManager
import com.ericlam.mc.legendguild.Lang
import com.ericlam.mc.legendguild.LegendGuild
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.dao.GuildShopItems
import com.ericlam.mc.legendguild.guild
import com.ericlam.mc.legendguild.ui.UIManager
import de.tr7zw.changeme.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ShopUI : UIFactoryPaginated {

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        throw Exception("trying to run API.jar into server")
    }

    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = ConcurrentHashMap()

    override val paginatedCaches: MutableMap<Guild, MutableList<Inventory>> = ConcurrentHashMap()

    override fun createPage(): Inventory {
        return UIManager.p.createGUI(
                rows = 6, title = "&a商店列表",
                fills = mapOf(
                        0..53 to Clicker(UIManager.p.itemStack(Material.AIR)) { player, stack ->
                            val res = GuildManager.buyProduct(player, stack)
                            player.sendMessage(res.message)
                            if (res == GuildManager.ShopResponse.BUY_SUCCESS) {
                                clickedInventory?.remove(stack)
                            }
                        },
                        (6 row 2)..(6 row 8) to Clicker(UIFactoryPaginated.decorate)
                )
        ) {
            mapOf(
                    6 row 1 to Clicker(UIFactoryPaginated.previous) { player, _ ->
                        val iterator = getIterator(player)
                        if (iterator.hasPrevious()) {
                            UIManager.openUI(player, iterator.previous())
                        } else {
                            player.sendMessage(Lang.Page["no-previous"])
                        }
                    },
                    6 row 9 to Clicker(UIFactoryPaginated.next) { player, _ ->
                        val iterator = getIterator(player)
                        if (iterator.hasNext()) {
                            UIManager.openUI(player, iterator.next())
                        } else {
                            player.sendMessage(Lang.Page["no-next"])
                        }
                    }
            )
        }
    }

    fun Player.addItem(stack: ItemStack, price: Int) {
        val guild = this.guild ?: let {
            this.sendMessage(Lang["not-in-guild"])
            return
        }
        val inventories = paginatedCaches.keys.find { it == guild }?.let { paginatedCaches[it] } ?: return
        var inv = inventories.lastOrNull() ?: return
        while (inv.firstEmpty() == -1) {
            inv = JoinerUI.createPage()
            inventories.add(inv)
        }
        val item = UIManager.p.itemStack(stack.type, display = stack.itemMeta?.displayName ?: stack.type.toString(),
                lore = listOf(
                        "&e擁有人: &f$displayName",
                        "&e價格: &f$price 貢獻值"
                ))
        val id = UUID.randomUUID()
        LegendGuild.guildShopController.update(guild.name) {
            items[id] = GuildShopItems.ShopItem(price, stack)
        }
        val nbtItem = NBTItem(item)
        nbtItem.setString("guild.shop", id.toString())
        inv.addItem(item)
    }

    private val GuildManager.ShopResponse.message: String
        get() {
            return when (this) {
                GuildManager.ShopResponse.NOT_IN_GUILD -> Lang["not-in-guild"]
                GuildManager.ShopResponse.INVALID_ITEM -> Lang["invalid-item"]
                GuildManager.ShopResponse.NO_PRODUCT -> Lang.Shop["no-product"]
                GuildManager.ShopResponse.NOT_ENOUGH_CONTRIBUTE -> Lang.Shop["no-contribute"]
                GuildManager.ShopResponse.BUY_SUCCESS -> Lang.Shop["buy-success"]
            }
        }
}