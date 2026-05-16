package com.tripsyc.app.data.api.models

// Chat API response types
data class ChatUser(
    val id: String,
    val name: String? = null,
    val avatarUrl: String? = null
)

data class ReplyUser(
    val id: String,
    val name: String? = null
)

data class ReplyInfo(
    val id: String,
    val text: String,
    val imageUrl: String? = null,
    val userId: String,
    val user: ReplyUser
)

data class ChatMessageWithUser(
    val id: String,
    val tripId: String,
    val userId: String,
    val text: String,
    val imageUrl: String? = null,
    val replyToId: String? = null,
    val replyTo: ReplyInfo? = null,
    val isPinned: Boolean = false,
    val editedAt: String? = null,
    val createdAt: String? = null,
    val user: ChatUser,
    val reactions: List<GroupedReaction>? = null
)

data class MessagesResponse(
    val messages: List<ChatMessageWithUser>,
    val nextCursor: String? = null
)

data class GroupedReaction(
    val emoji: String,
    val count: Int,
    val userIds: List<String>
)

data class ReactionsResponse(
    val reactions: List<GroupedReaction>
)

data class ReadReceiptUser(
    val id: String,
    val name: String? = null,
    val avatarUrl: String? = null
)

data class ReadReceipt(
    val userId: String,
    val readAt: String,
    val user: ReadReceiptUser
)

data class ReadReceiptsResponse(
    val receipts: List<ReadReceipt>
)

// Expense API response types
data class SimpleUser(
    val id: String,
    val name: String? = null
)

data class SplitWithUser(
    val id: String,
    val expenseId: String,
    val userId: String,
    val user: SimpleUser,
    val amount: Double,
    val settled: Boolean
)

data class ExpenseWithUser(
    val id: String,
    val tripId: String,
    val paidBy: String,
    val paidByUser: SimpleUser,
    val title: String,
    val amount: Double,
    val currency: String,
    val category: ExpenseCategory,
    val date: String? = null,
    val splitType: SplitType,
    val createdAt: String? = null,
    val splits: List<SplitWithUser>
)

data class Balance(
    val from: String,
    val fromName: String,
    val to: String,
    val toName: String,
    val amount: Double
)

data class ExpensesResponse(
    val expenses: List<ExpenseWithUser>,
    val balances: List<Balance>,
    val tripCurrency: String? = null
)

// Budget API response types
data class BandRange(val min: Int, val max: Int)

data class BudgetAPIResponse(
    val memberCount: Int,
    val totalMembers: Int,
    val showBands: Boolean,
    val greenBand: BandRange? = null,
    val yellowBand: BandRange? = null,
    val hasHardCaps: Boolean,
    val lowestHardCap: Int? = null,
    val currency: String,
    val myBudget: MyBudgetResponse? = null
)

data class MyBudgetResponse(
    val budgetMax: Int,
    val budgetType: BudgetType
)

data class BudgetUpdateResponse(val success: Boolean, val id: String)

// Destinations API response types
data class DestinationsAPIResponse(
    val destinations: List<Destination>,
    val phase: String,
    val currency: String,
    val homeCity: String? = null
)

data class ShortlistResponse(
    val success: Boolean,
    val shortlisted: List<String>? = null,
    val destinationPhase: String? = null
)

data class DealbreakerToggleResponse(val dealbreaker: Boolean)
data class DealbreakerListResponse(val destinationIds: List<String>)

// Activity
data class ActivityResponse(
    val activities: List<Activity>,
    val nextCursor: String? = null
)

// Notes
data class NotesResponse(
    val notes: List<Note>,
    val nextCursor: String? = null
)

// Packing
data class PackingResponse(
    val items: List<PackingItem>
)

// Responsibilities
data class ResponsibilitiesResponse(
    val items: List<Responsibility>
)

// Itinerary
data class ItineraryResponse(
    val items: List<ItineraryItem>
)

// Photos
data class PhotosResponse(
    val photos: List<TripPhoto>
)

data class UploadUrlResponse(
    val uploadUrl: String,
    val blobUrl: String
)

data class PhotoAlbumsResponse(
    val albums: List<PhotoAlbum>
)

data class PhotoCommentsResponse(
    val comments: List<PhotoComment>
)

data class PhotoReactionToggleResponse(
    val toggled: String,
    val emoji: String
)

data class PhotoHighlightToggleResponse(
    val toggled: String,
    val highlightVotes: Int? = null,
    val isHighlight: Boolean? = null
)

data class DownloadPhoto(
    val id: String,
    val url: String,
    val filename: String
)

data class PhotoDownloadResponse(
    val tripName: String,
    val photos: List<DownloadPhoto>
)

// Polls
data class PollOptionWithVotes(
    val id: String,
    val pollId: String,
    val text: String,
    val voteCount: Int,
    val votedByMe: Boolean
)

data class PollWithVotes(
    val id: String,
    val tripId: String,
    val createdBy: String,
    val question: String,
    val multiSelect: Boolean,
    val closedAt: String? = null,
    val createdAt: String? = null,
    val options: List<PollOptionWithVotes>
)

// Global availability
data class GlobalAvailabilityListResponse(
    val entries: List<GlobalAvailability>
)

// Pending invites
data class InviteActionResponse(
    val success: Boolean,
    val action: String? = null,
    val tripId: String? = null
)

// Auth
data class OTPResponse(val success: Boolean, val error: String? = null)
data class VerifyCodeResponse(val success: Boolean, val redirect: String? = null)

// Settle all
data class SettleAllResponse(val settled: Int, val message: String? = null)

// Global Overview
data class TripCostDestination(
    val city: String,
    val country: String,
    val shortlisted: Boolean
)

data class TripCost(
    val tripId: String,
    val tripName: String,
    val currency: String,
    val coverImage: String? = null,
    val approxMonth: String? = null,
    val totalExpenses: Double = 0.0,
    val myShare: Double = 0.0,
    val iPaid: Double = 0.0,
    val memberCount: Int = 0,
    val myRole: String = "MEMBER",
    val myRsvp: String? = null,
    val status: String = "Planning",
    val lockedDates: String? = null,
    val lockedDestination: String? = null,
    val destinations: List<TripCostDestination> = emptyList(),
    val budgetMax: Double? = null,
    val budgetType: String? = null
)

data class SettlementAmount(
    val currency: String,
    val amount: Double
)

data class OverviewSettlement(
    val direction: String,
    val personId: String,
    val name: String,
    val amount: Double,
    val amounts: List<SettlementAmount>? = null,
    val convertedTotal: Double? = null,
    val displayCurrency: String? = null,
    val tripNames: List<String> = emptyList()
)

data class TravelBuddy(
    val name: String,
    val count: Int,
    val trips: List<String> = emptyList()
)

data class PackingAlert(
    val tripId: String,
    val tripName: String,
    val unpacked: Int,
    val total: Int
)

data class DeadlineAlert(
    val tripId: String,
    val tripName: String,
    val type: String,
    val daysLeft: Int
)

data class AvailAlert(
    val tripId: String,
    val tripName: String
)

data class OverviewRecentActivity(
    val id: String,
    val tripId: String,
    val tripName: String,
    val type: String,
    val message: String,
    val createdAt: String? = null
)

data class OverviewData(
    val tripCosts: List<TripCost> = emptyList(),
    val totalExpenses: Double = 0.0,
    val totalOwed: Double = 0.0,
    val totalOwedToYou: Double = 0.0,
    val settlements: List<OverviewSettlement> = emptyList(),
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val travelBuddies: List<TravelBuddy> = emptyList(),
    val packingAlerts: List<PackingAlert> = emptyList(),
    val deadlineAlerts: List<DeadlineAlert> = emptyList(),
    val availabilityAlerts: List<AvailAlert> = emptyList(),
    val recentActivity: List<OverviewRecentActivity> = emptyList(),
    val userCurrency: String = "USD"
)
