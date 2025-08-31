package com.innovative.smis.data.api

import com.innovative.smis.data.model.response.*
import com.innovative.smis.data.model.response.SimpleDropdownResponse
import com.innovative.smis.data.model.response.DesludgingVehicleListResponse
import com.innovative.smis.data.api.request.EmptyingServiceRequest
import retrofit2.Response
import retrofit2.http.*

interface LaravelApiService {

    // =====================================
    // EMPTYING SCHEDULING ENDPOINTS
    // =====================================

    /**
     * Get containment issues
     * GET /api/emptying-scheduling/show-issue-with-containment
     */
    @GET("emptying-scheduling/show-issue-with-containment")
    suspend fun showContainmentIssue(): Response<ContainmentIssuesResponse>

    /**
     * Get emptying reasons
     * GET /api/emptying-scheduling/show-emptying-reason
     */
    @GET("emptying-scheduling/show-emptying-reason")
    suspend fun showEmptyingReason(): Response<SimpleDropdownResponse>

    /**
     * Get sanitation customer details
     * GET /api/emptying-scheduling/sanitation-customer-details/{id}
     */
    @GET("emptying-scheduling/sanitation-customer-details/{id}")
    suspend fun getSanitationCustomerDetails(
        @Path("id") customerId: String
    ): Response<SanitationCustomerResponse>

    /**
     * Filter applications
     * GET /api/emptying-scheduling/filter
     */
    @GET("emptying-scheduling/filter")
    suspend fun filterApplications(
        @Query("application_status") status: String? = null,
        @Query("eto_id") etoId: String? = null
    ): Response<ApplicationListResponse>

    /**
     * Update emptying scheduling
     * PATCH /api/emptying-scheduling/{id}
     */
    @PATCH("emptying-scheduling/{id}")
    suspend fun updateEmptyingScheduling(
        @Path("id") applicationId: String,
        @Body formData: Map<String, Any>
    ): Response<ApplicationListResponse>

    // =====================================
    // EMPTYING SERVICE ENDPOINTS
    // =====================================

    /**
     * Create emptying service
     * POST /api/emptyings
     */
    @Multipart
    @PATCH("emptyings/{id}")
    suspend fun createEmptyingService(
        @Path("id") applicationId: Int,
        @Part("start_time") startTime: okhttp3.RequestBody,
        @Part("end_time") endTime: okhttp3.RequestBody,
        @Part("volume_of_sludge") volumeOfSludge: okhttp3.RequestBody,
        @Part("no_of_trips") noOfTrips: okhttp3.RequestBody,
        @Part("sludge_type_a") sludgeTypeA: okhttp3.RequestBody,
        @Part("sludge_type_b") sludgeTypeB: okhttp3.RequestBody,
        @Part("location_of_containment") locationOfContainment: okhttp3.RequestBody,
        @Part("presence_of_pumping_point") presenceOfPumpingPoint: okhttp3.RequestBody,
        @Part("other_additional_repairing") otherAdditionalRepairing: okhttp3.RequestBody,
        @Part("extra_payment") extraPayment: okhttp3.RequestBody,
        @Part("receipt_number") receiptNumber: okhttp3.RequestBody,
        @Part("comments") comments: okhttp3.RequestBody,
        @Part("eto_id") etoId: okhttp3.RequestBody,
        @Part("desludging_vehicle_id") desludgingVehicleId: okhttp3.RequestBody,
        @Part("longitude") longitude: okhttp3.RequestBody,
        @Part("latitude") latitude: okhttp3.RequestBody,
        @Part receiptImage: okhttp3.MultipartBody.Part?,
        @Part pictureOfEmptying: okhttp3.MultipartBody.Part?
    ): Response<EmptyingFormResponse>

    /**
     * Get emptying service readonly data
     * GET /api/emptyings/readonly-data/{application_id}
     */
    @GET("emptyings/readonly-data/{application_id}")
    suspend fun getEmptyingReadonlyData(
        @Path("application_id") applicationId: Int
    ): Response<EmptyingReadonlyDataResponse>

    /**
     * Get additional repairing options
     * GET /api/emptyings/additional-repairing
     */
    @GET("emptyings/additional-repairing")
    suspend fun getAdditionalRepairingOptions(): Response<SimpleDropdownResponse>

    /**
     * Update emptying service
     * PATCH /api/emptyings/{id}
     */
    @PATCH("emptyings/{id}")
    suspend fun updateEmptyingService(
        @Path("id") serviceId: String,
        @Body request: EmptyingServiceRequest
    ): Response<EmptyingFormResponse>

    /**
     * Get emptying service details
     * GET /api/emptyings/{id}
     */
    @GET("emptyings/{id}")
    suspend fun getEmptyingServiceDetails(
        @Path("id") serviceId: String
    ): Response<EmptyingFormResponse>

    /**
     * Get desludging vehicles by ETO ID
     * GET /api/desludging-vehicle/{eto_id}
     */
    @GET("desludging-vehicle/{eto_id}")
    suspend fun getDesludgingVehicles(
        @Path("eto_id") etoId: Int
    ): Response<DesludgingVehicleListResponse>

    // =====================================
    // SITE PREPARATION ENDPOINTS
    // =====================================

    /**
     * Filter site preparation applications
     * GET /api/site-preparation/filter
     */
    @GET("site-preparation/filter")
    suspend fun filterSitePreparation(
        @Query("site_visit_required") siteVisitRequired: String? = null,
        @Query("application_status") status: String? = null
    ): Response<ApplicationListResponse>

    /**
     * Create site preparation
     * POST /api/site-preparation
     */
    @POST("site-preparation")
    suspend fun createSitePreparation(
        @Body formData: Map<String, Any>
    ): Response<EmptyingFormResponse>

    // =====================================
    // SLUDGE COLLECTION ENDPOINTS
    // =====================================

    /**
     * Get readonly data for sludge collection
     * GET /api/sludge-collection/readonly-data
     */
    @GET("sludge-collection/readonly-data")
    suspend fun getSludgeReadonlyData(): Response<EmptyingReadonlyDataResponse>

    /**
     * Get ETO names
     * GET /api/sludge-collection/get-eto-names
     */
    @GET("sludge-collection/get-eto-names")
    suspend fun getEtoNames(): Response<EtoNamesResponse>

    /**
     * Get truck numbers
     * GET /api/sludge-collection/get-truck-numbers
     */
    @GET("sludge-collection/get-truck-numbers")
    suspend fun getTruckNumbers(): Response<TruckNumbersResponse>

    /**
     * Create sludge collection
     * POST /api/sludge-collection
     */
    @POST("sludge-collection")
    suspend fun createSludgeCollection(
        @Body formData: Map<String, Any>
    ): Response<EmptyingFormResponse>

    /**
     * Update sludge collection
     * PATCH /api/sludge-collection/{id}
     */
    @PATCH("sludge-collection/{id}")
    suspend fun updateSludgeCollection(
        @Path("id") collectionId: String,
        @Body formData: Map<String, Any>
    ): Response<EmptyingFormResponse>

    /**
     * Get sludge collection details
     * GET /api/sludge-collection/{id}
     */
    @GET("sludge-collection/{id}")
    suspend fun getSludgeCollectionDetails(
        @Path("id") collectionId: String
    ): Response<EmptyingFormResponse>
}