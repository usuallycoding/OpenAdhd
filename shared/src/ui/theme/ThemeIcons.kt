package com.usuallycoding.openadhd.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun themeIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private fun ImageVector.Builder.filled(pathData: String) {
    addPath(addPathNodes(pathData), fill = SolidColor(Color.Black))
}

private fun ImageVector.Builder.stroked(pathData: String) {
    addPath(
        addPathNodes(pathData),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}

/** A circle with its left half filled, standing for "follow the system". */
val SystemThemeIcon: ImageVector by lazy {
    themeIcon("SystemTheme") {
        stroked("M12,3a9,9 0 1,0 0,18a9,9 0 1,0 0,-18z")
        filled("M12,3a9,9 0 0,0 0,18z")
    }
}

val LightThemeIcon: ImageVector by lazy {
    themeIcon("LightTheme") {
        filled("M12,8a4,4 0 1,0 0,8a4,4 0 1,0 0,-8z")
        stroked("M12,2v3M12,19v3M2,12h3M19,12h3M4.9,4.9l2.1,2.1M17,17l2.1,2.1M4.9,19.1l2.1,-2.1M17,7l2.1,-2.1")
    }
}

val DarkThemeIcon: ImageVector by lazy {
    themeIcon("DarkTheme") {
        filled("M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36 -0.98,1.37 -2.58,2.26 -4.4,2.26 -2.98,0 -5.4,-2.42 -5.4,-5.4 0,-1.81 0.89,-3.42 2.26,-4.4 -0.44,-0.06 -0.9,-0.1 -1.36,-0.1z")
    }
}
