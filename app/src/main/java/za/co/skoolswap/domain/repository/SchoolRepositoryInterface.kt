package za.co.skoolswap.domain.repository

import za.co.skoolswap.domain.model.Province
import za.co.skoolswap.domain.model.School
import za.co.skoolswap.utils.Result

interface SchoolRepositoryInterface {

    // Get all provinces (with caching)
    suspend fun getProvinces(): Result<List<Province>>

    // Search schools by province and query string
    suspend fun searchSchools(provinceId: Int, query: String): Result<List<School>>


    suspend fun searchSchoolsByTown(
        provinceId: Int,
        townName: String,
        schoolQuery: String? = null
    ): Result<List<School>>

    // Get school by ID
    suspend fun getSchoolById(id: Int): School?

    // Background refresh
    fun refreshProvincesInBackground()
}