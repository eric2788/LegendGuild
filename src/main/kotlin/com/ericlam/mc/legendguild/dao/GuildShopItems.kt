package com.ericlam.mc.legendguild.dao

import com.ericlam.mc.kotlib.config.dao.DataFile
import com.ericlam.mc.kotlib.config.dao.DataResource
import com.ericlam.mc.kotlib.config.dao.ForeignKey
import com.ericlam.mc.kotlib.config.dao.PrimaryKey
import org.bukkit.inventory.ItemStack
import java.util.*

@DataResource(folder = "GuildShopItems")
data class GuildShopItems(
        @PrimaryKey
        @ForeignKey(Guild::class)
        val guild: String,
        val items: MutableMap<UUID, ShopItem> = mutableMapOf()
) : DataFile {
    data class ShopItem(val price: Int, val item: ItemStack, val owner: UUID)


    override fun equals(other: Any?): Boolean {
        return (other as? GuildShopItems)?.let { it.guild == this.guild } ?: false
    }

    override fun hashCode(): Int {
        return guild.hashCode()
    }
}