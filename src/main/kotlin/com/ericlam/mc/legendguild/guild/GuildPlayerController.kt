package com.ericlam.mc.legendguild.guild

import com.ericlam.mc.kotlib.config.dao.Dao
import java.util.*

class GuildPlayerController(dao: Dao<GuildPlayer, UUID>) : Dao<GuildPlayer, UUID> by dao