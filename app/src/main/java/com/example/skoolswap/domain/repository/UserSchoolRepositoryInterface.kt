package com.example.skoolswap.domain.repository

import com.example.skoolswap.domain.model.SchoolMapping
import com.example.skoolswap.utils.Result

interface UserSchoolRepositoryInterface {
    suspend fun assignSchool(schoolId: Int): com.example.skoolswap.utils.Result<Unit>
    suspend fun getCurrentSchoolMapping(): com.example.skoolswap.utils.Result<SchoolMapping?>
    suspend fun updateSchoolMapping(mappingId: String, schoolId: Int): Result<SchoolMapping>
    suspend fun removeSchoolMapping(mappingId: String): com.example.skoolswap.utils.Result<Boolean>
    suspend fun getNearbySchoolIds(schoolId: Int): List<Int>
}