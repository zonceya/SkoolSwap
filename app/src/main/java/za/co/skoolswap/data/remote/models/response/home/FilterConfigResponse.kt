// data/remote/models/response/FilterConfigResponse.kt
package za.co.skoolswap.data.remote.models.response.home

import com.google.gson.annotations.SerializedName

data class FilterConfigResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("category_id") val categoryId: Int? = null,  // Nullable for global filter
    @SerializedName("category_name") val categoryName: String? = null,  // Nullable for global filter
    @SerializedName("filter_groups") val filterGroups: List<FilterGroupResponse>
)

data class FilterGroupResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("filter_type") val filterType: String,
    @SerializedName("options") val options: List<FilterOptionResponse>? = emptyList(),
    @SerializedName("min") val min: Float? = null,
    @SerializedName("max") val max: Float? = null,
    @SerializedName("step") val step: Int? = null
)

data class FilterOptionResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)