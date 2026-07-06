package com.example.skoolswap.common.constants

object ItemConstants {

    // ============ SUBCATEGORIES (Declare FIRST) ============
    data class Subcategory(val displayName: String, val id: Int)

    val WOMEN_SUBCATEGORIES = listOf(
        Subcategory("Tops for women", 101),
        Subcategory("Dresses", 102),
        Subcategory("Bottoms for women", 103),
        Subcategory("Coats & Jackets for women", 104),
        Subcategory("Shoes for women", 105),
        Subcategory("Activewear for women", 106),
        Subcategory("Accessories for women", 107),
        Subcategory("Underwear & Nightwear", 108),
        Subcategory("Swimwear for women", 109)
    )

    val MEN_SUBCATEGORIES = listOf(
        Subcategory("Tops for men", 201),
        Subcategory("Bottoms for men", 202),
        Subcategory("Coats & Jackets for men", 203),
        Subcategory("Shoes for men", 204),
        Subcategory("Activewear for men", 205),
        Subcategory("Accessories for men", 206),
        Subcategory("Underwear & Nightwear", 207),
        Subcategory("Swimwear for men", 208)
    )

    val KIDS_SUBCATEGORIES = listOf(
        Subcategory("Tops for kids", 301),
        Subcategory("Bottoms for kids", 302),
        Subcategory("Coats & Jackets for kids", 303),
        Subcategory("Shoes for kids", 304),
        Subcategory("Activewear for kids", 305),
        Subcategory("Accessories for kids", 306),
        Subcategory("Schoolwear", 307),
        Subcategory("Swimwear for kids", 308)
    )

    // ============ MAIN CATEGORIES ============
    data class Category(val displayName: String, val id: Int, val subcategories: List<Subcategory>)

    val MAIN_CATEGORIES = listOf(
        Category("Women", 1, WOMEN_SUBCATEGORIES),
        Category("Men", 2, MEN_SUBCATEGORIES),
        Category("Kids", 3, KIDS_SUBCATEGORIES),
        Category("Beauty", 4, emptyList()),
        Category("Home", 5, emptyList()),
        Category("Sports & Leisure", 6, emptyList()),
        Category("Technology", 7, emptyList()),
        Category("Books & Magazines", 8, emptyList()),
        Category("Pets", 9, emptyList()),
        Category("Other", 10, emptyList())
    )

    // ============ CONDITIONS ============
    data class Condition(val displayName: String, val id: Int)

    val CONDITIONS = listOf(
        Condition("New", 1),
        Condition("Used - Like New", 2),
        Condition("Used - Good", 3),
        Condition("Used - Fair", 4)
    )

    // ============ SIZES ============
    data class Size(val displayName: String, val id: Int)

    val SIZES = listOf(
        Size("XS", 1),
        Size("S", 2),
        Size("M", 3),
        Size("L", 4),
        Size("XL", 5)
    )

    // ============ BRANDS ============
    data class Brand(val displayName: String, val id: Int)

    val BRANDS = listOf(
        Brand("Brand A", 1),
        Brand("Brand B", 2),
        Brand("Brand C", 3),
        Brand("Brand D", 4)
    )

    // ============ COLORS ============
    data class Color(val displayName: String, val value: String)

    val COLORS = listOf(
        Color("Red", "red"),
        Color("Blue", "blue"),
        Color("Green", "green"),
        Color("Black", "black"),
        Color("White", "white")
    )

    // ============ MATERIALS ============
    data class Material(val displayName: String, val id: Int)

    val MATERIALS = listOf(
        Material("Cotton", 1),
        Material("Leather", 2),
        Material("Polyester", 3),
        Material("Denim", 4),
        Material("Wool", 5)
    )

    // ============ PROVINCES ============
    data class Province(val displayName: String, val id: Int)

    val PROVINCES = listOf(
        Province("Eastern Cape", 1),
        Province("Free State", 2),
        Province("Gauteng", 3),
        Province("KwaZulu-Natal", 4),
        Province("Limpopo", 5),
        Province("Mpumalanga", 6),
        Province("North West", 7),
        Province("Northern Cape", 8),
        Province("Western Cape", 9)  // ID 9, not 1
    )

    // ============ GENDERS ============
    data class Gender(val displayName: String, val id: Int)

    val GENDERS = listOf(
        Gender("Men", 1),
        Gender("Women", 2),
        Gender("Unisex", 3),
        Gender("Kids - Boys", 4),
        Gender("Kids - Girls", 5)
    )

    // ============ SCHOOLS ============
    data class School(val displayName: String, val id: Int)

    val SCHOOLS = listOf(
        School("University of Cape Town", 1),
        School("University of the Witwatersrand", 2),
        School("Stellenbosch University", 3),
        School("University of Pretoria", 4),
        School("Rhodes University", 5),
        School("University of Johannesburg", 6),
        School("University of KwaZulu-Natal", 7),
        School("University of the Western Cape", 8),
        School("University of South Africa", 9),
        School("Cape Peninsula University of Technology", 10)
    )

    // ============ LOCATIONS (Towns/Cities) ============
    data class Location(val displayName: String, val id: Int, val provinceId: Int)

    val LOCATIONS = listOf(
        // Format: Location(displayName, id, provinceId)
        Location("Gauteng", 1, 1),
        Location("Gauteng", 2, 2),
        Location("Western Cape", 3, 3),
        Location("KwaZulu-Natal", 4, 4),
        Location("Free State", 5, 5)
    )

    // ============ ITEM TYPES (for API) ============
    data class ItemType(val displayName: String, val id: Int, val categoryId: Int)

    val ITEM_TYPES = listOf(
        // Women (Category ID: 1)
        ItemType("Women's Tops", 101, 1),
        ItemType("Women's Dresses", 102, 1),
        ItemType("Women's Bottoms", 103, 1),
        ItemType("Women's Jackets", 104, 1),
        ItemType("Women's Shoes", 105, 1),

        // Men (Category ID: 2)
        ItemType("Men's Tops", 201, 2),
        ItemType("Men's Bottoms", 202, 2),
        ItemType("Men's Jackets", 203, 2),
        ItemType("Men's Shoes", 204, 2),
        ItemType("Men's Accessories", 205, 2),

        // Kids (Category ID: 3)
        ItemType("Kids Tops", 301, 3),
        ItemType("Kids Bottoms", 302, 3),
        ItemType("Kids Jackets", 303, 3),
        ItemType("Kids Shoes", 304, 3),
        ItemType("Kids Schoolwear", 305, 3)
    )

    // ============ RAILS ITEM TYPES (for API - IDs 1-9) ============
    // These match your Rails database item_types table
    val RAILS_ITEM_TYPES = listOf(
        ItemType("Shirt", 1, 1),            // ID: 1
        ItemType("Pants", 2, 2),            // ID: 2
        ItemType("Skirt", 3, 1),            // ID: 3
        ItemType("Blazer", 4, 2),           // ID: 4
        ItemType("Pens", 5, 8),             // ID: 5
        ItemType("Notebooks", 6, 8),        // ID: 6
        ItemType("Textbook", 7, 8),         // ID: 7
        ItemType("Tracksuit", 8, 6),        // ID: 8
        ItemType("Tennis Shoes", 9, 6)      // ID: 9
    )

    // ============ HELPER FUNCTIONS ============

    // Helper function to get Rails item types
    fun getRailsItemTypes(): List<ItemType> {
        return RAILS_ITEM_TYPES
    }

    fun getCategoryId(displayName: String): Int? {
        return MAIN_CATEGORIES.find { it.displayName == displayName }?.id
    }

    fun getSubcategoryId(categoryId: Int, subcategoryDisplayName: String): Int? {
        val category = MAIN_CATEGORIES.find { it.id == categoryId }
        return category?.subcategories?.find { it.displayName == subcategoryDisplayName }?.id
    }

    fun getConditionId(displayName: String): Int? {
        return CONDITIONS.find { it.displayName == displayName }?.id
    }

    fun getSizeId(displayName: String): Int? {
        return SIZES.find { it.displayName == displayName }?.id
    }

    fun getBrandId(displayName: String): Int? {
        return BRANDS.find { it.displayName == displayName }?.id
    }

    fun getProvinceId(displayName: String): Int? {
        return PROVINCES.find { it.displayName == displayName }?.id
    }

    fun getGenderId(displayName: String): Int? {
        return GENDERS.find { it.displayName == displayName }?.id
    }

    fun getSchoolId(displayName: String): Int? {
        return SCHOOLS.find { it.displayName == displayName }?.id
    }

    fun getLocationsByProvince(provinceId: Int): List<Location> {
        return LOCATIONS.filter { it.provinceId == provinceId }
    }

    fun getItemTypeId(categoryId: Int, displayName: String): Int? {
        return ITEM_TYPES.find { it.categoryId == categoryId && it.displayName == displayName }?.id
    }
}