package ink.ptms.sandalphon

import io.izzel.taboolib.loader.Plugin
import io.izzel.taboolib.module.config.TConfig
import io.izzel.taboolib.module.inject.TInject

object Sandalphon : Plugin(){
    @TInject("settings.yml")
    lateinit var settings: TConfig
        private set
}