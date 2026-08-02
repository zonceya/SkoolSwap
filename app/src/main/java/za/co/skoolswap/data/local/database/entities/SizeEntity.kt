package za.co.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sizes")
data class SizeEntity(
    @PrimaryKey
    val id: Int,
    val name: String
)