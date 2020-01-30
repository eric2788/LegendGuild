package com.ericlam.mc.legendguild.config

import com.ericlam.mc.kotlib.config.Resource
import com.ericlam.mc.kotlib.config.dto.ConfigFile
import com.ericlam.mc.legendguild.dao.GuildPlayer

@Resource(locate = "permissions.yml")
data class Perms(val permissions: Map<GuildPlayer.Role, List<String>>) : ConfigFile()