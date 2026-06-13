package com.example.skoolswap.ui.shop

import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage

val sampleItems = listOf(
    Item(
        id = "sample1",
        shopId = 0L,
        name = "Nike Air Max 270",
        description = "Comfortable running shoes",
        price = 1899.00,
        quantity = 1,
        status = "active",
        mainCategoryId = 1,
        subCategoryId = 2,
        brandId = 1,
        sizeId = 9,
        colorId = 3,
        provinceId = 1,
        locationId = 1,
        genderId = 1,
        schoolId = null,
        itemConditionId = 1,
        createdAt = "",
        itemTypeId = 2,  // ✅ ADD THIS - maps to "Sport" category
        images = listOf(
            ItemImage(
                id = 1,
                url = "https://static.nike.com/a/images/t_PDP_1280_v1/f_auto,q_auto:eco/8416e8a6-3e6e-4b1e-9b5a-2b5f5b5b5b5b/air-max-270-mens-shoe.jpg"
            )
        )
    ),
    Item(
        id = "sample2",
        shopId = 0L,
        name = "Adidas Ultraboost 22",
        description = "Premium running shoes",
        price = 2299.00,
        quantity = 0,
        status = "sold",
        mainCategoryId = 1,
        subCategoryId = 2,
        brandId = 2,
        sizeId = 9,
        colorId = 1,
        provinceId = 1,
        locationId = 1,
        genderId = 1,
        schoolId = null,
        itemConditionId = 1,
        createdAt = "",
        itemTypeId = 2,  // ✅ ADD THIS
        images = listOf(
            ItemImage(
                id = 2,
                url = "https://assets.adidas.com/images/w_600,f_auto,q_auto/6b5b5b5b5b5b5b5b5b5b5b5b/ultraboost-22-running-shoes.jpg"
            )
        )
    ),
    // Add itemTypeId = 2 to sample3 and sample4 as well
)