package com.ericlam.mc.legendguild.config

import com.ericlam.mc.kotlib.config.Prefix
import com.ericlam.mc.kotlib.config.Resource
import com.ericlam.mc.kotlib.config.dto.MessageFile

@Resource(locate = "lang.yml")
@Prefix(path = "prefix")
class LangFile : MessageFile()