package za.co.skoolswap.ui.component

import za.co.skoolswap.R
import za.co.skoolswap.domain.model.School
import za.co.skoolswap.domain.repository.SchoolRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SchoolPickerBottomSheet(
    private val provinceId: Int,
    private val townName: String? = null,
    private val onSchoolSelected: (School) -> Unit
) : ApiSearchablePickerBottomSheet<School>(
    title = "Select School",
    placeholder = "Search schools...",
    minSearchLength = 2,
    debounceDelay = 300L
) {

    @Inject
    lateinit var schoolRepository: SchoolRepositoryInterface

    override suspend fun performSearch(query: String): List<School> {
        return try {
            // ✅ Use withContext(NonCancellable) to prevent cancellation
            withContext(NonCancellable) {
                Timber.tag("SchoolPicker").d("🔍 Searching for: $query")

                if (townName != null && townName.isNotBlank()) {
                    Timber.tag("SchoolPicker").d("📍 Searching in town: $townName")
                    val result = schoolRepository.searchSchoolsByTown(
                        provinceId = provinceId,
                        townName = townName,
                        schoolQuery = query
                    )
                    val schools = result.getOrNull() ?: emptyList()
                    Timber.tag("SchoolPicker").d("🏫 Found ${schools.size} schools in town")

                    // ✅ If no results, try province-only search as fallback
                    if (schools.isEmpty()) {
                        Timber.tag("SchoolPicker").d("⚠️ No schools in town, trying province search")
                        val fallbackResult = schoolRepository.searchSchools(provinceId, query)
                        val fallbackSchools = fallbackResult.getOrNull() ?: emptyList()
                        Timber.tag("SchoolPicker").d("🏫 Found ${fallbackSchools.size} schools in province")
                        return@withContext fallbackSchools
                    }
                    return@withContext schools
                } else {
                    // Fallback: search by province only
                    Timber.tag("SchoolPicker").d("📍 No town specified, searching province")
                    val result = schoolRepository.searchSchools(provinceId, query)
                    val schools = result.getOrNull() ?: emptyList()
                    Timber.tag("SchoolPicker").d("🏫 Found ${schools.size} schools in province")
                    return@withContext schools
                }
            }
        } catch (e: CancellationException) {
            // ✅ Don't treat cancellation as error - just return empty
            Timber.tag("SchoolPicker").d("Search cancelled: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Timber.tag("SchoolPicker").e(e, "❌ Search error")
            emptyList()
        }
    }

    override fun getDisplayName(item: School): String = item.name

    override fun onItemSelected(item: School) {
        onSchoolSelected(item)
    }

    override fun getLogoUrl(item: School): String? = item.logoUrl

    override fun hasLogo(item: School): Boolean = !item.logoUrl.isNullOrEmpty()

    override fun getItemLayoutResId(): Int = R.layout.item_school_picker
}