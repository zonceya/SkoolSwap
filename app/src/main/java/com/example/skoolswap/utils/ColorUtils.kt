package com.example.skoolswap.utils
import com.example.skoolswap.R
// Create this file: ColorMapper.kt
object ColorUtils {

    // Map API color names to your colors.xml resources
    fun getColorResourceId(colorName: String): Int {
        return when (colorName.lowercase()) {
            "red" -> R.color.red
            "blue" -> R.color.blue
            "green" -> R.color.green
            "black" -> R.color.black
            "white" -> R.color.white
            "gold" -> R.color.gold
            "gray", "grey" -> R.color.gray_light
            "yellow" -> R.color.gold  // Use gold for yellow
            "navy" -> R.color.blue_dark  // You'd need to add blue_dark
            "maroon" -> R.color.red_dark
            "brown" -> R.color.brown  // You'd need to add brown
            else -> R.color.gray_light  // Default fallback
        }
    }

    // Alternative: If you want to use hex values directly
    fun getColorHex(colorName: String): String {
        return when (colorName.lowercase()) {
            "red" -> "#FF0000"
            "blue" -> "#0000FF"
            "green" -> "#00FF00"
            "black" -> "#000000"
            "white" -> "#FFFFFF"
            "gold" -> "#FFD700"
            "gray", "grey" -> "#CCCCCC"
            "navy" -> "#000080"
            "maroon" -> "#800000"
            "brown" -> "#A52A2A"
            else -> "#CCCCCC"  // Default gray
        }
    }
}