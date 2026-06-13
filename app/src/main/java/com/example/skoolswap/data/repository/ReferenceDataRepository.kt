package com.example.skoolswap.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.skoolswap.data.local.database.dao.*
import com.example.skoolswap.data.local.database.entities.BrandEntity
import com.example.skoolswap.data.local.database.entities.ColorEntity
import com.example.skoolswap.data.local.database.entities.ConditionEntity
import com.example.skoolswap.data.local.database.entities.GenderEntity
import com.example.skoolswap.data.local.database.entities.LocationEntity
import com.example.skoolswap.data.local.database.entities.ProvinceEntity
import com.example.skoolswap.data.local.database.entities.SchoolEntity
import com.example.skoolswap.data.local.database.entities.SizeEntity
import com.example.skoolswap.data.local.database.entities.SubCategoryEntity
import com.example.skoolswap.data.local.database.entities.TagEntity
import com.example.skoolswap.data.local.database.entities.TownEntity
import com.example.skoolswap.data.mapper.ReferenceDataMapper
import com.example.skoolswap.data.remote.api.ReferenceDataApiService
import com.example.skoolswap.data.remote.models.response.reference.AllReferenceData
import com.example.skoolswap.domain.model.reference.*
import com.example.skoolswap.domain.repository.ReferenceDataRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceDataRepository @Inject constructor(
    private val referenceApiService: ReferenceDataApiService,
    private val authRepository: AuthRepository,
    private val mainCategoryDao: MainCategoryDao,
    private val subCategoryDao: SubCategoryDao,
    private val colorDao: ColorDao,
    private val sizeDao: SizeDao,
    private val brandDao: BrandDao,
    private val conditionDao: ConditionDao,
    private val provinceDao: ProvinceDao,
    private val townDao: TownDao,
    private val schoolDao: SchoolDao,
    private val genderDao: GenderDao,
    private val tagDao: TagDao,
    private val locationDao: LocationDao,
    private val sharedPreferences: SharedPreferences  // Add this
) : ReferenceDataRepositoryInterface {

    private val refreshMutex = Mutex()
    private val refreshFlags = mutableMapOf<String, Boolean>()

    // Cache duration: 24 hours
    private val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L

    // ============ CACHE HELPERS ============
    // In ReferenceDataRepository.kt
    private suspend fun isCacheValid(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            val lastSync = sharedPreferences.getLong("last_sync_$key", 0)
            val isValid = System.currentTimeMillis() - lastSync < CACHE_DURATION_MS

            // ALSO check if database has data
            val hasData = when (key) {
                "all_reference_data" -> mainCategoryDao.getCount() > 0
                else -> true
            }

            val cacheValid = isValid && hasData
            Log.d("ReferenceDataRepo", "Cache for $key is ${if (cacheValid) "valid" else "invalid"} (isValid=$isValid, hasData=$hasData)")
            cacheValid
        }
    }

    private suspend fun updateCacheTimestamp(key: String) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit().putLong("last_sync_$key", System.currentTimeMillis()).apply()
            Log.d("ReferenceDataRepo", "Updated cache timestamp for $key")
        }
    }

    // ============ RETRY HELPER ============
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        block: suspend () -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelayMs
        repeat(maxRetries - 1) { attempt ->
            val result = block()
            if (result.isSuccess) {
                return result
            }
            Log.w("ReferenceDataRepo", "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms")
            delay(currentDelay)
            currentDelay *= 2
        }
        return block()
    }

    // ============ HELPER METHODS ============
    private suspend fun <T> performRefresh(
        key: String,
        fetchFromApi: suspend () -> Result<List<T>>,
        saveToDb: suspend (List<T>) -> Unit
    ): Result<Unit> {
        return refreshMutex.withLock {
            if (refreshFlags[key] == true) {
                Log.d("ReferenceDataRepo", "Already refreshing $key, skipping")
                return Result.success(Unit)
            }

            refreshFlags[key] = true
            try {
                val result = performRefreshInternal(fetchFromApi, saveToDb)
                refreshFlags[key] = false
                result
            } catch (e: Exception) {
                refreshFlags[key] = false
                throw e
            }
        }
    }

    private suspend fun <T> performRefreshInternal(
        fetchFromApi: suspend () -> Result<List<T>>,
        saveToDb: suspend (List<T>) -> Unit
    ): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val authToken = getAuthToken()
                if (authToken == null) {
                    return@withContext Result.failure(Exception("User not authenticated"))
                }

                val result = fetchFromApi()
                result.onSuccess { items ->
                    saveToDb(items)
                    Log.d("ReferenceDataRepo", "Saved ${items.size} items")
                }.map { Unit }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getAuthToken(): String? {
        return try {
            authRepository.getAuthToken().value
        } catch (e: Exception) {
            null
        }
    }

    // ============ MAIN CATEGORIES ============
    override fun getMainCategories(): Flow<List<MainCategory>> {
        return mainCategoryDao.getAll().map { entities ->
            entities.map { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshMainCategories(): Result<Unit> {
        return performRefresh(
            key = "main_categories",
            fetchFromApi = {
                val response = referenceApiService.getMainCategories()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        Result.success(body.categories ?: emptyList())
                    } else {
                        Result.failure(Exception("API returned success=false"))
                    }
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { ReferenceDataMapper.toEntity(it) }
                mainCategoryDao.clearAll()
                mainCategoryDao.insertAll(entities)
            }
        )
    }

    override suspend fun getMainCategoryById(id: Int): MainCategory? {
        return withContext(Dispatchers.IO) {
            mainCategoryDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ BULK REFRESH (SINGLE VERSION) ============
    override suspend fun refreshAllReferenceDataBulk(forceRefresh: Boolean): Result<Unit> {
        // Check cache first (unless force refresh)
        if (!forceRefresh && isCacheValid("all_reference_data")) {
            Log.d("ReferenceDataRepo", "✅ Using cached data, no network call needed")
            return Result.success(Unit)
        }

        Log.d("ReferenceDataRepo", "🔄 Cache expired or force refresh, fetching from network")

        return retryWithBackoff(maxRetries = 3) {
            performBulkRefresh()
        }
    }

    private suspend fun performBulkRefresh(): Result<Unit> {
        val key = "all_reference_data_bulk"

        return refreshMutex.withLock {
            if (refreshFlags[key] == true) {
                Log.d("ReferenceDataRepo", "Already refreshing all data, skipping")
                return Result.success(Unit)
            }

            refreshFlags[key] = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val authToken = getAuthToken()
                    if (authToken == null) {
                        return@withContext Result.failure(Exception("User not authenticated"))
                    }

                    val response = referenceApiService.getAllReferenceData()

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true && body.data != null) {
                            saveAllReferenceData(body.data)
                            updateCacheTimestamp("all_reference_data")
                            Log.d("ReferenceDataRepo", "✅ Successfully saved all reference data")
                            Result.success(Unit)
                        } else {
                            Result.failure(Exception("API returned success=false or null data"))
                        }
                    } else {
                        Result.failure(Exception("API call failed: ${response.code()}"))
                    }
                }
                refreshFlags[key] = false
                result
            } catch (e: Exception) {
                refreshFlags[key] = false
                Result.failure(e)
            }
        }
    }

    // Helper function to save all data
    private suspend fun saveAllReferenceData(data: AllReferenceData) {
        Log.d("ReferenceRepo", "💾 Saving ALL reference data to database")

        // Save main categories
        val mainCategoryEntities = data.mainCategories.map {
            ReferenceDataMapper.toEntity(it)
        }
        mainCategoryDao.clearAll()
        mainCategoryDao.insertAll(mainCategoryEntities)

        // Save sub categories
        val allSubCategoryEntities = mutableListOf<SubCategoryEntity>()

        data.mainCategories.forEach { mainCategory ->
            val subCategories = mainCategory.subCategories ?: emptyList()

            val subCategoryEntities = subCategories.map { subCategory ->
                SubCategoryEntity(
                    id = subCategory.id,
                    name = subCategory.name,
                    description = subCategory.description,
                    mainCategoryId = mainCategory.id,
                    displayOrder = subCategory.displayOrder,
                    isActive = true
                )
            }
            allSubCategoryEntities.addAll(subCategoryEntities)
            Log.d("ReferenceRepo", "📦 Found ${subCategoryEntities.size} subcategories for ${mainCategory.name}")
        }

        if (allSubCategoryEntities.isNotEmpty()) {
            subCategoryDao.clearAll()
            subCategoryDao.insertAll(allSubCategoryEntities)
            Log.d("ReferenceRepo", "✅ Saved ${allSubCategoryEntities.size} subcategories")
        } else {
            Log.w("ReferenceRepo", "⚠️ No subcategories found in bulk data!")
        }

        // Save colors
        val colorEntities = data.colors.map {
            ColorEntity(it.id, it.name)
        }
        colorDao.clearAll()
        colorDao.insertAll(colorEntities)

        // Save sizes
        val sizeEntities = data.sizes.map {
            SizeEntity(it.id, it.name)
        }
        sizeDao.clearAll()
        sizeDao.insertAll(sizeEntities)

        // Save brands
        val brandEntities = data.brands.map {
            BrandEntity(it.id, it.name)
        }
        brandDao.clearAll()
        brandDao.insertAll(brandEntities)

        // Save conditions
        val conditionEntities = data.conditions.map {
            ConditionEntity(it.id, it.name, it.description)
        }
        conditionDao.clearAll()
        conditionDao.insertAll(conditionEntities)

        // Save provinces
        val provinceEntities = data.provinces.map {
            ProvinceEntity(it.id, it.name)
        }
        provinceDao.clearAll()
        provinceDao.insertAll(provinceEntities)

        // Save towns
        val townEntities = data.towns.map {
            TownEntity(it.id, it.name, it.provinceId)
        }
        townDao.clearAll()
        townDao.insertAll(townEntities)

        // Save schools
        val schoolEntities = data.schools.map {
            SchoolEntity(it.id, it.name, "unknown", it.province?.id ?: 0)
        }
        schoolDao.clearAll()
        schoolDao.insertAll(schoolEntities)

        // Save genders
        val genderEntities = data.genders.map {
            GenderEntity(it.id, it.name)
        }
        genderDao.clearAll()
        genderDao.insertAll(genderEntities)

        // Save tags
        val tagEntities = data.tags.map {
            TagEntity(it.id, it.name, it.tagType ?: "unknown")
        }
        tagDao.clearAll()
        tagDao.insertAll(tagEntities)

        // Save locations
        val locationEntities = data.locations.map {
            LocationEntity(
                id = it.id,
                province = it.province,
                stateOrRegion = it.stateOrRegion,
                country = it.country,
                townId = it.townId
            )
        }
        locationDao.clearAll()
        locationDao.insertAll(locationEntities)

        Log.d("ReferenceRepo", "✅ Saved ALL reference data successfully")
    }

    // ============ SUB CATEGORIES ============
    override fun getSubCategories(mainCategoryId: Int?): Flow<List<SubCategory>> {
        return if (mainCategoryId != null) {
            subCategoryDao.getByMainCategoryId(mainCategoryId).map { entities ->
                Log.d("ReferenceRepo", "📦 Loading ${entities.size} subcategories from DB for mainCategoryId: $mainCategoryId")
                entities.map { entity ->
                    ReferenceDataMapper.toDomain(entity)
                }
            }
        } else {
            subCategoryDao.getAll().map { entities ->
                Log.d("ReferenceRepo", "📦 Loading ${entities.size} subcategories from DB (all)")
                entities.map { entity ->
                    ReferenceDataMapper.toDomain(entity)
                }
            }
        }
    }

    override suspend fun refreshSubCategories(mainCategoryId: Int?): Result<Unit> {
        val key = if (mainCategoryId != null) "sub_categories_$mainCategoryId" else "sub_categories_all"
        return performRefresh(
            key = key,
            fetchFromApi = {
                Log.d("ReferenceRepo", "📡 Fetching subcategories for mainCategoryId: $mainCategoryId")
                val response = if (mainCategoryId != null) {
                    referenceApiService.getSubCategories(mainCategoryId)
                } else {
                    return@performRefresh Result.failure(Exception("mainCategoryId is required"))
                }
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        val subCategories = body.subCategories ?: emptyList()
                        Log.d("ReferenceRepo", "✅ API returned ${subCategories.size} subcategories")
                        Result.success(subCategories)
                    } else {
                        Result.failure(Exception(body?.error ?: "Unknown error"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} subcategories to database")
                val entities = dtos.map { dto ->
                    ReferenceDataMapper.toEntity(dto, mainCategoryId)
                }
                if (mainCategoryId != null) {
                    subCategoryDao.deleteByMainCategoryId(mainCategoryId)
                }
                subCategoryDao.insertAll(entities)
            }
        )
    }

    override suspend fun getSubCategoryById(id: Int): SubCategory? {
        return withContext(Dispatchers.IO) {
            subCategoryDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ COLORS ============
    override fun getColors(): Flow<List<Color>> {
        return colorDao.getAll().map { entities ->
            entities.map { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshColors(): Result<Unit> {
        return performRefresh(
            key = "colors",
            fetchFromApi = {
                val response = referenceApiService.getItemColors()
                if (response.isSuccessful) {
                    val colors = response.body()
                    if (colors != null) Result.success(colors)
                    else Result.failure(Exception("Response body was null"))
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { ColorEntity(it.id, it.name) }
                colorDao.clearAll()
                colorDao.insertAll(entities)
            }
        )
    }

    override suspend fun getColorById(id: Int): Color? {
        return withContext(Dispatchers.IO) {
            colorDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ SIZES ============
    override fun getSizes(): Flow<List<Size>> {
        return sizeDao.getAll().map { entities ->
            entities.map { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshSizes(): Result<Unit> {
        return performRefresh(
            key = "sizes",
            fetchFromApi = {
                val response = referenceApiService.getItemSizes()
                if (response.isSuccessful) {
                    val sizes = response.body()
                    if (sizes != null) Result.success(sizes)
                    else Result.failure(Exception("Response body was null"))
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { SizeEntity(it.id, it.name) }
                sizeDao.clearAll()
                sizeDao.insertAll(entities)
            }
        )
    }

    override suspend fun getSizeById(id: Int): Size? {
        return withContext(Dispatchers.IO) {
            sizeDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ BRANDS ============
    override fun getBrands(): Flow<List<Brand>> {
        return brandDao.getAll().map { entities ->
            entities.map { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshBrands(): Result<Unit> {
        return performRefresh(
            key = "brands",
            fetchFromApi = {
                val response = referenceApiService.getBrands()
                if (response.isSuccessful) {
                    val brands = response.body()
                    if (brands != null) Result.success(brands)
                    else Result.failure(Exception("Response body was null"))
                } else {
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { BrandEntity(it.id, it.name) }
                brandDao.clearAll()
                brandDao.insertAll(entities)
            }
        )
    }

    override suspend fun getBrandById(id: Int): Brand? {
        return withContext(Dispatchers.IO) {
            brandDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ CONDITIONS ============
    override fun getConditions(): Flow<List<Condition>> {
        return conditionDao.getAll().map { entities ->
            entities.map { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshConditions(): Result<Unit> {
        return performRefresh(
            key = "conditions",
            fetchFromApi = {
                val response = referenceApiService.getItemConditions()
                if (response.isSuccessful) {
                    val conditions = response.body()
                    if (conditions != null) Result.success(conditions)
                    else Result.failure(Exception("Response body was null"))
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { ConditionEntity(it.id, it.name, it.description) }
                conditionDao.clearAll()
                conditionDao.insertAll(entities)
            }
        )
    }

    override suspend fun getConditionById(id: Int): Condition? {
        return withContext(Dispatchers.IO) {
            conditionDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ PROVINCES ============
    override fun getProvinces(): Flow<List<Province>> {
        return provinceDao.getAll().map { entities ->
            entities.map { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshProvinces(): Result<Unit> {
        return performRefresh(
            key = "provinces",
            fetchFromApi = {
                val response = referenceApiService.getProvinces()
                if (response.isSuccessful) {
                    val provinces = response.body()
                    if (provinces != null) Result.success(provinces)
                    else Result.failure(Exception("Response body was null"))
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { ProvinceEntity(it.id, it.name) }
                provinceDao.clearAll()
                provinceDao.insertAll(entities)
            }
        )
    }

    override suspend fun getProvinceById(id: Int): Province? {
        return withContext(Dispatchers.IO) {
            provinceDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ TOWNS ============
    override fun getTowns(provinceId: Int?): Flow<List<Town>> {
        return if (provinceId != null) {
            townDao.getByProvinceId(provinceId).map { entities ->
                entities.map { ReferenceDataMapper.toDomain(it) }
            }
        } else {
            townDao.getAll().map { entities ->
                entities.map { ReferenceDataMapper.toDomain(it) }
            }
        }
    }

    override suspend fun refreshTowns(provinceId: Int?): Result<Unit> {
        return performRefresh(
            key = "towns_$provinceId",
            fetchFromApi = {
                if (provinceId == null) return@performRefresh Result.failure(Exception("provinceId is required"))
                val response = referenceApiService.getTowns(provinceId)
                if (response.isSuccessful) {
                    val towns = response.body()
                    if (towns != null) Result.success(towns)
                    else Result.failure(Exception("Response body was null"))
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { TownEntity(it.id, it.name, it.provinceId) }
                if (provinceId != null) townDao.deleteByProvinceId(provinceId)
                townDao.insertAll(entities)
            }
        )
    }

    override suspend fun getTownById(id: Int): Town? {
        return withContext(Dispatchers.IO) {
            townDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ SCHOOLS ============
    override fun getSchools(): Flow<List<School>> {
        return schoolDao.getAll().map { entities ->
            entities.map { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshSchools(): Result<Unit> {
        return performRefresh(
            key = "schools",
            fetchFromApi = {
                val response = referenceApiService.getSchools()
                if (response.isSuccessful) {
                    val schools = response.body()
                    if (schools != null) Result.success(schools)
                    else Result.failure(Exception("Response body was null"))
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { SchoolEntity(it.id, it.name, it.schoolType ?: "unknown", it.province?.id ?: 0) }
                schoolDao.clearAll()
                schoolDao.insertAll(entities)
            }
        )
    }

    override suspend fun getSchoolById(id: Int): School? {
        return withContext(Dispatchers.IO) {
            schoolDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ GENDERS ============
    override fun getGenders(): Flow<List<Gender>> {
        return genderDao.getAll().map { entities ->
            entities.map { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshGenders(): Result<Unit> {
        return performRefresh(
            key = "genders",
            fetchFromApi = {
                val response = referenceApiService.getGenders()
                if (response.isSuccessful) {
                    val genders = response.body()
                    if (genders != null) Result.success(genders)
                    else Result.failure(Exception("Response body was null"))
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { GenderEntity(it.id, it.name) }
                genderDao.clearAll()
                genderDao.insertAll(entities)
            }
        )
    }

    override suspend fun getGenderById(id: Int): Gender? {
        return withContext(Dispatchers.IO) {
            genderDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ TAGS ============
    override fun getTags(): Flow<List<Tag>> {
        return tagDao.getAll().map { entities ->
            entities.map { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshTags(): Result<Unit> {
        return performRefresh(
            key = "tags",
            fetchFromApi = {
                val response = referenceApiService.getTags()
                if (response.isSuccessful) {
                    val tags = response.body()
                    if (tags != null) Result.success(tags)
                    else Result.failure(Exception("Response body was null"))
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { TagEntity(it.id, it.name, it.tagType ?: "unknown") }
                tagDao.clearAll()
                tagDao.insertAll(entities)
            }
        )
    }

    override suspend fun getTagById(id: Int): Tag? {
        return withContext(Dispatchers.IO) {
            tagDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    // ============ LOCATIONS ============
    override fun getLocations(): Flow<List<Location>> {
        return locationDao.getAll().map { entities ->
            entities.map { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshLocations(): Result<Unit> {
        return performRefresh(
            key = "locations",
            fetchFromApi = {
                val response = referenceApiService.getLocations()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) Result.success(body.locations ?: emptyList())
                    else Result.failure(Exception("API returned success=false"))
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                val entities = dtos.map { ReferenceDataMapper.toEntity(it) }
                locationDao.clearAll()
                locationDao.insertAll(entities)
            }
        )
    }

    override suspend fun getLocationById(id: Int): Location? {
        return withContext(Dispatchers.IO) {
            locationDao.getById(id)?.let { ReferenceDataMapper.toDomain(it) }
        }
    }

    override suspend fun refreshAllReferenceData(): Result<Unit> {
        return refreshAllReferenceDataBulk(forceRefresh = true)
    }


    override suspend fun clearAllCache() {
        withContext(Dispatchers.IO) {
            mainCategoryDao.clearAll()
            subCategoryDao.clearAll()
            colorDao.clearAll()
            sizeDao.clearAll()
            brandDao.clearAll()
            conditionDao.clearAll()
            provinceDao.clearAll()
            townDao.clearAll()
            schoolDao.clearAll()
            genderDao.clearAll()
            tagDao.clearAll()
            locationDao.clearAll()
            // Clear cache timestamps
            sharedPreferences.edit().clear().apply()
            Log.d("ReferenceDataRepo", "🗑️ All cache cleared")
        }
    }
}