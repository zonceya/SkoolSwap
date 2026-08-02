package za.co.skoolswap.utils

import android.graphics.Color

object ColorUtils {

    // Your existing method
    fun getColorHex(colorName: String): String {
        return when (colorName.lowercase()) {
            "black" -> "#000000"
            "white" -> "#FFFFFF"
            "navy", "navy blue" -> "#000080"
            "red" -> "#FF0000"
            "blue" -> "#0000FF"
            "grey" -> "#808080"
            "maroon" -> "#800000"
            "royal blue" -> "#4169E1"
            "sky blue" -> "#87CEEB"
            "charcoal" -> "#36454F"
            "khaki" -> "#C3B091"
            "brown" -> "#8B4513"
            "burgundy" -> "#800020"
            "green" -> "#008000"
            "forest green" -> "#228B22"
            "olive" -> "#808000"
            "yellow" -> "#FFFF00"
            "gold" -> "#FFD700"
            "orange" -> "#FFA500"
            "pink" -> "#FFC0CB"
            "purple" -> "#800080"
            "teal" -> "#008080"
            "turquoise" -> "#40E0D0"
            "multicolor" -> "#FF00FF"
            else -> "#CCCCCC" // Default grey
        }
    }

    // Add this method to get color as Int
    fun getColorInt(colorName: String): Int {
        return try {
            Color.parseColor(getColorHex(colorName))
        } catch (e: Exception) {
            Color.parseColor("#CCCCCC")
        }
    }
}