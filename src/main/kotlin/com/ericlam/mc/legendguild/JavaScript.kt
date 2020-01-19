package com.ericlam.mc.legendguild

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

object JavaScript : ScriptEngine by ScriptEngineManager().getEngineByName("JavaScript")