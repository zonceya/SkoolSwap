package za.co.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val schoolType: String?,
    val provinceId: Int? = null,
    val locationId: Int? = null,
    val logoUrl: String? = null
)