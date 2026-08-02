package za.co.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "towns",
    indices = [Index(value = ["provinceId"])]
)
data class TownEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val provinceId: Int
)