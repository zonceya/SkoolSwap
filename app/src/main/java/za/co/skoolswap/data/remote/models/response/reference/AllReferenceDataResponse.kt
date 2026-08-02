package za.co.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class AllReferenceDataResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: AllReferenceData
)

data class AllReferenceData(
    @SerializedName("main_categories") val mainCategories: List<MainCategoryDto>,
    @SerializedName("colors") val colors: List<ItemColorDto>,
    @SerializedName("sizes") val sizes: List<ItemSizeDto>,
    @SerializedName("brands") val brands: List<BrandDto>,
    @SerializedName("conditions") val conditions: List<ItemConditionDto>,
    @SerializedName("provinces") val provinces: List<ProvinceDto>,
    @SerializedName("towns") val towns: List<TownDto>,
    @SerializedName("schools") val schools: List<SchoolDto>,
    @SerializedName("genders") val genders: List<GenderDto>,
    @SerializedName("tags") val tags: List<TagDto>,
    @SerializedName("locations") val locations: List<LocationDto>
)