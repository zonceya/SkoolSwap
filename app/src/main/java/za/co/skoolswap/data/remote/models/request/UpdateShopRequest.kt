package za.co.skoolswap.data.remote.models.request


data class UpdateShopRequest(
    val shop: UpdateShopData
)

data class UpdateShopData(
    val display_name: String
)