package za.co.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genders")
data class GenderEntity(
    @PrimaryKey
    val id: Int,
    val name: String
)