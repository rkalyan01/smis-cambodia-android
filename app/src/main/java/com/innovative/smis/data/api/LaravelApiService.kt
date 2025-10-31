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
     * Get emptying service readonly data
     * GET /api/emptyings/readonly-data/{application_id}
     */
    @GET("emptyings/readonly-data/{application_id}")
    suspend fun getEmptyingReadonlyData(
        @Path("application_id") applicationId: Int
    ): Response<EmptyingReadonlyDataResponse>

    /**
     * Get additional repairing options
     * GET /api/site-preparation/show-additional-repairing
     */
    @GET("site-preparation/show-additional-repairing")
    suspend fun getAdditionalRepairingOptions(): Response<SimpleDropdownResponse>

    /**
     * Create emptying service (use this instead of update for initial submission)
     * POST /api/emptyings/create/{application_id}
     */
    @POST("emptyings/create/{application_id}")
    suspend fun createEmptyingService(
        @Path("application_id") applicationId: Int,
        @Body request: EmptyingServiceRequest
    ): Response<EmptyingFormResponse>

    /**
     * Update payment details for emptying service
     * POST /api/emptyings/{emptying_id} with X-HTTP-Method-Override: PUT
     * Using POST with method override header for Laravel multipart compatibility
     */
    @Multipart
    @POST("emptyings/{emptying_id}")
    suspend fun updateEmptyingPaymentDetails(
        @Path("emptying_id") emptyingId: Int,
        @Header("X-HTTP-Method-Override") method: String,
        @Part("extra_payment") extraPayment: okhttp3.RequestBody?,
        @Part("receipt_number") receiptNumber: okhttp3.RequestBody?,
        @Part("comments") comments: okhttp3.RequestBody?,
        @Part receiptImage: okhttp3.MultipartBody.Part?
    ): Response<EmptyingFormResponse>

    /**
     * Update/Create emptying service (DEPRECATED - use createEmptyingService)
     * PATCH /api/emptyings/{id}
     */
    @PATCH("emptyings/{id}")
    suspend fun updateEmptyingService(
        @Path("id") applicationId: String,
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

    // =====================================
    // ADDITIONAL REPAIRING ENDPOINTS
    // =====================================

    /**
     * Get trip filter applications
     * GET /api/emptyings/trip-filter
     */
    @GET("emptyings/trip-filter")
    suspend fun getTripFilterApplications(
        @Query("application_status") applicationStatus: String,
        @Query("eto_id") etoId: String,
        @Query("additional_trip_required") additionalTripRequired: String
    ): Response<TripFilterResponse>

    /**
     * Get regular payment amount
     * GET /api/emptyings-trip/get-amount-regular-payment
     */
    @GET("emptyings-trip/get-amount-regular-payment")
    suspend fun getRegularPaymentAmount(): Response<RegularPaymentAmountResponse>

    /**
     * Create trip entry
     * POST /api/emptyings-trip/create/{emptying_id}
     */
    @POST("emptyings-trip/create/{emptying_id}")
    suspend fun createTripEntry(
        @Path("emptying_id") emptyingId: Int,
        @Body request: com.innovative.smis.data.model.request.TripCreateRequest
    ): Response<TripCreateResponse>

    /**
     * Update payment details for additional repairing
     * PATCH /api/emptyings/{emptying_id}
     */
    @Multipart
    @PATCH("emptyings/{emptying_id}")
    suspend fun updatePaymentDetails(
        @Path("emptying_id") emptyingId: Int,
        @Part("amount_of_extra_payment") amountOfExtraPayment: okhttp3.RequestBody?,
        @Part("receipt_number") receiptNumber: okhttp3.RequestBody?,
        @Part("comments") comments: okhttp3.RequestBody?,
        @Part receiptImage: okhttp3.MultipartBody.Part?
    ): Response<PaymentUpdateResponse>

    /**
     * Get emptying details for payment update
     * GET /api/emptyings/{emptying_id}
     */
    @GET("emptyings/{emptying_id}")
    suspend fun getEmptyingDetails(
        @Path("emptying_id") emptyingId: Int
    ): Response<EmptyingDetailsResponse>

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