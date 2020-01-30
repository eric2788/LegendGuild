package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.ui.UIManager
import de.tr7zw.changeme.nbtapi.NBTEntity
import de.tr7zw.changeme.nbtapi.NBTItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
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
                            val nbtPlayer = NBTEntity(player)
                            val id = NBTItem(stack).getString("guild.shop")?.let { UUID.fromString(it) }
                            when (nbtPlayer.getString("guild.admin.operate")) {
                                "shop.set" -> {
                                    clickedInventory?.remove(stack)
                                    if (player.inventory.addItem(stack).isNotEmpty()) {
                                        player.world.dropItem(player.location, stack)
                                    }
                                    val shop = id?.let {
                                        LegendGuild.guildShopController.findById(player.guild?.name ?: "")
                                    }?.let { it.items[id] }
                                    shop?.owner?.let { Bukkit.getOfflinePlayer(it) }?.notify(Lang.Shop["product-removed"])
                                    player.tellSuccess()
                                }
                                "shop.check" -> isCancelled = true
                                else -> {
                                    val res = GuildManager.buyProduct(player, stack)
                                    player.sendMessage(res.first.message)
                                    if (res.first == GuildManager.ShopResponse.BUY_SUCCESS) {
                                        clickedInventory?.remove(stack)
                                        res.second?.owner?.let { Bukkit.getOfflinePlayer(it) }?.notify(Lang.Shop["someone-bought"])
                                    }
                                    return@Clicker
                                }
                            }
                            nbtPlayer.removeKey("guild.admin.operate")
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

    private val GuildManager.ShopResponse.message: String
        get() {
            return when (this) {
                GuildManager.ShopResponse.NOT_IN_GUILD -> Lang["not-in-guild"]
                GuildManager.ShopResponse.INVALID_ITEM -> Lang["invalid-item"]
                GuildManager.ShopResponse.NO_PRODUCT -> Lang.Shop["no-product"]
                GuildManager.ShopResponse.NOT_ENOUGH_CONTRIBUTE -> Lang.Shop["no-contribute"]
                GuildManager.ShopResponse.BUY_SUCCESS -> Lang.Shop["buy-success"]
                GuildManager.ShopResponse.INVENTORY_FULL -> Lang.Shop["inv-full"]
            }
        }
}