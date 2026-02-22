package com.example.skoolswap.domain.repository

import com.example.skoolswap.domain.model.reference.*
import kotlinx.coroutines.flow.Flow

interface ReferenceDataRepositoryInterface {

    // ============ MAIN CATEGORIES ============
    fun getMainCategories(): Flow<List<MainCategory>>
    suspend fun refreshMainCategories(): Result<Unit>
    suspend fun getMainCategoryById(id: Int): MainCategory?

    // ============ SUB CATEGORIES ============
    fun getSubCategories(mainCategoryId: Int? = null): Flow<List<SubCategory>>
    suspend fun refreshSubCategories(mainCategoryId: Int? = null): Result<Unit>
    suspend fun getSubCategoryById(id: Int): SubCategory?

    // ============ COLORS ============
    fun getColors(): Flow<List<Color>>
    suspend fun refreshColors(): Result<Unit>
    suspend fun getColorById(id: Int): Color?

    // ============ SIZES ============
    fun getSizes(): Flow<List<Size>>
    suspend fun refreshSizes(): Result<Unit>
    suspend fun getSizeById(id: Int): Size?

    // ============ BRANDS ============
    fun getBrands(): Flow<List<Brand>>
    suspend fun refreshBrands(): Result<Unit>
    suspend fun getBrandById(id: Int): Brand?

    // ============ CONDITIONS ============
    fun getConditions(): Flow<List<Condition>>
    suspend fun refreshConditions(): Result<Unit>
    suspend fun getConditionById(id: Int): Condition?

    // ============ PROVINCES ============
    fun getProvinces(): Flow<List<Province>>
    suspend fun refreshProvinces(): Result<Unit>
    suspend fun getProvinceById(id: Int): Province?

    // ============ TOWNS ============
    fun getTowns(provinceId: Int? = null): Flow<List<Town>>
    suspend fun refreshTowns(provinceId: Int? = null): Result<Unit>
    suspend fun getTownById(id: Int): Town?

    // ============ SCHOOLS ============
    fun getSchools(): Flow<List<School>>
    suspend fun refreshSchools(): Result<Unit>
    suspend fun getSchoolById(id: Int): School?

    // ============ GENDERS ============
    fun getGenders(): Flow<List<Gender>>
    suspend fun refreshGenders(): Result<Unit>
    suspend fun getGenderById(id: Int): Gender?

    // ============ TAGS ============
    fun getTags(): Flow<List<Tag>>
    suspend fun refreshTags(): Result<Unit>
    suspend fun getTagById(id: Int): Tag?

    // ============ LOCATIONS ============
    fun getLocations(): Flow<List<Location>>
    suspend fun refreshLocations(): Result<Unit>
    suspend fun getLocationById(id: Int): Location?

    // ============ BULK OPERATIONS ============
    suspend fun refreshAllReferenceData(): Result<Unit>

    // 👇 ADD THIS NEW METHOD
    suspend fun refreshAllReferenceDataBulk(): Result<Unit>

    suspend fun clearAllCache()
}