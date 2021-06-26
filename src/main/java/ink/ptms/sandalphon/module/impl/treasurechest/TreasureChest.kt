package ink.ptms.sandalphon.module.impl.treasurechest

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.impl.scriptblock.ScriptBlock
import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestData
import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestInventory
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.module.db.local.LocalFile
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TSchedule
import io.izzel.taboolib.util.item.Items
import io.lumine.xikage.mythicmobs.MythicMobs
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.NumberConversions

object TreasureChest {

    @LocalFile("module/treasurechest.yml")
    lateinit var data: FileConfiguration
        private set

    val chests = ArrayList<ChestData>()

    val isMythicMobsHooked by lazy {
        Bukkit.getPluginManager().getPlugin("MythicMobs") != null
    }

    @TSchedule
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null && !Utils.asgardHook && !Utils.mythicMobsdHook) {
            return
        }
        chests.clear()
        data.getKeys(false).forEach {
            chests.add(ChestData(Utils.toLocation(it.replace("__", "."))).run {
                item.addAll(data.getStringList("$it.item")
                    .map { item -> item.split(" ")[0] to NumberConversions.toInt(item.split(" ")[1]) })
                if (data.contains("$it.link")) {
                    link = Utils.toLocation(data.getString("$it.link")!!.replace("__", "."))
                }
                title = data.getString("$it.title")!!
                random = data.getInt("$it.random.min") to data.getInt("$it.random.max")
                update = data.getLong("$it.update")
                locked = data.getString("$it.locked")!!
                global = data.getBoolean("$it.global")
                globalTime = data.getLong("$it.global-time")
                replace = Items.asMaterial(data.getString("$it.replace"))!!
                condition.addAll(data.getStringList("$it.condition"))
                this
            })
        }
    }

    @TSchedule(period = 20 * 60, async = true)
    fun export() {
        data.getKeys(false).forEach { data.set(it, null) }
        chests.forEach { chest ->
            val location = Utils.fromLocation(chest.block).replace(".", "__")
            if (chest.link != null) {
                data.set("$location.link", Utils.fromLocation(chest.link!!).replace(".", "__"))
            }
            data.set("$location.item", chest.item.map { "${it.first} ${it.second}" })
            data.set("$location.title", chest.title)
            data.set("$location.random.min", chest.random.first)
            data.set("$location.random.max", chest.random.second)
            data.set("$location.update", chest.update)
            data.set("$location.locked", chest.locked)
            data.set("$location.global", chest.global)
            data.set("$location.global-time", chest.globalTime)
            data.set("$location.replace", chest.replace.name)
            data.set("$location.condition", chest.condition)
        }
    }

    @TFunction.Cancel
    fun cancel() {
        Bukkit.getOnlinePlayers().forEach { player ->
            val inventory = player.openInventory.topInventory
            if (inventory.holder is ChestInventory) {
                val chest = (inventory.holder as ChestInventory).chestData
                inventory.filter { item -> Items.nonNull(item) }.forEachIndexed { index, item ->
                    Bukkit.getScheduler().runTaskLater(Sandalphon.plugin, Runnable {
                        CronusUtils.addItem(player as Player, item)
                        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 2f)
                    }, index.toLong())
                }
                inventory.clear()
                chest.globalInventory = null
                chest.globalTime = System.currentTimeMillis() + chest.update
                player.closeInventory()
                // closed animation
                if (chest.replace == Material.CHEST || chest.replace == Material.TRAPPED_CHEST) {
                    ink.ptms.sandalphon.module.api.NMS.HANDLE.sendBlockAction(player, chest.block.block, 1, 0)
                }
            }
        }
        export()
    }

    @TSchedule(period = 20)
    fun tick() {
        chests.forEach { it.tick() }
    }

    fun delete(location: String) {
        ScriptBlock.data.set(location.replace(".", "__"), null)
    }

    fun getChest(block: Block): ChestData? {
        return chests.firstOrNull { it.isBlock(block) }
    }

    fun isGuardianNearly(loc: Location): Boolean {
        if (isMythicMobsHooked) {
            return loc.world!!.getNearbyEntities(loc, 16.0, 16.0, 16.0).any {
                if (it is LivingEntity) {
                    val mob = MythicMobs.inst().mobManager.getMythicMobInstance(it)
                    mob != null && mob.type.config.getStringList("Purtmars.Type").contains("guardian:treasure")
                } else {
                    false
                }
            }
        }
        return false
    }
}