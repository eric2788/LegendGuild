package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.Guild
import com.ericlam.mc.legendguild.ui.UIManager
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

    val adminOperate: MutableMap<UUID, Operation> = mutableMapOf()

    enum class Operation {
        BANK,
        SET_BANK
    }

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        val guild = bPlayer.guild ?: return emptyList()
        return paginatedCaches[guild] ?: let {
            val shop = LegendGuild.guildShopController.findById(guild.name) ?: return emptyList()
            LegendGuild.debug("initializing shop inventory list for ${guild.name}")
            LegendGuild.debug("shop current paginatedCaches details: ${paginatedCaches.map { "${it.key.name} => ${it.value.flatMap { i -> i.contents.toList() }.map { s -> s.toString() }}" }}")
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
            LegendGuild.debug("shop inventory list initial size: ${inventories.size}")
            inventories
        }.also {
            updatePaginatedInfo(guild, it)
            paginatedCaches[guild] = it
        }
    }

    fun addProduct(p: OfflinePlayer, item: ItemStack) {
        val inventories = paginatedCaches[p.guild] ?: let {
            LegendGuild.debug("cannot find any shopUI pages, creating one")
            val i = getPaginatedUI(p)
            if (i.isNotEmpty()) {
                LegendGuild.debug("create success, using recursive method")
                addProduct(p, item)
            }
            return
        }
        var inv = inventories.lastOrNull() ?: throw IllegalStateException("shop ui inventory list is empty")
        if (inv.firstEmpty() == -1) {
            inv = createPage()
            inventories.add(inv)
        }
        LegendGuild.debug("preparing to add item $item")
        inv.addItem(item)
    }

    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = ConcurrentHashMap()

    override val paginatedCaches: MutableMap<Guild, MutableList<Inventory>> = ConcurrentHashMap()

    override fun createPage(): Inventory {
        LegendGuild.debug("Creating new page of ${this::class.simpleName}")
        pageCache.clear()
        LegendGuild.debug("${this::class.simpleName} new page, so clear pageCache")
        return UIManager.p.createGUI(
                rows = 6, title = "&a商店列表",
                fills = mapOf(
                        0..53 to Clicker(UIManager.p.itemStack(Material.AIR)) { player, stack ->
                            val id = NBTItem(stack).getString("guild.shop")?.let {
                                LegendGuild.debug("clicked item $it, casting to UUID")
                                UUID.fromString(it)
                            }
                            val operation = adminOperate[player.uniqueId]
                            LegendGuild.debug("Player clicked operation: $operation")
                            when (operation) {
                                Operation.SET_BANK -> {
                                    LegendGuild.debug("admin clicked ShopUI.")
                                    LegendGuild.debug("admin operation: remove product")
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
                                Operation.BANK -> {
                                    LegendGuild.debug("admin clicked ShopUI.")
                                    LegendGuild.debug("admin operation: checking product")
                                    isCancelled = true
                                }
                                else -> {
                                    LegendGuild.debug("player clicked ShopUI.")
                                    val res = GuildManager.buyProduct(player, stack)
                                    player.sendMessage(res.first.message)
                                    if (res.first == GuildManager.ShopResponse.BUY_SUCCESS) {
                                        clickedInventory?.remove(stack)
                                        res.second?.owner?.let { Bukkit.getOfflinePlayer(it) }?.notify(Lang.Shop["someone-bought"])
                                    }
                                    return@Clicker
                                }
                            }
                        },
                        (6 row 2)..(6 row 8) to Clicker(UIFactoryPaginated.decorate)
                )
        ) {
            pageOperator
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