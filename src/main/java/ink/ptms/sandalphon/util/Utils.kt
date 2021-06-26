package ink.ptms.sandalphon.util

import com.google.common.base.Enums
import ink.ptms.zaphkiel.ZaphkielAPI
import io.izzel.taboolib.internal.gson.*
import io.izzel.taboolib.kotlin.kether.common.util.LocalizedException
import io.izzel.taboolib.module.inject.TInject
import io.izzel.taboolib.module.locale.TLocale
import io.izzel.taboolib.util.item.Items
import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.items.MythicItem
import me.asgard.sacreditem.SacredItemBuilder
import me.asgard.sacreditem.item.SacredItemManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.NumberConversions
import org.bukkit.util.Vector

object Utils {

    val asgardHook: Boolean
        get() = Bukkit.getPluginManager().isPluginEnabled("SacredItem")

    val mythicMobsdHook: Boolean
        get() = Bukkit.getPluginManager().isPluginEnabled("MythicMobs")

    val serializer = GsonBuilder().excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(
            Vector::class.java,
            JsonSerializer<Vector> { a, _, _ -> JsonPrimitive("${a.x},${a.y},${a.z}") })
        .registerTypeAdapter(
            Vector::class.java,
            JsonDeserializer { a, _, _ ->
                a.asString.split(",").run { Vector(this[0].asDouble(), this[1].asDouble(), this[2].asDouble()) }
            })
        .registerTypeAdapter(Material::class.java, JsonSerializer<Material> { a, _, _ -> JsonPrimitive(a.name) })
        .registerTypeAdapter(Material::class.java, JsonDeserializer { a, _, _ -> Items.asMaterial(a.asString) })
        .registerTypeAdapter(
            Location::class.java,
            JsonSerializer<Location> { a, _, _ -> JsonPrimitive(fromLocation(a)) })
        .registerTypeAdapter(Location::class.java, JsonDeserializer { a, _, _ -> toLocation(a.asString) })
        .registerTypeAdapter(BlockFace::class.java, JsonSerializer<BlockFace> { a, _, _ -> JsonPrimitive(a.name) })
        .registerTypeAdapter(
            BlockFace::class.java,
            JsonDeserializer { a, _, _ -> Enums.getIfPresent(BlockFace::class.java, a.asString).or(BlockFace.SELF) })
        .create()!!

    fun item(item: String, player: Player): ItemStack? {
        if (asgardHook) {
            return SacredItemBuilder.buildItem(player, SacredItemManager.getInstance().getItem(item) ?: return null)
        }
        if (mythicMobsdHook) {
            return MythicMobs.inst().itemManager.getItemStack(item)
        }
        return ZaphkielAPI.getItem(item, player)?.save()
    }

    fun itemId(itemStack: ItemStack): String? {
        if (asgardHook) {
            return SacredItemManager.getInstance().itemList.map {
                TLocale.Translate.setUncolored(it.split("-")[0]).trim()
            }.firstOrNull { itemStack.isSimilar(SacredItemManager.getInstance().getItem(it)) }
        }
        if (mythicMobsdHook) {
            return MythicMobs.inst().itemManager.items.firstOrNull { getItemStack(it) == itemStack }?.internalName
        }
        val itemStream = ZaphkielAPI.read(itemStack)
        if (itemStream.isExtension()) {
            return itemStream.getZaphkielName()
        }
        return null
    }

    fun getItemStack(mythicItem: MythicItem): ItemStack {
        return MythicMobs.inst().itemManager.getItemStack(mythicItem.internalName)
    }

    fun getMythicItem(itemStack: ItemStack): MythicItem? {
        return MythicMobs.inst().itemManager.items.firstOrNull { getItemStack(it) == itemStack }
    }

    fun format(json: JsonElement): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(json)
    }

    fun fromLocation(location: Location): String {
        return "${location.world?.name},${location.x},${location.y},${location.z}"
    }

    fun toLocation(source: String): Location {
        return source.split(",").run {
            Location(
                Bukkit.getWorld(get(0)),
                getOrElse(1) { "0" }.asDouble(),
                getOrElse(2) { "0" }.asDouble(),
                getOrElse(3) { "0" }.asDouble()
            )
        }
    }

    fun String.asDouble(): Double {
        return NumberConversions.toDouble(this)
    }

    fun String.printed(separator: String = ""): List<String> {
        val result = ArrayList<String>()
        var i = 0
        while (i < length) {
            if (get(i) == '§') {
                i++
            } else {
                result.add("${substring(0, i + 1)}${if (i % 2 == 1) separator else ""}")
            }
            i++
        }
        if (separator.isNotEmpty() && i % 2 == 0) {
            result.add(this)
        }
        return result
    }

    fun LocalizedException.print() {
        println("[Sandalphon] Unexpected exception while parsing kether shell:")
        localizedMessage.split("\n").forEach {
            println("[Sandalphon] $it")
        }
    }
}