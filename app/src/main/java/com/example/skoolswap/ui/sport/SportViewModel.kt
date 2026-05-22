package com.example.skoolswap.ui.sport

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.R
import com.example.skoolswap.domain.model.GearItem
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.SportItem
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SportViewModel @Inject constructor(
    private val productsRepository: ProductsRepositoryInterface  // ← ADD THIS
) : ViewModel() {

    private val _featuredSports = MutableLiveData<List<SportItem>>()
    val featuredSports: LiveData<List<SportItem>> = _featuredSports

    private val _moreSports = MutableLiveData<List<SportItem>>()
    val moreSports: LiveData<List<SportItem>> = _moreSports

    private val _allSportItems = MutableLiveData<List<Item>>()
    val allSportItems: LiveData<List<Item>> = _allSportItems

    private val _gearItems = MutableLiveData<List<GearItem>>()
    val gearItems: LiveData<List<GearItem>> = _gearItems

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadSportData() {
        viewModelScope.launch {
            _isLoading.value = true
            Timber.tag("SportViewModel").d("loadSportData - START")

            // Featured Sports (Grid)
            _featuredSports.value = listOf(
                SportItem(6, "Rugby", R.drawable.ic_rugby),
                SportItem(10, "Soccer", R.drawable.ic_soccer),
                SportItem(7, "Cricket", R.drawable.ic_cricket),
                SportItem(8, "Hockey", R.drawable.ic_hockey)
            )
            Timber.tag("SportViewModel").d("Featured sports set: ${_featuredSports.value?.size}")

            // More Sports (Horizontal scroll - white chips)
            _moreSports.value = listOf(
                SportItem(36, "Tennis", R.drawable.ic_tennis),
                SportItem(35, "Swimming", R.drawable.ic_swimming),
                SportItem(9, "Netball", R.drawable.ic_netball),
                SportItem(37, "Basketball", R.drawable.ic_basketball)
            )
            Timber.tag("SportViewModel").d("More sports set: ${_moreSports.value?.size}")

            // Shop by Gear (Horizontal scroll - black chips)
            _gearItems.value = listOf(
                GearItem(1, "Boots", "boots"),
                GearItem(2, "Jerseys", "jersey"),
                GearItem(3, "Balls", "ball"),
                GearItem(4, "Bats", "bat"),
                GearItem(5, "Equipment", "equipment")
            )

            // ← ADD THIS: Load all sport items from API
            loadAllSportItems()

            _isLoading.value = false
        }
    }

    // Add this function to ShopViewModel
    private fun getSampleItems(): List<Item> {
        return listOf(
            createSampleItem("1", "School Bag", 59.00, 4, "Medium", "Good"),      // Accessories
            createSampleItem("2", "Acer Laptop", 900.00, 4, "15 inch", "Used"),   // Accessories
            createSampleItem("3", "History Book", 200.00, 5, "Paperback", "Good"), // Books
            createSampleItem("4", "Math Textbook", 150.00, 5, "Hardcover", "Like New"), // Books
            createSampleItem("5", "Soccer Ball", 25.00, 2, "Size 5", "Good"),      // Sport
            createSampleItem("6", "Notebook", 45.00, 3, "A4", "New"),              // Stationary
            createSampleItem("7", "Uniform Shirt", 85.00, 1, "Large", "Excellent") // Uniform
        )
    }

    private fun createSampleItem(
        id: String,
        name: String,
        price: Double,
        typeId: Int,
        size: String,
        condition: String
    ): Item {
        return Item(
            id = id,
            shopId = 1L,
            name = name,
            description = "$size, Condition: $condition",
            price = price,
            quantity = 1,
            status = "active",
            createdAt = "",
            itemTypeId = typeId,  // This is key for category filtering!
            images = emptyList()
        )
    }
    private fun loadAllSportItems() {
        viewModelScope.launch {
            Timber.tag("SportViewModel").d("Loading all sport items from API")

            val result = productsRepository.getRecommendedAll(
                page = 1,
                categoryId = 2,  // Sport category ID
                conditionId = null,
                minPrice = null,
                maxPrice = null
            )

            when (result) {
                is Result.Success -> {
                    _allSportItems.value = result.data.items
                    Timber.tag("SportViewModel")
                        .d("All sport items loaded: ${result.data.items.size}")
                }
                is Result.Error -> {
                    Timber.tag("SportViewModel")
                        .e("Failed to load sport items: ${result.exception.message}")
                    _allSportItems.value = emptyList()
                }
            }
        }
    }
}