package com.ericlam.mc.legendguild.guild

import com.ericlam.mc.kotlib.config.dao.Dao

class GuildController(dao: Dao<Guild, String>) : Dao<Guild, String> by dao