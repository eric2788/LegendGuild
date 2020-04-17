package com.ericlam.mc.legendguild.config

import com.ericlam.mc.kotlib.config.Resource
import com.ericlam.mc.kotlib.config.dto.ConfigFile
import org.bukkit.inventory.ItemStack

@Resource(locate = "items.yml")
data class Items(val items: MutableMap<String, ItemWrapper>) : ConfigFile() {
    data class ItemWrapper(
            val item: ItemStack
    )
}