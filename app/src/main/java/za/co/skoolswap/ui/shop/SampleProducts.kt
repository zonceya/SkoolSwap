package za.co.skoolswap.ui.shop

import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.ItemImage

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
        itemTypeId = 2,  // Sport category
        viewCount = 124,  // ✅ ADD VIEW COUNT
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
        itemTypeId = 2,  // Sport category
        viewCount = 56,   // ✅ ADD VIEW COUNT
        images = listOf(
            ItemImage(
                id = 2,
                url = "https://assets.adidas.com/images/w_600,f_auto,q_auto/6b5b5b5b5b5b5b5b5b5b5b5b/ultraboost-22-running-shoes.jpg"
            )
        )
    ),
    Item(
        id = "sample3",
        shopId = 0L,
        name = "Riverside Primary School Uniform",
        description = "School uniform",
        price = 402.00,
        quantity = 1,
        status = "active",
        mainCategoryId = 1,
        subCategoryId = 1,
        brandId = null,
        sizeId = 5,
        colorId = 2,
        provinceId = 1,
        locationId = 1,
        genderId = 1,
        schoolId = 123,
        itemConditionId = 1,
        createdAt = "",
        itemTypeId = 1,  // Uniform category
        viewCount = 87,   // ✅ ADD VIEW COUNT
        images = listOf(
            ItemImage(
                id = 3,
                url = "https://cdn.skoolswap.co.za/sample/uniform.jpg"
            )
        )
    ),
    Item(
        id = "sample4",
        shopId = 0L,
        name = "St. John's College Badge",
        description = "School badge",
        price = 127.00,
        quantity = 1,
        status = "active",
        mainCategoryId = 1,
        subCategoryId = 1,
        brandId = null,
        sizeId = null,
        colorId = 4,
        provinceId = 1,
        locationId = 1,
        genderId = 1,
        schoolId = 456,
        itemConditionId = 1,
        createdAt = "",
        itemTypeId = 1,  // Uniform category
        viewCount = 203,  // ✅ ADD VIEW COUNT
        images = listOf(
            ItemImage(
                id = 4,
                url = "https://cdn.skoolswap.co.za/sample/badge.jpg"
            )
        )
    ),
    Item(
        id = "sample5",
        shopId = 0L,
        name = "English Textbook - Grade 11",
        description = "School textbook",
        price = 250.00,
        quantity = 1,
        status = "active",
        mainCategoryId = 1,
        subCategoryId = 3,
        brandId = 3,
        sizeId = null,
        colorId = null,
        provinceId = 1,
        locationId = 1,
        genderId = 1,
        schoolId = null,
        itemConditionId = 2,
        createdAt = "",
        itemTypeId = 3,  // Stationary/Books category
        viewCount = 45,   // ✅ ADD VIEW COUNT
        images = listOf(
            ItemImage(
                id = 5,
                url = "https://cdn.skoolswap.co.za/sample/textbook.jpg"
            )
        )
    ),
    Item(
        id = "sample6",
        shopId = 0L,
        name = "Queens High School for Girls Tie",
        description = "School tie",
        price = 155.00,
        quantity = 1,
        status = "active",
        mainCategoryId = 1,
        subCategoryId = 1,
        brandId = null,
        sizeId = null,
        colorId = 5,
        provinceId = 1,
        locationId = 1,
        genderId = 2,  // Girls
        schoolId = 789,
        itemConditionId = 1,
        createdAt = "",
        itemTypeId = 1,  // Uniform category
        viewCount = 312,  // ✅ ADD VIEW COUNT
        images = listOf(
            ItemImage(
                id = 6,
                url = "https://cdn.skoolswap.co.za/sample/tie.jpg"
            )
        )
    ),
    Item(
        id = "sample7",
        shopId = 0L,
        name = "Riverside Primary Socks",
        description = "School socks",
        price = 62.00,
        quantity = 1,
        status = "active",
        mainCategoryId = 1,
        subCategoryId = 1,
        brandId = null,
        sizeId = 8,
        colorId = 1,
        provinceId = 1,
        locationId = 1,
        genderId = 1,
        schoolId = 123,
        itemConditionId = 1,
        createdAt = "",
        itemTypeId = 1,  // Uniform category
        viewCount = 18,   // ✅ ADD VIEW COUNT
        images = listOf(
            ItemImage(
                id = 7,
                url = "https://cdn.skoolswap.co.za/sample/socks.jpg"
            )
        )
    ),
    Item(
        id = "sample8",
        shopId = 0L,
        name = "Science Textbook - Grade 11",
        description = "School science textbook",
        price = 320.00,
        quantity = 1,
        status = "active",
        mainCategoryId = 1,
        subCategoryId = 3,
        brandId = 3,
        sizeId = null,
        colorId = null,
        provinceId = 1,
        locationId = 1,
        genderId = 1,
        schoolId = null,
        itemConditionId = 2,
        createdAt = "",
        itemTypeId = 3,  // Stationary/Books category
        viewCount = 9,    // ✅ ADD VIEW COUNT
        images = listOf(
            ItemImage(
                id = 8,
                url = "https://cdn.skoolswap.co.za/sample/science.jpg"
            )
        )
    )
)