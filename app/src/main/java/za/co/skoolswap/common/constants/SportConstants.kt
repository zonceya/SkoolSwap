package za.co.skoolswap.common.constants


import za.co.skoolswap.domain.model.GearItem
import za.co.skoolswap.domain.model.SportItem
import za.co.skoolswap.R

object SportConstants {

    // All available sports with their IDs and icons
    val ALL_SPORTS = listOf(
        SportItem(6, "Rugby", R.drawable.ic_rugby),
        SportItem(10, "BasketBall", R.drawable.ic_basketball),
        SportItem(7, "Cricket", R.drawable.ic_cricket),
        SportItem(8, "Netball", R.drawable.ic_netball),
        SportItem(36, "Soccer", R.drawable.ic_soccer),
        SportItem(35, "Swimming", R.drawable.ic_swimming),
        SportItem(9, "Hockey", R.drawable.ic_hockey),
        SportItem(37, "Tennis", R.drawable.ic_tennis)
    )

    // Featured Sports Set 1
    val FEATURED_SET_1 = listOf(
        SportItem(6, "Rugby", R.drawable.ic_rugby),
        SportItem(10, "Basketball", R.drawable.ic_basketball),
        SportItem(7, "Cricket", R.drawable.ic_cricket),
        SportItem(8, "Netball", R.drawable.ic_netball)
    )

    // Featured Sports Set 2 (Alternate)
    val FEATURED_SET_2 = listOf(
        SportItem(37, "Soccer", R.drawable.ic_soccer),
        SportItem(35, "Swimming", R.drawable.ic_swimming),
        SportItem(36, "Tennis", R.drawable.ic_tennis),
        SportItem(9, "Hockey", R.drawable.ic_hockey)
    )

    // More Sports Set 1 (Text chips)
    val MORE_SET_1 = listOf(
        SportItem(36, "Tennis", R.drawable.ic_soccer),
        SportItem(35, "Swimming", R.drawable.ic_swimming),
        SportItem(9, "Netball", R.drawable.ic_hockey),
        SportItem(37, "Basketball", R.drawable.ic_tennis)
    )

    // More Sports Set 2 (Alternate text chips)
    val MORE_SET_2 = listOf(
        SportItem(6, "Rugby", R.drawable.ic_rugby),
        SportItem(10, "Soccer", R.drawable.ic_basketball),
        SportItem(7, "Cricket", R.drawable.ic_cricket),
        SportItem(8, "Hockey", R.drawable.ic_netball)
    )

    // Gear items
    val GEAR_ITEMS = listOf(
        GearItem(1, "Boots", "boots"),
        GearItem(2, "Jerseys", "jersey"),
        GearItem(3, "Balls", "ball"),
        GearItem(4, "Bats", "bat"),
        GearItem(5, "Equipment", "equipment")
    )
}