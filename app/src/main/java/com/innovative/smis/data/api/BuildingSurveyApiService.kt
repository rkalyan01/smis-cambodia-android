package com.innovative.smis.data.api

import com.innovative.smis.data.model.response.*
import com.innovative.smis.data.model.response.WmsBuildingResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface BuildingSurveyApiService {
    
    /**
     * Get building survey details for a specific BIN
     * GET /api/get-bs-survey/{bin}
     */
    @GET("get-bs-survey/{bin}")
    suspend fun getBuildingSurvey(
        @Path("bin") bin: String
    ): Response<BuildingSurveyResponse>
    
    /**
     * Update/submit building survey
     * POST /api/update-survey/{bin}
     */
    @POST("update-survey/{bin}")
    suspend fun updateBuildingSurvey(
        @Path("bin") bin: String,
        @Body request: BuildingSurveyRequest
    ): Response<BuildingSurveyResponse>
    
    /**
     * Get structure types dropdown options
     * GET /api/structure-types
     */
    @GET("structure-types")
    suspend fun getStructureTypes(): Response<SurveyDropdownResponse>
    
    /**
     * Get functional uses dropdown options  
     * GET /api/functional-uses
     */
    @GET("functional-uses")
    suspend fun getFunctionalUses(): Response<SurveyDropdownResponse>
    
    /**
     * Get building uses dropdown options
     * GET /api/building-uses
     */
    @GET("building-uses")
    suspend fun getBuildingUses(): Response<SurveyDropdownResponse>
    
    /**
     * Get defecation places dropdown options
     * GET /api/defecation-places
     */
    @GET("defecation-places")
    suspend fun getDefecationPlaces(): Response<SurveyDropdownResponse>
    
    /**
     * Get toilet connections dropdown options
     * GET /api/toilet-connections
     */
    @GET("toilet-connections")
    suspend fun getToiletConnections(): Response<SurveyDropdownResponse>
    
    /**
     * Get storage tank types dropdown options
     * GET /api/storage-tank-types
     */
    @GET("storage-tank-types")
    suspend fun getStorageTankTypes(): Response<SurveyDropdownResponse>
    
    /**
     * Get storage tank connections dropdown options
     * GET /api/storage-tank-connections
     */
    @GET("storage-tank-connections") 
    suspend fun getStorageTankConnections(): Response<SurveyDropdownResponse>
    
    /**
     * Get road codes for dropdown
     * GET /api/getRoadCode
     */
    @GET("getRoadCode")
    suspend fun getRoadCodes(): Response<RoadCodeResponse>
    
    /**
     * Get sangkat options for dropdown
     * GET /api/getSankhat
     */
    @GET("getSankhat")
    suspend fun getSangkats(): Response<SangkatResponse>
    
    /**
     * Get WMS building data
     * GET /api/wms/buildings
     */
    @GET("wms/buildings")
    suspend fun getBuildingWms(
        @Query("bin") bin: String? = null,
        @Query("sangkat") sangkat: String? = null
    ): Response<WmsBuildingResponse>
    
    /**
     * Get WMS sangkat data
     * GET /api/wms/sangkats
     */
    @GET("wms/sangkats")
    suspend fun getSangkatWms(): Response<WmsSangkatResponse>
    
    /**
     * Get WMS road data
     * GET /api/wms/roads
     */
    @GET("wms/roads")
    suspend fun getRoadWms(): Response<WmsRoadResponse>
    
    /**
     * Get WMS sewer data
     * GET /api/wms/sewers
     */
    @GET("wms/sewers")
    suspend fun getSewerWms(): Response<WmsSewerResponse>
    
    /**
     * Get WFS building layer data (raw body for flexible parsing: API may return either a GeoJSON FeatureCollection or a raw array of features).
     * GET /api/wfs-building-layer
     */
    @GET("wfs-building-layer")
    suspend fun getWFSLayerBuildingsRaw(): Response<ResponseBody>
    
    /**
     * Get WFS building survey layer data
     * GET /api/wfs-buildingsurvey-layer
     */
    @GET("wfs-buildingsurvey-layer")
    suspend fun getWFSLayerBuildingSurveys(): Response<com.innovative.smis.data.model.response.WfsBuildingSurveyLayerResponse>
    
    /**
     * Get buildings with shared sewer
     * GET /api/buildingwithsharedSewer
     */
    @GET("buildingwithsharedSewer")
    suspend fun getBuildingsWithSharedSewer(): Response<SharedSewerBuildingResponse>
}

// WMS Response classes are imported from com.innovative.smis.data.model.response.*