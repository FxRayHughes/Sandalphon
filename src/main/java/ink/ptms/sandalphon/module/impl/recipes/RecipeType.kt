package ink.ptms.sandalphon.module.impl.recipes

import io.izzel.taboolib.internal.xseries.XMaterial

enum class RecipeType(val id: Char, val display: String, val material: XMaterial) {

    CRAFT_TABLE('1', "合成", XMaterial.CRAFTING_TABLE),

    FURNACE('2', "熔炉", XMaterial.FURNACE),

    BLASTING('3', "高炉", XMaterial.BLAST_FURNACE),

    SMOKING('4', "烟熏炉", XMaterial.SMOKER)
}