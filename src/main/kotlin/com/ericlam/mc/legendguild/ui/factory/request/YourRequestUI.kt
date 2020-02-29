package com.ericlam.mc.legendguild.ui.factory.request

import com.ericlam.mc.legendguild.ui.UIManager
import com.ericlam.mc.legendguild.ui.factory.UIFactory
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

object YourRequestUI : UIFactory {
    override fun getUI(bPlayer: OfflinePlayer): Inventory? {
        return invCaches[bPlayer] ?: let {
            UIManager.p.createGUI(1, "&a你的委託內容") {
                mapOf(

                )
            }
        }
    }


    override val invCaches: MutableMap<OfflinePlayer, Inventory>
        get() = ConcurrentHashMap()
}