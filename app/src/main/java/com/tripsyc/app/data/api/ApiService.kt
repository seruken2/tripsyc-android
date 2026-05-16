package com.tripsyc.app.data.api

import com.tripsyc.app.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── Auth ─────────────────────────────────────────────────────────────────

    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body body: Map<String, String>): OTPResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body body: Map<String, String>): VerifyCodeResponse

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<User?>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    // ─── Trips ────────────────────────────────────────────────────────────────

    @GET("api/trips")
    suspend fun getTrips(): List<Trip>

    @GET("api/trips/{id}")
    suspend fun getTrip(@Path("id") id: String): Trip

    @POST("api/trips")
    suspend fun createTrip(@Body body: Map<String, String?>): Trip

    @PATCH("api/trips/{id}/manage")
    suspend fun updateTrip(
        @Path("id") id: String,
        @Body body: Map<String, Any?>
    ): Trip

    @POST("api/trips/{id}/invite")
    suspend fun inviteMember(
        @Path("id") tripId: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @GET("api/past-co-travelers")
    suspend fun getPastCoTravelers(
        @Query("excludeTripId") excludeTripId: String? = null
    ): PastCoTravelersResponse

    @GET("api/destinations/{id}/environment")
    suspend fun getDestinationEnvironment(
        @Path("id") destinationId: String
    ): DestinationEnvironment

    // ─── Smart Itinerary (AI) ─────────────────────────────────────────────────

    @GET("api/ai/itinerary-draft")
    suspend fun getSmartItineraryDrafts(
        @Query("tripId") tripId: String
    ): AIItineraryDraftsResponse

    @POST("api/ai/itinerary-draft")
    suspend fun generateSmartItinerary(
        @Body body: Map<String, String?>
    ): AIItineraryGenerateResponse

    @POST("api/ai/itinerary-draft/vote")
    suspend fun voteOnSmartItem(
        @Body body: Map<String, String>
    ): Response<Unit>

    @POST("api/ai/itinerary-draft/item/{id}/accept")
    suspend fun acceptSmartItem(
        @Path("id") itemId: String
    ): Response<Map<String, Any?>>

    @POST("api/ai/itinerary-draft/accept-majority")
    suspend fun acceptMajoritySmartItems(
        @Body body: Map<String, String>
    ): AcceptMajorityResponse

    @POST("api/ai/trip-summary")
    suspend fun generateTripSummary(
        @Body body: Map<String, String>
    ): TripSummaryResponse

    @POST("api/trips/{id}/clone")
    suspend fun cloneTrip(
        @Path("id") sourceTripId: String,
        @Body body: Map<String, Any?>
    ): CloneTripResponse

    @POST("api/ai/packing-list")
    suspend fun generatePackingSuggestions(
        @Body body: Map<String, String>
    ): PackingListSuggestionsResponse

    @GET("api/exchange-rates")
    suspend fun getExchangeRates(
        @Query("from") from: String,
        @Query("to") to: String
    ): ExchangeRatesResponse

    // ─── Members ──────────────────────────────────────────────────────────────

    @GET("api/trip-members/{tripId}")
    suspend fun getMembers(@Path("tripId") tripId: String): List<TripMember>

    @POST("api/trip-members")
    suspend fun joinTrip(@Body body: Map<String, Any?>): TripMember

    @PATCH("api/trip-members/update")
    suspend fun updateMember(@Body body: Map<String, Any?>): TripMember

    @POST("api/trip-members/leave")
    suspend fun leaveTrip(@Body body: Map<String, String>): Response<Unit>

    @DELETE("api/trip-members/{tripId}")
    suspend fun removeMember(
        @Path("tripId") tripId: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    // ─── Availability ─────────────────────────────────────────────────────────

    @GET("api/availability/{tripId}")
    suspend fun getAvailability(@Path("tripId") tripId: String): AvailabilityResponse

    @POST("api/availability")
    suspend fun setAvailability(@Body body: Map<String, Any>): List<Availability>

    // ─── Global Availability ──────────────────────────────────────────────────

    @GET("api/global-availability")
    suspend fun getGlobalAvailability(): GlobalAvailabilityListResponse

    @POST("api/global-availability")
    suspend fun setGlobalAvailability(@Body body: Map<String, Any>): Response<Unit>

    @HTTP(method = "DELETE", path = "api/global-availability", hasBody = true)
    suspend fun deleteGlobalAvailability(@Body body: Map<String, Any>): Response<Unit>

    // ─── Destinations ─────────────────────────────────────────────────────────

    @GET("api/destinations")
    suspend fun getDestinations(@Query("tripId") tripId: String): DestinationsAPIResponse

    @POST("api/destinations")
    suspend fun addDestination(@Body body: Map<String, Any?>): Destination

    @DELETE("api/destinations/{id}")
    suspend fun deleteDestination(@Path("id") id: String): Response<Unit>

    @POST("api/destinations/shortlist")
    suspend fun updateShortlist(@Body body: Map<String, Any>): ShortlistResponse

    @POST("api/destinations/shortlist-phase")
    suspend fun advanceToShortlist(@Body body: Map<String, String>): Response<Unit>

    @POST("api/votes")
    suspend fun castVote(@Body body: Map<String, Any?>): Vote

    @POST("api/comments")
    suspend fun addComment(@Body body: Map<String, Any?>): Comment

    @HTTP(method = "DELETE", path = "api/comments", hasBody = true)
    suspend fun deleteComment(@Body body: Map<String, String>): Response<Unit>

    @POST("api/dealbreakers")
    suspend fun toggleDealbreaker(@Body body: Map<String, String>): DealbreakerToggleResponse

    @GET("api/dealbreakers")
    suspend fun getDealbreakers(@Query("tripId") tripId: String): DealbreakerListResponse

    // ─── Budget ───────────────────────────────────────────────────────────────

    @GET("api/budget/{tripId}")
    suspend fun getBudget(@Path("tripId") tripId: String): BudgetAPIResponse

    @POST("api/budget/update")
    suspend fun updateBudget(@Body body: Map<String, Any>): BudgetUpdateResponse

    // ─── Expenses ─────────────────────────────────────────────────────────────

    @GET("api/expenses")
    suspend fun getExpenses(@Query("tripId") tripId: String): ExpensesResponse

    @POST("api/expenses")
    suspend fun addExpense(@Body body: Map<String, Any?>): Expense

    @PATCH("api/expenses")
    suspend fun settleSplit(@Body body: Map<String, Any>): ExpenseSplit

    @HTTP(method = "DELETE", path = "api/expenses", hasBody = true)
    suspend fun deleteExpense(@Body body: Map<String, String>): Response<Unit>

    @POST("api/expenses/settle")
    suspend fun settleAll(@Body body: Map<String, String>): SettleAllResponse

    // ─── Chat ─────────────────────────────────────────────────────────────────

    @GET("api/chat")
    suspend fun getMessages(
        @Query("tripId") tripId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): MessagesResponse

    @POST("api/chat")
    suspend fun sendMessage(@Body body: Map<String, Any?>): ChatMessageWithUser

    @PATCH("api/chat")
    suspend fun patchMessage(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @HTTP(method = "DELETE", path = "api/chat", hasBody = true)
    suspend fun deleteMessage(@Body body: Map<String, String>): Response<Unit>

    @POST("api/chat/reactions")
    suspend fun addReaction(@Body body: Map<String, String>): Response<Unit>

    @HTTP(method = "DELETE", path = "api/chat/reactions", hasBody = true)
    suspend fun removeReaction(@Body body: Map<String, String>): Response<Unit>

    @GET("api/chat/read-receipts")
    suspend fun getReadReceipts(@Query("tripId") tripId: String): ReadReceiptsResponse

    @POST("api/chat/read")
    suspend fun markRead(@Body body: Map<String, String>): Response<Unit>

    @POST("api/chat/typing")
    suspend fun sendTyping(@Body body: Map<String, String>): Response<Unit>

    @GET("api/chat/typing")
    suspend fun getTyping(@Query("tripId") tripId: String): Response<Map<String, Any>>

    // ─── Notes ────────────────────────────────────────────────────────────────

    @GET("api/notes")
    suspend fun getNotes(@Query("tripId") tripId: String): NotesResponse

    @POST("api/notes")
    suspend fun createNote(@Body body: Map<String, Any?>): Note

    @PATCH("api/notes")
    suspend fun updateNote(@Body body: Map<String, Any?>): Note

    @HTTP(method = "DELETE", path = "api/notes", hasBody = true)
    suspend fun deleteNote(@Body body: Map<String, String>): Response<Unit>

    // ─── Packing ──────────────────────────────────────────────────────────────

    @GET("api/packing")
    suspend fun getPackingItems(@Query("tripId") tripId: String): PackingResponse

    @POST("api/packing")
    suspend fun addPackingItem(@Body body: Map<String, String>): PackingItem

    @PATCH("api/packing")
    suspend fun togglePacked(@Body body: Map<String, Any>): Response<Unit>

    @HTTP(method = "DELETE", path = "api/packing", hasBody = true)
    suspend fun deletePackingItem(@Body body: Map<String, String>): Response<Unit>

    // ─── Responsibilities ─────────────────────────────────────────────────────

    @GET("api/responsibilities")
    suspend fun getResponsibilities(@Query("tripId") tripId: String): ResponsibilitiesResponse

    @POST("api/responsibilities")
    suspend fun addResponsibility(@Body body: Map<String, Any?>): Responsibility

    @PATCH("api/responsibilities")
    suspend fun updateResponsibility(@Body body: Map<String, Any>): Response<Unit>

    @HTTP(method = "DELETE", path = "api/responsibilities", hasBody = true)
    suspend fun deleteResponsibility(@Body body: Map<String, String>): Response<Unit>

    // ─── Itinerary ────────────────────────────────────────────────────────────

    @GET("api/itinerary")
    suspend fun getItinerary(@Query("tripId") tripId: String): ItineraryResponse

    @POST("api/itinerary")
    suspend fun addItineraryItem(@Body body: Map<String, Any?>): ItineraryItem

    @PATCH("api/itinerary")
    suspend fun updateItineraryItem(@Body body: Map<String, Any?>): ItineraryItem

    @HTTP(method = "DELETE", path = "api/itinerary", hasBody = true)
    suspend fun deleteItineraryItem(@Body body: Map<String, String>): Response<Unit>

    // ─── Polls ────────────────────────────────────────────────────────────────

    @GET("api/polls")
    suspend fun getPolls(@Query("tripId") tripId: String): List<PollWithVotes>

    @POST("api/polls")
    suspend fun createPoll(@Body body: Map<String, Any>): Poll

    @PATCH("api/polls")
    suspend fun votePoll(@Body body: Map<String, String>): Response<Unit>

    @HTTP(method = "DELETE", path = "api/polls", hasBody = true)
    suspend fun closePoll(@Body body: Map<String, String>): Response<Unit>

    // ─── Locks ────────────────────────────────────────────────────────────────

    @GET("api/locks/{tripId}")
    suspend fun getLocks(@Path("tripId") tripId: String): List<DecisionLock>

    @POST("api/locks")
    suspend fun lockDecision(@Body body: Map<String, Any>): DecisionLock

    // ─── Unlock Votes ─────────────────────────────────────────────────────────

    @GET("api/unlock-votes")
    suspend fun getUnlockVote(@Query("tripId") tripId: String): Response<UnlockVote?>

    @POST("api/unlock-votes")
    suspend fun requestUnlock(@Body body: Map<String, Any?>): UnlockVote

    @PATCH("api/unlock-votes")
    suspend fun castUnlockBallot(@Body body: Map<String, Any>): UnlockVote

    // ─── Photos ───────────────────────────────────────────────────────────────

    @GET("api/photos")
    suspend fun getPhotos(@Query("tripId") tripId: String): PhotosResponse

    @GET("api/upload-url")
    suspend fun getUploadUrl(@Query("tripId") tripId: String): UploadUrlResponse

    @POST("api/photos")
    suspend fun savePhoto(@Body body: Map<String, Any?>): TripPhoto

    @HTTP(method = "DELETE", path = "api/photos", hasBody = true)
    suspend fun deletePhoto(@Body body: Map<String, String>): Response<Unit>

    @POST("api/photos/reactions")
    suspend fun togglePhotoReaction(@Body body: Map<String, String>): PhotoReactionToggleResponse

    @GET("api/photos/comments")
    suspend fun getPhotoComments(@Query("photoId") photoId: String): PhotoCommentsResponse

    @POST("api/photos/comments")
    suspend fun addPhotoComment(@Body body: Map<String, String>): PhotoComment

    @HTTP(method = "DELETE", path = "api/photos/comments", hasBody = true)
    suspend fun deletePhotoComment(@Body body: Map<String, String>): Response<Unit>

    @GET("api/photos/albums")
    suspend fun getPhotoAlbums(@Query("tripId") tripId: String): PhotoAlbumsResponse

    @POST("api/photos/albums")
    suspend fun createPhotoAlbum(@Body body: Map<String, String>): PhotoAlbum

    @PATCH("api/photos/albums")
    suspend fun updatePhotoAlbum(@Body body: Map<String, Any?>): PhotoAlbum

    @HTTP(method = "DELETE", path = "api/photos/albums", hasBody = true)
    suspend fun deletePhotoAlbum(@Body body: Map<String, String>): Response<Unit>

    @POST("api/photos/highlights")
    suspend fun togglePhotoHighlight(@Body body: Map<String, String>): PhotoHighlightToggleResponse

    @GET("api/photos/download")
    suspend fun getPhotoDownloadList(@Query("tripId") tripId: String): PhotoDownloadResponse

    // ─── Activity ─────────────────────────────────────────────────────────────

    @GET("api/activity/{tripId}")
    suspend fun getActivity(
        @Path("tripId") tripId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): ActivityResponse

    // ─── Profile ──────────────────────────────────────────────────────────────

    @GET("api/profile")
    suspend fun getProfile(): User

    @PATCH("api/profile")
    suspend fun updateProfile(@Body body: Map<String, Any?>): User

    @DELETE("api/account")
    suspend fun deleteAccount(): Response<Unit>

    // ─── Pending Invites ──────────────────────────────────────────────────────

    @GET("api/pending-invites")
    suspend fun getPendingInvites(): List<PendingInvite>

    @POST("api/pending-invites")
    suspend fun respondToInvite(@Body body: Map<String, String>): InviteActionResponse

    // ─── Notification Preferences ─────────────────────────────────────────────

    @GET("api/notifications/preferences/{tripId}")
    suspend fun getNotificationPrefs(@Path("tripId") tripId: String): NotificationPref

    @PATCH("api/notifications/preferences")
    suspend fun updateNotificationPrefs(@Body body: Map<String, String>): NotificationPref

    // ─── Device Tokens (FCM) ──────────────────────────────────────────────────

    @POST("api/device-tokens")
    suspend fun registerDeviceToken(@Body body: Map<String, String>): Response<Unit>

    @HTTP(method = "DELETE", path = "api/device-tokens", hasBody = true)
    suspend fun unregisterDeviceToken(@Body body: Map<String, String>): Response<Unit>

    // ─── Global Overview ──────────────────────────────────────────────────────

    @GET("api/overview")
    suspend fun getOverview(): OverviewData

    // ─── Weather ──────────────────────────────────────────────────────────────

    @GET("api/weather")
    suspend fun getWeather(
        @Query("city") city: String,
        @Query("country") country: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): WeatherResponse

    // ─── Group Profile ────────────────────────────────────────────────────────

    @GET("api/group-profile")
    suspend fun getGroupProfile(@Query("tripId") tripId: String): GroupProfile

}
