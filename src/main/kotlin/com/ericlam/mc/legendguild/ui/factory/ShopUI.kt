package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.ui.UIManager
import de.tr7zw.nbtapi.NBTEntity
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

object ShopUI : UIFactoryPaginated {

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        val guild = bPlayer.guild ?: return emptyList()
        val shop = LegendGuild.guildShopController.findById(guild.name) ?: return emptyList()
        return paginatedCaches[guild] ?: let {
            BukkitPlugin.plugin.debug("initializing shop inventory list for ${guild.name}")
            BukkitPlugin.plugin.debug("shop current paginatedCaches details: ${paginatedCaches.map { "${it.key.name} => ${it.value.flatMap { i -> i.contents.toList() }.map { s -> s.toString() }}" }}")
            val inventories = mutableListOf<Inventory>()
            var currentInv = createPage()
            inventories.add(currentInv)
            val queue = ConcurrentLinkedDeque(shop.items.entries)
            while (queue.isNotEmpty()) {
                val gPlayer = queue.poll()
                val shopItem = gPlayer.value.toShopItem(gPlayer.value.owner, gPlayer.key)
                currentInv.addItem(shopItem.item)
                if (currentInv.firstEmpty() == -1) {
                    currentInv = createPage()
                    inventories.add(currentInv)
                }
            }
            BukkitPlugin.plugin.debug("shop inventory list initial size: ${inventories.size}")
            inventories
        }.also {
            updatePaginatedInfo(guild, it)
            paginatedCaches[guild] = it
        }
    }

    fun addProduct(p: OfflinePlayer, item: ItemStack) {
        val inventories = paginatedCaches[p.guild] ?: let {
            val i = getPaginatedUI(p)
            if (i.isNotEmpty()) return addProduct(p, item)
            else return
        }
        var inv = inventories.lastOrNull() ?: throw IllegalStateException("shop ui inventory list is empty")
        if (inv.firstEmpty() == -1) {
            inv = createPage()
            inventories.add(inv)
        }
        inv.addItem(item)
    }

    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = ConcurrentHashMap()

    override val paginatedCaches: MutableMap<Guild, MutableList<Inventory>> = ConcurrentHashMap()

    override fun createPage(): Inventory {
        BukkitPlugin.plugin.debug("Creating new page of ${this::class.simpleName}")
        return UIManager.p.createGUI(
                rows = 6, title = "&a商店列表",
                fills = mapOf(
                        0..53 to Clicker(UIManager.p.itemStack(Material.AIR)) { player, stack ->
                            val nbtPlayer = NBTEntity(player)
                            val id = NBTItem(stack).getString("guild.shop")?.let {
                                BukkitPlugin.plugin.debug("clicked item $it, casting to UUID")
                                UUID.fromString(it)
                            }
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
                                    clickedInventory?.remove(stack)
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