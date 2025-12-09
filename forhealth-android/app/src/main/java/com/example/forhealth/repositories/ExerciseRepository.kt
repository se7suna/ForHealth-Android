package com.example.forhealth.repositories

import com.example.forhealth.network.ApiResult
import com.example.forhealth.network.RetrofitClient
import com.example.forhealth.network.dto.sports.*
import com.example.forhealth.network.safeApiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * 运动数据仓库
 * 负责运动记录的增删改查（只返回DTO，不进行转换）
 */
class ExerciseRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * 获取用户可用的运动类型列表
     */
    suspend fun getAvailableSportsTypes(): ApiResult<List<SearchSportsResponse>> {
        return safeApiCall {
            apiService.getAvailableSportsTypes()
        }
    }
    
    /**
     * 创建自定义运动类型
     */
    suspend fun createSport(
        sportName: String,
        describe: String? = null,
        mets: Double? = null,
        imageFile: File? = null
    ): ApiResult<SimpleSportsResponse> {
        return safeApiCall {
            val imagePart: MultipartBody.Part? = if (imageFile != null && imageFile.exists()) {
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("imageFile", imageFile.name, requestFile)
            } else {
                null
            }
            
            apiService.createSport(
                sportName = sportName,
                describe = describe,
                mets = mets,
                imageFile = imagePart
            )
        }
    }
    
    /**
     * 更新自定义运动类型
     */
    suspend fun updateSport(
        oldSportName: String,
        newSportName: String? = null,
        describe: String? = null,
        mets: Double? = null,
        imageFile: File? = null
    ): ApiResult<SimpleSportsResponse> {
        return safeApiCall {
            val imagePart: MultipartBody.Part? = if (imageFile != null && imageFile.exists()) {
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("imageFile", imageFile.name, requestFile)
            } else {
                null
            }
            
            apiService.updateSport(
                oldSportName = oldSportName,
                newSportName = newSportName,
                describe = describe,
                mets = mets,
                imageFile = imagePart
            )
        }
    }
    
    /**
     * 删除自定义运动类型
     */
    suspend fun deleteSport(sportName: String): ApiResult<SimpleSportsResponse> {
        return safeApiCall {
            apiService.deleteSport(sportName)
        }
    }
    
    /**
     * 记录运动及消耗卡路里
     */
    suspend fun logSports(request: LogSportsRequest): ApiResult<SimpleSportsResponse> {
        return safeApiCall {
            apiService.logSports(request)
        }
    }
    
    /**
     * 更新运动记录
     */
    suspend fun updateSportRecord(request: UpdateSportsRecordRequest): ApiResult<SimpleSportsResponse> {
        return safeApiCall {
            apiService.updateSportRecord(request)
        }
    }
    
    /**
     * 删除运动记录
     */
    suspend fun deleteSportRecord(recordId: String): ApiResult<SimpleSportsResponse> {
        return safeApiCall {
            apiService.deleteSportRecord(recordId)
        }
    }
    
    /**
     * 查询运动历史
     */
    suspend fun searchSportsRecords(request: SearchSportRecordsRequest): ApiResult<List<SearchSportRecordsResponse>> {
        return safeApiCall {
            apiService.searchSportsRecords(request)
        }
    }
    
    /**
     * 获取用户全部运动记录
     */
    suspend fun getAllSportsRecords(): ApiResult<List<SearchSportRecordsResponse>> {
        return safeApiCall {
            apiService.getAllSportsRecords()
        }
    }
    
    /**
     * 获取今日运动记录
     */
    suspend fun getTodaySportsRecords(): ApiResult<List<SearchSportRecordsResponse>> {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(java.util.Date())
        return searchSportsRecords(
            SearchSportRecordsRequest(
                start_date = today,
                end_date = today
            )
        )
    }
}

