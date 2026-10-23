package com.usuallycoding.openadhd.ui.theme

import androidx.compose.ui.graphics.vector.ImageVector

val TodayIcon: ImageVector by lazy {
    themeIcon("Today") {
        stroked("M12,3a9,9 0 1,0 0,18a9,9 0 1,0 0,-18z")
        stroked("M8,12l3,3l5,-6")
    }
}

val TemplatesIcon: ImageVector by lazy {
    themeIcon("Templates") { stroked("M4,6h16M4,12h16M4,18h10") }
}

val AddIcon: ImageVector by lazy {
    themeIcon("Add") { stroked("M12,5v14M5,12h14") }
}

val EditIcon: ImageVector by lazy {
    themeIcon("Edit") { stroked("M4,20l4,-1l11,-11l-3,-3l-11,11z") }
}

val DeleteIcon: ImageVector by lazy {
    themeIcon("Delete") { stroked("M5,7h14M10,7V4h4v3M7,7l1,13h8l1,-13") }
}

val CoinIcon: ImageVector by lazy {
    themeIcon("Coin") {
        stroked("M12,3a9,9 0 1,0 0,18a9,9 0 1,0 0,-18z")
        filled("M12,8a4,4 0 1,0 0,8a4,4 0 1,0 0,-8z")
    }
}

val ShopIcon: ImageVector by lazy {
    themeIcon("Shop") {
        stroked("M4,10h16v10H4zM3,6h18v4H3zM12,6v14")
        stroked("M12,6c-2,0 -4,-3 -2,-3c1.5,0 2,1.5 2,3c0,-1.5 0.5,-3 2,-3c2,0 0,3 -2,3")
    }
}
