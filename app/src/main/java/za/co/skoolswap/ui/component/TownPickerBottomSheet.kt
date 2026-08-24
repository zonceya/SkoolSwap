package za.co.skoolswap.ui.component


import za.co.skoolswap.domain.repository.ReferenceDataRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import za.co.skoolswap.domain.model.reference.Town
import javax.inject.Inject

@AndroidEntryPoint
class TownPickerBottomSheet(
    private val provinceId: Int,
    private val onTownSelected: (Town) -> Unit
) : ApiSearchablePickerBottomSheet<Town>(
    title = "Select Town",
    placeholder = "Search Town...",
    minSearchLength = 2,
    debounceDelay = 300L
) {

    @Inject
    lateinit var referenceRepository: ReferenceDataRepositoryInterface

    override suspend fun performSearch(query: String): List<Town> {
        val result = referenceRepository.searchTowns(provinceId, query)
        return (result.getOrNull() ?: emptyList()) as List<Town>
    }

    override fun getDisplayName(item: Town): String = item.name

    override fun onItemSelected(item: Town) {
        onTownSelected(item)
    }

    // No logos for towns
    override fun hasLogo(item: Town): Boolean = false
}