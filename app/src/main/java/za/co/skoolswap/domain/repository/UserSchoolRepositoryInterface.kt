package za.co.skoolswap.domain.repository

import za.co.skoolswap.domain.model.SchoolMapping
import za.co.skoolswap.utils.Result

interface UserSchoolRepositoryInterface {
    suspend fun assignSchool(schoolId: Int): za.co.skoolswap.utils.Result<Unit>
    suspend fun getCurrentSchoolMapping(): za.co.skoolswap.utils.Result<SchoolMapping?>
    suspend fun updateSchoolMapping(mappingId: String, schoolId: Int): Result<SchoolMapping>
    suspend fun removeSchoolMapping(mappingId: String): za.co.skoolswap.utils.Result<Boolean>
    suspend fun getNearbySchoolIds(schoolId: Int): List<Int>
}