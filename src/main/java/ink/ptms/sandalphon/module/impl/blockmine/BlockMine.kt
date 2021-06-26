package ink.ptms.sandalphon.module.impl.blockmine

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockData
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockState
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockStructure
import ink.ptms.sandalphon.module.impl.holographic.Hologram
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.module.event.EventCancellable
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TSchedule
import io.izzel.taboolib.util.Files
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import java.io.File
import java.nio.charset.StandardCharsets

object BlockMine {

    val blocks = ArrayList<BlockData>()
    val blocksCache = HashSet<Material>()

    @TSchedule(period = 20)
    fun e() {
        blocks.forEach { it.grow() }
    }

    @Suppress("UNCHECKED_CAST")
    @TSchedule(delay = 1)
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null && !Utils.asgardHook) {
            return
        }
        blocks.clear()
        Files.folder(Sandalphon.plugin.dataFolder, "module/blockmine").listFiles()?.map { file ->
            if (file.name.endsWith(".json")) {
                val block = Utils.serializer.fromJson(file.readText(StandardCharsets.UTF_8), BlockData::class.java)
                blocks.add(block)
                block.blocks.toList().forEach { state ->
                    val location = state.location.clone().add(Sandalphon.settings.getDouble("BlockMine.x"),
                        Sandalphon.settings.getDouble("BlockMine.y"),
                        Sandalphon.settings.getDouble("BlockMine.z"))
                    val info = Sandalphon.settings.getStringListColored("BlockMine.HD")
                        .map {
                            it.replace("{Type}", block.id)
                        }
                    Hologram.add("BlockMine-${fromLocation(location)}",
                        fromLocation(location),
                        info.toMutableList(),
                        mutableListOf())
                    Hologram.export()
                }
            }
        }
        cached()
    }

    fun fromLocation(location: Location): String {
        return "${location.world?.name},${location.x},${location.y},${location.z}".replace(".", "__")
    }

    @TFunction.Cancel
    @TSchedule(period = 20 * 60, async = true)
    fun export() {
        blocks.forEach { block ->
            Files.file(Sandalphon.plugin.dataFolder, "module/blockmine/${block.id}.json")
                .writeText(Utils.format(Utils.serializer.toJsonTree(block)), StandardCharsets.UTF_8)
        }
    }

    fun cached() {
        blocksCache.clear()
        blocks.forEach { b ->
            b.progress.forEach { p ->
                p.structures.forEach {
                    blocksCache.add(it.origin)
                    blocksCache.add(it.replace)
                }
            }
        }
    }

    fun delete(id: String) {
        Files.deepDelete(File(Sandalphon.plugin.dataFolder, "module/blockmine/$id.json"))
    }

    fun find(block: Location): FindResult? {
        if (block.block.type !in blocksCache) {
            return null
        }
        blocks.forEach { b ->
            val find = b.find(block)
            if (find != null) {
                return FindResult(b, find.first, find.second)
            }
        }
        return null
    }

    fun getBlock(id: String): BlockData? {
        return blocks.firstOrNull { it.id == id }
    }

    class FindResult(val blockData: BlockData, val blockState: BlockState, val blockStructure: BlockStructure)
}