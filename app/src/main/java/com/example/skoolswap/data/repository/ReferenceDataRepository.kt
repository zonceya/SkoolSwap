package com.example.skoolswap.data.repository


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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map


@Singleton
class ReferenceDataRepository @Inject constructor(
    private val referenceApiService: ReferenceDataApiService,
    private val authRepository: AuthRepository,
    // You'll need to create these DAOs
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
    private val locationDao: LocationDao
) : ReferenceDataRepositoryInterface {

    private val refreshMutex = Mutex()
    private val refreshFlags = mutableMapOf<String, Boolean>()

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
                    return@withContext Result.failure(
                        Exception("User not authenticated")
                    )
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
                        Log.e("ReferenceRepo", "❌ API returned success=false: ${body?.error}")
                        Result.failure(Exception(body?.error ?: "Unknown error"))
                    }
                } else {
                    Log.e("ReferenceRepo", "❌ API call failed: ${response.code()}")
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} subcategories to database")

                val entities = dtos.map { dto ->
                    // Pass the mainCategoryId from the context
                    ReferenceDataMapper.toEntity(dto, mainCategoryId)
                }

                // Clear old data for this main category
                if (mainCategoryId != null) {
                    subCategoryDao.deleteByMainCategoryId(mainCategoryId)
                    Log.d("ReferenceRepo", "🗑️ Deleted old subcategories for mainCategoryId: $mainCategoryId")
                }

                // Insert new data
                subCategoryDao.insertAll(entities)
                Log.d("ReferenceRepo", "✅ Inserted ${entities.size} subcategories")
            }
        )
    }
    override suspend fun getSubCategoryById(id: Int): SubCategory? {
        return withContext(Dispatchers.IO) {
            val entity: SubCategoryEntity? = subCategoryDao.getById(id)
            entity?.let {
                ReferenceDataMapper.toDomain(it)
            }
        }
    }
    override suspend fun refreshAllReferenceData(): Result<Unit> {
        return try {
            refreshMainCategories()
            refreshSubCategories()
            refreshColors()
            refreshSizes()
            refreshBrands()
            refreshConditions()
            refreshProvinces()
            refreshSchools()
            refreshGenders()
            refreshTags()
            refreshLocations()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun refreshAllReferenceDataBulk(): Result<Unit> {
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
                            val data = body.data

                            // Save all data to database
                            saveAllReferenceData(data)

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
            TagEntity(it.id, it.name, it.tagType)
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
                val response = referenceApiService.getItemColors()  // Now returns List<ItemColorDto>

                if (response.isSuccessful) {
                    val colors = response.body()
                    if (colors != null) {
                        Log.d("ReferenceRepo", "✅ Successfully fetched ${colors.size} colors")
                        Result.success(colors)
                    } else {
                        Result.failure(Exception("Response body was null"))
                    }
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} colors to database")
                val entities = dtos.map { dto ->
                    ColorEntity(
                        id = dto.id,
                        name = dto.name
                    )
                }
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
                val response = referenceApiService.getItemSizes()  // Now returns List<ItemSizeDto>

                if (response.isSuccessful) {
                    val sizes = response.body()
                    if (sizes != null) {
                        Log.d("ReferenceRepo", "✅ Successfully fetched ${sizes.size} sizes")
                        Result.success(sizes)
                    } else {
                        Result.failure(Exception("Response body was null"))
                    }
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} sizes to database")
                val entities = dtos.map { dto ->
                    SizeEntity(
                        id = dto.id,
                        name = dto.name
                    )
                }
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
                val response = referenceApiService.getBrands()  // Now returns List<BrandDto>

                if (response.isSuccessful) {
                    val brands = response.body()  // This is now directly the list
                    if (brands != null) {
                        Log.d("ReferenceRepo", "✅ Successfully fetched ${brands.size} brands")
                        Result.success(brands)
                    } else {
                        Log.e("ReferenceRepo", "❌ Response body was null")
                        Result.failure(Exception("Response body was null"))
                    }
                } else {
                    Log.e("ReferenceRepo", "❌ API call failed: ${response.code()}")
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} brands to database")
                val entities = dtos.map { dto ->
                    BrandEntity(
                        id = dto.id,
                        name = dto.name
                    )
                }
                brandDao.clearAll()
                brandDao.insertAll(entities)
                Log.d("ReferenceRepo", "✅ Saved ${entities.size} brands")
            }        )
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
                val response = referenceApiService.getItemConditions()  // Now returns List<ItemConditionDto>

                if (response.isSuccessful) {
                    val conditions = response.body()
                    if (conditions != null) {
                        Log.d("ReferenceRepo", "✅ Successfully fetched ${conditions.size} conditions")
                        Result.success(conditions)
                    } else {
                        Result.failure(Exception("Response body was null"))
                    }
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} conditions to database")
                val entities = dtos.map { dto ->
                    ConditionEntity(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description
                    )
                }
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
                    if (provinces != null) {
                        Log.d("ReferenceRepo", "✅ Successfully fetched ${provinces.size} provinces")
                        Result.success(provinces)
                    } else {
                        Result.failure(Exception("Response body was null"))
                    }
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} provinces to database")
                val entities = dtos.map { dto ->
                    ProvinceEntity(
                        id = dto.id,
                        name = dto.name
                    )
                }
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
                if (provinceId == null) {
                    return@performRefresh Result.failure(Exception("provinceId is required"))
                }
                val response = referenceApiService.getTowns(provinceId)
                if (response.isSuccessful) {
                    val towns = response.body()
                    if (towns != null) {
                        Log.d("ReferenceRepo", "✅ Successfully fetched ${towns.size} towns for province $provinceId")
                        Result.success(towns)
                    } else {
                        Result.failure(Exception("Response body was null"))
                    }
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} towns to database")
                val entities = dtos.map { dto ->
                    TownEntity(
                        id = dto.id,
                        name = dto.name,
                        provinceId = dto.provinceId
                    )
                }
                if (provinceId != null) {
                    townDao.deleteByProvinceId(provinceId)
                }
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
                    if (schools != null) {
                        Log.d("ReferenceRepo", "✅ Successfully fetched ${schools.size} schools")
                        Result.success(schools)
                    } else {
                        Result.failure(Exception("Response body was null"))
                    }
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} schools to database")

                val entities = dtos.map { dto ->
                    SchoolEntity(
                        id = dto.id,
                        name = dto.name,
                        schoolType = dto.schoolType ?: "unknown",
                        provinceId = dto.province?.id  // Just use the provinceId
                    )
                }
                schoolDao.clearAll()
                schoolDao.insertAll(entities)

                // Remove the provinceEntities part if you don't need it
                // The provinces should already be loaded from refreshProvinces()
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
                    if (genders != null) {
                        Log.d("ReferenceRepo", "✅ Successfully fetched ${genders.size} genders")
                        Result.success(genders)
                    } else {
                        Result.failure(Exception("Response body was null"))
                    }
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} genders to database")
                val entities = dtos.map { dto ->
                    GenderEntity(
                        id = dto.id,
                        name = dto.name
                        // Note: display_name can be stored if you add it to your entity
                    )
                }
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
                    if (tags != null) {
                        Log.d("ReferenceRepo", "✅ Successfully fetched ${tags.size} tags")
                        Result.success(tags)
                    } else {
                        Result.failure(Exception("Response body was null"))
                    }
                } else {
                    Result.failure(Exception("API call failed: ${response.code()}"))
                }
            },
            saveToDb = { dtos ->
                Log.d("ReferenceRepo", "💾 Saving ${dtos.size} tags to database")
                val entities = dtos.map { dto ->
                    TagEntity(
                        id = dto.id,
                        name = dto.name,
                        tagType = dto.tagType
                    )
                }
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
                    if (body?.success == true) {
                        Result.success(body.locations ?: emptyList())
                    } else {
                        Result.failure(Exception("API returned success=false"))
                    }
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

    // ============ BULK OPERATIONS ============
/*    override suspend fun refreshAllReferenceData(): Result<Unit> {
        return try {
            refreshMainCategories()
            refreshSubCategories()
            refreshColors()
            refreshSizes()
            refreshBrands()
            refreshConditions()
            refreshProvinces()
            refreshSchools()
            refreshGenders()
            refreshTags()
            refreshLocations()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/

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
        }
    }
}