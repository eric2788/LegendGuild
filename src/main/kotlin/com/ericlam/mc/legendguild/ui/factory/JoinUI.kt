package com.ericlam.mc.legendguild.ui.factory

import com.ericlam.mc.kotlib.Clicker
import com.ericlam.mc.kotlib.row
import com.ericlam.mc.legendguild.*
import com.ericlam.mc.legendguild.dao.GuildPlayer
import com.ericlam.mc.legendguild.ui.UIManager
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.TimeUnit

object JoinUI : UIFactoryPaginated {

    private val inventories: MutableList<Inventory> = LinkedList()

    init {
        UIManager.p.schedule(period = 3, unit = TimeUnit.MINUTES) { updateInv() }
    }

    private fun updateInv() {
        inventories.forEach { it.viewers.forEach { p -> p.closeInventory() } }
        inventories.clear()
        var inv = createPage()
        inventories.add(inv)
        GuildManager.guildMap.forEach { guild ->
            if (inv.firstEmpty() == -1) {
                inv = createPage()
                inventories.add(inv)
            }
            val lore = listOf(
                    "&e擁有者: ${guild.members.find { it.role == GuildPlayer.Role.POPE }?.name}",
                    "&b等級: ${guild.currentLevel}"
            )
            val pope = guild.members.find { it.role == GuildPlayer.Role.POPE }
            val item = makeHead(display = "&a${guild.name}",
                    lore = lore).apply {
                this.itemMeta = itemMeta.toSkullMeta(pope?.skinValue ?: steveSkin)
            }
            val nbt = NBTItem(item)
            nbt.setString("guild.join", guild.name)
            inv.addItem(nbt.item)
        }
    }

    override fun getPaginatedUI(bPlayer: OfflinePlayer): List<Inventory> {
        return if (inventories.isEmpty()) updateInv().let { inventories } else inventories
    }

    override val pageCache: MutableMap<OfflinePlayer, ListIterator<Inventory>> = mutableMapOf()

    override fun createPage(): Inventory {
        LegendGuild.debug("Creating new page of ${this::class.simpleName}")
        pageCache.clear()
        LegendGuild.debug("${this::class.simpleName} new page, so clear pageCache")
        return UIManager.p.createGUI(
                rows = 6, title = "&a宗門列表一覽 (三分鐘更新一次)",
                fills = mapOf(
                        0..53 to Clicker(UIManager.p.itemStack(Material.AIR)) { player, stack: ItemStack? ->
                            val guild = stack?.let { NBTItem(it).getString("guild.join") } ?: return@Clicker
                            LegendGuild.debug("${player.name} just clicked a button for joining $guild !")
                            val res = player.join(guild)
                            player.sendMessage(Lang[res.path].mFormat(guild))
                        },
                        (6 row 2)..(6 row 8) to Clicker(UIFactoryPaginated.decorate)
                )
        ) {
            pageOperator
        }
    }
}