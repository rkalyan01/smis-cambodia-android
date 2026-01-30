package com.innovative.smis.data.repository

import com.innovative.smis.data.api.BuildingSurveyApiService
import com.innovative.smis.data.model.response.*
import com.innovative.smis.util.common.Resource
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException
import android.util.Log

interface BuildingSurveyRepository {
    suspend fun getBuildingSurvey(bin: String): Flow<Resource<BuildingSurveyResponse>>
    suspend fun submitBuildingSurvey(bin: String, request: BuildingSurveyRequest): Flow<Resource<BuildingSurveyResponse>>
    suspend fun saveSurveyOffline(survey: com.innovative.smis.data.local.entity.BuildingSurveyEntity): Resource<Boolean>
    suspend fun getStructureTypes(): Flow<Resource<SurveyDropdownResponse>>
    suspend fun getFunctionalUses(): Flow<Resource<SurveyDropdownResponse>>
    suspend fun getBuildingUses(): Flow<Resource<SurveyDropdownResponse>>
    suspend fun getDefecationPlaces(): Flow<Resource<SurveyDropdownResponse>>
    suspend fun getToiletConnections(): Flow<Resource<SurveyDropdownResponse>>
    suspend fun getStorageTankTypes(): Flow<Resource<SurveyDropdownResponse>>
    suspend fun getStorageTankConnections(): Flow<Resource<SurveyDropdownResponse>>
    suspend fun getRoadCodes(): Flow<Resource<RoadCodeResponse>>
    suspend fun getSangkats(): Flow<Resource<SangkatResponse>>
    suspend fun getBuildingWms(bin: String? = null, sangkat: String? = null): Flow<Resource<WmsBuildingResponse>>
    suspend fun getRoadWms(): Flow<Resource<WmsRoadResponse>>
    suspend fun getSewerWms(): Flow<Resource<WmsSewerResponse>>
    suspend fun getSangkatWms(): Flow<Resource<WmsSangkatResponse>>
    suspend fun getWFSLayerBuildings(): Flow<Resource<com.innovative.smis.data.model.response.WfsBuildingLayerResponse>>
    suspend fun getWFSLayerBuildingSurveys(): Flow<Resource<com.innovative.smis.data.model.response.WfsBuildingSurveyLayerResponse>>
}

class BuildingSurveyRepositoryImpl(
    private val apiService: BuildingSurveyApiService,
    private val buildingSurveyDao: com.innovative.smis.data.local.dao.BuildingSurveyDao,
    private val moshi: Moshi
) : BuildingSurveyRepository {
    
    override suspend fun saveSurveyOffline(survey: com.innovative.smis.data.local.entity.BuildingSurveyEntity): Resource<Boolean> {
        return try {
            buildingSurveyDao.insertSurvey(survey)
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save survey locally")
        }
    }

    override suspend fun getBuildingSurvey(bin: String): Flow<Resource<BuildingSurveyResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getBuildingSurvey(bin)
            if (response.isSuccessful) {
                val surveyResponse = response.body()
                if (surveyResponse != null && surveyResponse.success) {
                    emit(Resource.Success(surveyResponse))
                } else {
                    emit(Resource.Error("Failed to get building survey"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun submitBuildingSurvey(bin: String, request: BuildingSurveyRequest): Flow<Resource<BuildingSurveyResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.updateBuildingSurvey(bin, request)
            if (response.isSuccessful) {
                val surveyResponse = response.body()
                if (surveyResponse != null && surveyResponse.success) {
                    emit(Resource.Success(surveyResponse))
                } else {
                    emit(Resource.Error(surveyResponse?.message ?: "Failed to submit survey"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getStructureTypes(): Flow<Resource<SurveyDropdownResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getStructureTypes()
            if (response.isSuccessful) {
                val dropdownResponse = response.body()
                if (dropdownResponse != null && dropdownResponse.success) {
                    emit(Resource.Success(dropdownResponse))
                } else {
                    emit(Resource.Error("Failed to get structure types"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getFunctionalUses(): Flow<Resource<SurveyDropdownResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getFunctionalUses()
            if (response.isSuccessful) {
                val dropdownResponse = response.body()
                if (dropdownResponse != null && dropdownResponse.success) {
                    emit(Resource.Success(dropdownResponse))
                } else {
                    emit(Resource.Error("Failed to get functional uses"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getBuildingUses(): Flow<Resource<SurveyDropdownResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getBuildingUses()
            if (response.isSuccessful) {
                val dropdownResponse = response.body()
                if (dropdownResponse != null && dropdownResponse.success) {
                    emit(Resource.Success(dropdownResponse))
                } else {
                    emit(Resource.Error("Failed to get building uses"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getDefecationPlaces(): Flow<Resource<SurveyDropdownResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getDefecationPlaces()
            if (response.isSuccessful) {
                val dropdownResponse = response.body()
                if (dropdownResponse != null && dropdownResponse.success) {
                    emit(Resource.Success(dropdownResponse))
                } else {
                    emit(Resource.Error("Failed to get defecation places"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getToiletConnections(): Flow<Resource<SurveyDropdownResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getToiletConnections()
            if (response.isSuccessful) {
                val dropdownResponse = response.body()
                if (dropdownResponse != null && dropdownResponse.success) {
                    emit(Resource.Success(dropdownResponse))
                } else {
                    emit(Resource.Error("Failed to get toilet connections"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getStorageTankTypes(): Flow<Resource<SurveyDropdownResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getStorageTankTypes()
            if (response.isSuccessful) {
                val dropdownResponse = response.body()
                if (dropdownResponse != null && dropdownResponse.success) {
                    emit(Resource.Success(dropdownResponse))
                } else {
                    emit(Resource.Error("Failed to get storage tank types"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getStorageTankConnections(): Flow<Resource<SurveyDropdownResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getStorageTankConnections()
            if (response.isSuccessful) {
                val dropdownResponse = response.body()
                if (dropdownResponse != null && dropdownResponse.success) {
                    emit(Resource.Success(dropdownResponse))
                } else {
                    emit(Resource.Error("Failed to get storage tank connections"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getRoadCodes(): Flow<Resource<RoadCodeResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getRoadCodes()
            if (response.isSuccessful) {
                val roadCodeResponse = response.body()
                if (roadCodeResponse != null && roadCodeResponse.success) {
                    emit(Resource.Success(roadCodeResponse))
                } else {
                    emit(Resource.Error("Failed to get road codes"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getSangkats(): Flow<Resource<SangkatResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getSangkats()
            if (response.isSuccessful) {
                val sangkatResponse = response.body()
                if (sangkatResponse != null && sangkatResponse.success) {
                    emit(Resource.Success(sangkatResponse))
                } else {
                    emit(Resource.Error("Failed to get sangkats"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getBuildingWms(bin: String?, sangkat: String?): Flow<Resource<com.innovative.smis.data.model.response.WmsBuildingResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getBuildingWms(bin, sangkat)
            if (response.isSuccessful) {
                val wmsResponse = response.body()
                if (wmsResponse != null && wmsResponse.success) {
                    emit(Resource.Success(wmsResponse))
                } else {
                    emit(Resource.Error("Failed to get building WMS data"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getWFSLayerBuildings(): Flow<Resource<com.innovative.smis.data.model.response.WfsBuildingLayerResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getWFSLayerBuildingsRaw()
            if (response.isSuccessful) {
                val body = response.body()?.string()
                if (body != null) {
                    Log.d("BuildingRepo", "WFS Body size: ${body.length}")
                    Log.d("BuildingRepo", "WFS Body start: ${body.take(1000)}")
                    val trimmed = body.trimStart()
                    val wfsResponse = try {
                            if (trimmed.startsWith("[")) {
                                Log.d("BuildingRepo", "Parsing as List<WfsBuildingFeature> (Array)")
                                // API returns a root-level array of features
                                val type = Types.newParameterizedType(List::class.java, WfsBuildingFeature::class.java)
                                val adapter = moshi.adapter<List<WfsBuildingFeature>>(type)
                                val features = adapter.fromJson(body) ?: emptyList()
                                Log.d("BuildingRepo", "Parsed ${features.size} features from Array")
                                WfsBuildingLayerResponse(type = "FeatureCollection", features = features)
                            } else {
                                Log.d("BuildingRepo", "Parsing as FeatureCollection (Object)")
                                // Standard GeoJSON FeatureCollection
                                val adapter = moshi.adapter(WfsBuildingLayerResponse::class.java)
                                val response = adapter.fromJson(body)
                                Log.d("BuildingRepo", "Parsed response: ${response != null}")
                                response ?: throw IOException("Empty WFS response")
                            }
                        } catch (e: Exception) {
                            Log.e("BuildingRepo", "Moshi Parsing FAILED: ${e.message}", e)
                            throw e // Re-throw to be caught by outer block
                        }
                    Log.d("BuildingRepo", "Parsing complete. Features: ${wfsResponse.features?.size}")
                    emit(Resource.Success(wfsResponse))
                } else {
                    Log.e("BuildingRepo", "WFS Body is null")
                    emit(Resource.Error("Failed to get WFS building layer"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getWFSLayerBuildingSurveys(): Flow<Resource<com.innovative.smis.data.model.response.WfsBuildingSurveyLayerResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getWFSLayerBuildingSurveys()
            if (response.isSuccessful) {
                val wfsResponse = response.body()
                if (wfsResponse != null) {
                    emit(Resource.Success(wfsResponse))
                } else {
                    emit(Resource.Error("Failed to get WFS building survey layer"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getRoadWms(): Flow<Resource<WmsRoadResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getRoadWms()
            if (response.isSuccessful) {
                val wmsResponse = response.body()
                if (wmsResponse != null && wmsResponse.success) {
                    emit(Resource.Success(wmsResponse))
                } else {
                    emit(Resource.Error("Failed to get road WMS data"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getSewerWms(): Flow<Resource<WmsSewerResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getSewerWms()
            if (response.isSuccessful) {
                val wmsResponse = response.body()
                if (wmsResponse != null && wmsResponse.success) {
                    emit(Resource.Success(wmsResponse))
                } else {
                    emit(Resource.Error("Failed to get sewer WMS data"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getSangkatWms(): Flow<Resource<WmsSangkatResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getSangkatWms()
            if (response.isSuccessful) {
                val wmsResponse = response.body()
                if (wmsResponse != null && wmsResponse.success) {
                    emit(Resource.Success(wmsResponse))
                } else {
                    emit(Resource.Error("Failed to get sangkat WMS data"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}