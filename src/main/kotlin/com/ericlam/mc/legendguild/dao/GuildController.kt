package com.ericlam.mc.legendguild.dao

import com.ericlam.mc.kotlib.config.dao.Dao

class GuildController(dao: Dao<Guild, String>) : Dao<Guild, String> by dao