package ink.ptms.sandalphon.module.impl.holographic

import ink.ptms.sandalphon.module.impl.holographic.data.HologramData
import ink.ptms.sandalphon.util.Utils
import ink.ptms.sandalphon.util.Utils.asDouble
import io.izzel.taboolib.module.db.local.LocalFile
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TSchedule
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration

/**
 * @author sky
 * @since 2020-05-27 11:17
 */
object Hologram {

    @LocalFile("module/hologram.yml")
    lateinit var data: FileConfiguration
        private set

    val holograms = ArrayList<HologramData>()

    fun add(id: String, location: String, info: MutableList<String>, check: MutableList<String>) {
        holograms.add(HologramData(id, toLocation(location), info, check))
    }

    fun toLocation(source: String): Location {
        return source.replace("__", ".").split(",").run {
            Location(
                Bukkit.getWorld(get(0)),
                getOrElse(1) { "0" }.asDouble(),
                getOrElse(2) { "0" }.asDouble(),
                getOrElse(3) { "0" }.asDouble()
            )
        }
    }

    @TSchedule
    fun import() {
        holograms.clear()
        data.getKeys(false).forEach {
            holograms.add(HologramData(it,
                Utils.toLocation(data.getString("$it.location")!!),
                data.getStringList("$it.content"),
                data.getStringList("$it.condition")))
        }
    }

    @TFunction.Cancel
    fun export() {
        data.getKeys(false).forEach { data.set(it, null) }
        holograms.forEach { holo ->
            data.set("${holo.id}.location", Utils.fromLocation(holo.location))
            data.set("${holo.id}.content", holo.content)
            data.set("${holo.id}.condition", holo.condition)
        }
    }

    @TFunction.Cancel
    fun cancel() {
        holograms.forEach { it.cancel() }
    }

    @TSchedule(period = 20)
    fun e() {
        Bukkit.getOnlinePlayers().forEach { player ->
            holograms.filter { it.location.world?.name == player.world.name }.forEach {
                it.refresh(player)
            }
        }
    }

    fun delete(id: String) {
        data.set(id, null)
    }

    fun getHologram(id: String): HologramData? {
        return holograms.firstOrNull { it.id == id }
    }
}