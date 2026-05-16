package com.tripsyc.app.data.api.models

import com.google.gson.annotations.SerializedName

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class TravelStyle { CHILL, ADVENTURE, PARTY, CULTURAL }
enum class AvailabilityStatus { AVAILABLE, FLEXIBLE, UNAVAILABLE }
enum class VoteValue { UP, DOWN }
enum class LockType { DATE, DESTINATION }
enum class MemberRole { CREATOR, CO_ORGANIZER, MEMBER }
enum class RsvpStatus { GOING, MAYBE, CANT_MAKE_IT }
enum class BudgetType { HARD, SOFT }
enum class DestinationPhase { SUGGEST, SHORTLIST, FINAL }
enum class UnlockStatus { PENDING, APPROVED, REJECTED }
enum class ItineraryCategory { FLIGHT, HOTEL, ACTIVITY, RESTAURANT, TRANSPORT, EMERGENCY, INFO, OTHER }
enum class ExpenseCategory { FOOD, TRANSPORT, ACCOMMODATION, ACTIVITIES, SHOPPING, DRINKS, OTHER }
enum class SplitType { EQUAL, CUSTOM }
enum class NotificationMode { ALL, DIGEST_DAILY, DIGEST_WEEKLY, LOCKS_ONLY, MUTED }

// ─── User ─────────────────────────────────────────────────────────────────────

data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val city: String? = null,
    val bio: String? = null,
    val travelStyle: TravelStyle? = null,
    val currency: String? = null,
    val isAdmin: Boolean? = null,
    val createdAt: String? = null,
    val isImpersonating: Boolean? = null,
    val realAdminEmail: String? = null
)

// ─── Trip ────────────────────────────────────────────────────────────────────

data class TripCount(
    val members: Int? = null
)

data class Trip(
    val id: String,
    val name: String,
    val approxMonth: String? = null,
    val coverImage: String? = null,
    val currency: String = "USD",
    val timezone: String = "UTC",
    val inviteCode: String = "",
    val createdBy: String = "",
    val createdAt: String? = null,
    val destinationPhase: DestinationPhase? = null,
    val dateDeadline: String? = null,
    val destDeadline: String? = null,
    val invitesDisabled: Boolean? = null,
    val members: List<TripMember>? = null,
    val locks: List<DecisionLock>? = null,
    val destinations: List<Destination>? = null,
    @SerializedName("_count") val count: TripCount? = null
)

// ─── TripMember ──────────────────────────────────────────────────────────────

data class TripMember(
    val id: String,
    val userId: String,
    val tripId: String? = null,
    val name: String = "",
    val homeCity: String? = null,
    val travelStyle: TravelStyle = TravelStyle.CHILL,
    val role: MemberRole = MemberRole.MEMBER,
    val rsvp: RsvpStatus? = null,
    val rsvpNote: String? = null,
    // ISO yyyy-MM-dd. Set when a member is only attending part of a
    // locked-date trip; feeds expense-split window exclusion.
    val attendFrom: String? = null,
    val attendUntil: String? = null,
    // Plus-one (guest the member is bringing) and arrival logistics —
    // server stores both as JSON strings; PlusOneInfo / ArrivalInfo
    // helpers below parse them on demand. We hold the raw string so a
    // round-trip read/write preserves any forward-compat keys the
    // server adds later.
    val plusOne: String? = null,
    val arrivalInfo: String? = null,
    // Preferred off-app messaging — surfaces in the group chat handle row
    // so members can ping each other on iMessage/WhatsApp/etc.
    val chatPlatform: String? = null,
    val chatHandle: String? = null,
    val joinedAt: String? = null,
    val avatarUrl: String? = null,
    val email: String? = null,
    val user: User? = null
)

data class PlusOneInfo(
    val bringing: Boolean = false,
    val name: String? = null,
    val relationship: String? = null,
    val costSplit: String? = null
)

data class ArrivalInfo(
    val flightNumber: String? = null,
    val arrivalTime: String? = null,
    val airport: String? = null,
    val terminal: String? = null,
    val needsPickup: Boolean? = null
)

// ─── PastCoTraveler ──────────────────────────────────────────────────────────

data class PastCoTraveler(
    val userId: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val sharedTripCount: Int = 1,
    val lastTripId: String,
    val lastTripName: String,
    val lastTripAt: String? = null
)

data class PastCoTravelersResponse(
    val coTravelers: List<PastCoTraveler>
)

// ─── DecisionLock ────────────────────────────────────────────────────────────

data class DecisionLock(
    val id: String,
    val tripId: String,
    val lockType: LockType,
    val locked: Boolean = false,
    val lockedValue: String? = null,
    val lockedAt: String? = null,
    val unlockedAt: String? = null,
    val budgetSnapshot: String? = null
)

// ─── Destination ─────────────────────────────────────────────────────────────

data class DistanceInfo(
    val km: Double,
    val miles: Double,
    val flightTime: String? = null,
    val driveTime: String? = null
)

data class Destination(
    val id: String,
    val tripId: String,
    val city: String,
    val country: String,
    val imageUrl: String? = null,
    val estimatedCostMin: Int? = null,
    val estimatedCostMax: Int? = null,
    val addedBy: String = "",
    val shortlisted: Boolean = false,
    val createdAt: String? = null,
    val votes: List<Vote>? = null,
    val comments: List<Comment>? = null,
    val dealbreakers: List<Dealbreaker>? = null,
    val distance: DistanceInfo? = null,
    val affordability: String? = null
)

// ─── Vote ────────────────────────────────────────────────────────────────────

data class Vote(
    val id: String,
    val userId: String,
    val tripId: String,
    val destinationId: String,
    val value: VoteValue,
    val rank: Int? = null,
    val createdAt: String? = null
)

// ─── Comment ─────────────────────────────────────────────────────────────────

data class CommentUser(
    val id: String,
    val name: String? = null,
    val avatarUrl: String? = null
)

data class Comment(
    val id: String,
    val destinationId: String,
    val userId: String,
    val text: String,
    val parentId: String? = null,
    val createdAt: String? = null,
    val replies: List<Comment>? = null,
    val userName: String? = null,
    val user: CommentUser? = null
)

// ─── Dealbreaker ─────────────────────────────────────────────────────────────

data class Dealbreaker(
    val id: String,
    val destinationId: String,
    val userId: String,
    val createdAt: String? = null
)

// ─── BudgetConstraint ────────────────────────────────────────────────────────

data class BudgetConstraint(
    val id: String,
    val userId: String,
    val tripId: String,
    val budgetMax: Int,
    val budgetType: BudgetType,
    @SerializedName("private") val isPrivate: Boolean = true,
    val createdAt: String? = null
)

data class BudgetBand(
    val min: Int,
    val max: Int,
    val count: Int = 0,
    val percentage: Double = 0.0
)

data class BudgetResponse(
    val bands: List<BudgetBand> = emptyList(),
    val userBudget: BudgetConstraint? = null,
    val tripCurrency: String = "USD"
)

// ─── Expense ─────────────────────────────────────────────────────────────────

data class ExpenseSplit(
    val id: String,
    val expenseId: String,
    val userId: String,
    val amount: Double,
    val settled: Boolean
)

data class Expense(
    val id: String,
    val tripId: String,
    val paidBy: String,
    val title: String,
    val amount: Double,
    val currency: String,
    val category: ExpenseCategory,
    val date: String? = null,
    val splitType: SplitType,
    val createdAt: String? = null,
    val splits: List<ExpenseSplit>? = null
)

// ─── ChatMessage ─────────────────────────────────────────────────────────────

data class ChatReaction(
    val id: String,
    val messageId: String,
    val userId: String,
    val emoji: String,
    val createdAt: String? = null
)

data class ChatMessage(
    val id: String,
    val tripId: String,
    val userId: String,
    val text: String,
    val createdAt: String? = null,
    val reactions: List<ChatReaction>? = null,
    val userName: String? = null,
    val userAvatar: String? = null
)

// ─── Poll ────────────────────────────────────────────────────────────────────

data class PollVote(
    val id: String,
    val pollOptionId: String,
    val userId: String,
    val createdAt: String? = null
)

data class PollOption(
    val id: String,
    val pollId: String,
    val text: String,
    val createdAt: String? = null,
    val votes: List<PollVote>? = null
)

data class Poll(
    val id: String,
    val tripId: String,
    val createdBy: String,
    val question: String,
    val multiSelect: Boolean = false,
    val closedAt: String? = null,
    val createdAt: String? = null,
    val options: List<PollOption>? = null
)

// ─── ItineraryItem ───────────────────────────────────────────────────────────

data class ItineraryItem(
    val id: String,
    val tripId: String,
    val addedBy: String,
    val category: ItineraryCategory,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val date: String? = null,
    val time: String? = null,
    val endDate: String? = null,
    val endTime: String? = null,
    val url: String? = null,
    val phone: String? = null,
    val confirmationCode: String? = null,
    val sortOrder: Int = 0,
    val createdAt: String? = null
)

// ─── Activity ────────────────────────────────────────────────────────────────

data class Activity(
    val id: String,
    val tripId: String,
    val userId: String? = null,
    val type: String,
    val message: String,
    val createdAt: String? = null
)

// ─── Note ────────────────────────────────────────────────────────────────────

data class NoteUser(
    val id: String,
    val name: String? = null
)

data class Note(
    val id: String,
    val tripId: String,
    val userId: String,
    val title: String,
    val url: String? = null,
    val body: String? = null,
    val createdAt: String? = null,
    val user: NoteUser? = null
)

// ─── PackingItem ─────────────────────────────────────────────────────────────

data class PackingItem(
    val id: String,
    val tripId: String,
    val userId: String,
    val text: String,
    val packed: Boolean = false,
    val createdAt: String? = null
)

// ─── Responsibility ──────────────────────────────────────────────────────────

data class Responsibility(
    val id: String,
    val tripId: String,
    val title: String,
    val description: String? = null,
    val assignedTo: String? = null,
    val completed: Boolean = false,
    val createdAt: String? = null
)

// ─── TripPhoto ───────────────────────────────────────────────────────────────

data class PhotoUser(
    val id: String,
    val name: String? = null,
    val avatarUrl: String? = null
)

data class ReactionSummary(
    val emoji: String,
    val count: Int,
    val mine: Boolean
)

data class TripPhoto(
    val id: String,
    val tripId: String,
    val userId: String,
    val url: String,
    val caption: String? = null,
    val mediaType: String? = null,
    val locationName: String? = null,
    val albumId: String? = null,
    val isHighlight: Boolean? = null,
    val highlightVotes: Int? = null,
    val createdAt: String? = null,
    val user: PhotoUser? = null,
    val reactions: List<ReactionSummary>? = null,
    val commentCount: Int? = null
) {
    val isVideo: Boolean get() = mediaType == "video"
}

data class PhotoAlbumCount(val photos: Int)

data class PhotoAlbum(
    val id: String,
    val tripId: String,
    val name: String,
    val coverUrl: String? = null,
    val sortOrder: Int? = null,
    val createdAt: String? = null,
    @com.google.gson.annotations.SerializedName("_count") val count: PhotoAlbumCount? = null
) {
    val photoCount: Int get() = count?.photos ?: 0
}

data class PhotoComment(
    val id: String,
    val photoId: String,
    val userId: String,
    val text: String,
    val createdAt: String? = null,
    val user: PhotoUser? = null
)

// ─── Availability ────────────────────────────────────────────────────────────

data class AvailabilityUser(
    val id: String,
    val name: String? = null
)

data class Availability(
    val id: String,
    val userId: String,
    val tripId: String,
    val date: String,
    val status: AvailabilityStatus,
    val user: AvailabilityUser? = null
)

data class UserAvailabilityStatus(
    val userId: String,
    val userName: String? = null,
    val status: AvailabilityStatus
)

data class AvailabilityAggregate(
    val date: String,
    val available: Int,
    val flexible: Int,
    val unavailable: Int,
    val userStatuses: List<UserAvailabilityStatus>? = null
)

data class BestDate(
    val date: String,
    val available: Int,
    val flexible: Int,
    val unavailable: Int,
    val total: Int
)

data class AvailabilityResponse(
    val raw: List<Availability>,
    val aggregated: Map<String, DateAvailabilityCounts>,
    val bestDates: List<BestDate>,
    val memberCount: Int,
    val threshold: Int
)

data class DateAvailabilityCounts(
    val available: Int,
    val flexible: Int,
    val unavailable: Int,
    val total: Int
)

// ─── GlobalAvailability ──────────────────────────────────────────────────────

data class GlobalAvailability(
    val id: String,
    val date: String,
    val status: AvailabilityStatus
)

// ─── PendingInvite ───────────────────────────────────────────────────────────

data class PendingInviteTrip(
    val id: String,
    val name: String,
    val approxMonth: String? = null,
    val coverImage: String? = null,
    val createdAt: String? = null,
    @SerializedName("_count") val count: TripCount? = null
)

data class PendingInvite(
    val id: String,
    val createdAt: String? = null,
    val trip: PendingInviteTrip
)

// ─── UnlockVote ──────────────────────────────────────────────────────────────

data class UnlockBallot(
    val id: String,
    val unlockVoteId: String,
    val userId: String,
    val approve: Boolean,
    val createdAt: String? = null
)

data class UnlockVote(
    val id: String,
    val tripId: String,
    val lockType: LockType,
    val requestedBy: String,
    val reason: String? = null,
    val status: UnlockStatus = UnlockStatus.PENDING,
    val expiresAt: String,
    val createdAt: String? = null,
    val ballots: List<UnlockBallot>? = null,
    val ballotsCount: Int? = null,
    val approveCount: Int? = null,
    val rejectCount: Int? = null,
    val memberCount: Int? = null
)

// ─── NotificationPref ────────────────────────────────────────────────────────

data class NotificationPref(
    val id: String,
    val userId: String,
    val tripId: String,
    val mode: NotificationMode
)

// ─── Weather ─────────────────────────────────────────────────────────────────

data class WeatherDay(
    val day: String,
    val temp: Int,
    val low: Int? = null,
    val icon: String,
    val description: String? = null,
    val precip: Double? = null
)

data class WeatherCurrent(
    val temp: Int,
    val description: String,
    val icon: String
)

data class WeatherMatched(
    val city: String,
    val country: String,
    val admin1: String? = null,
    val countryMismatch: Boolean
)

data class WeatherResponse(
    val historical: Boolean,
    val current: WeatherCurrent,
    val forecast: List<WeatherDay>,
    val matched: WeatherMatched? = null
)

// ─── GroupProfile ────────────────────────────────────────────────────────────

data class MemberBudgetRange(
    val name: String,
    val min: Int? = null,
    val max: Int? = null,
    val currency: String
)

data class NamedPref(val name: String, val pref: String)
data class NamedLevel(val name: String, val level: String)
data class NamedStyle(val name: String, val style: String)
data class InterestCount(val interest: String, val count: Int)
data class NamedAirport(val name: String, val airport: String)
data class LangCount(val lang: String, val count: Int)
data class NamedNationality(val name: String, val nationality: String)
data class PassportWarning(val name: String, val expiry: String, val daysUntilTrip: Int? = null)
data class MemberCountriesVisited(val name: String, val countries: List<String>)
data class MemberBucketList(val name: String, val items: List<String>)
data class NamedSeat(val name: String, val seat: String)
data class NamedTag(val name: String, val tag: String)
data class GroupHardNo(val item: String, val names: List<String>)
data class GroupAllergy(val allergy: String, val names: List<String>)
data class BirthdayInTrip(val name: String, val month: Int, val day: Int)
data class BlackoutRange(val start: String, val end: String)
data class MemberBlackout(val name: String, val ranges: List<BlackoutRange>)
data class MemberPTOLimit(val name: String, val ptoRemainingDays: Int)
data class EmergencyContact(
    val memberName: String,
    val contactName: String,
    val contactPhone: String? = null,
    val bloodType: String? = null,
    val medicalNotes: String? = null,
    val insuranceProvider: String? = null
)

data class GroupProfile(
    val memberCount: Int,
    val isOrganizer: Boolean,
    val dietaryNeeds: List<String> = emptyList(),
    val budgetRanges: List<MemberBudgetRange> = emptyList(),
    val splitPreferences: List<NamedPref> = emptyList(),
    val dominantSplitPreference: String? = null,
    val cashPreferences: List<NamedPref> = emptyList(),
    val commonInterests: List<InterestCount> = emptyList(),
    val activityLevels: List<NamedLevel> = emptyList(),
    val hasActivityConflict: Boolean = false,
    val planningStyles: List<NamedStyle> = emptyList(),
    val hasPlanningConflict: Boolean = false,
    val tripDurationPrefs: List<NamedPref> = emptyList(),
    val roomingAlerts: List<String> = emptyList(),
    val accommodationPrefs: List<NamedPref> = emptyList(),
    val flexibleWorkers: List<String> = emptyList(),
    val officeWorkers: List<String> = emptyList(),
    val drivers: List<String> = emptyList(),
    val carOwners: List<String>? = null,
    val homeAirports: List<NamedAirport> = emptyList(),
    val sharedLanguages: List<LangCount> = emptyList(),
    val nationalities: List<NamedNationality> = emptyList(),
    val passportWarnings: List<PassportWarning> = emptyList(),
    val flightSeatPrefs: List<NamedSeat>? = null,
    val groupHardNos: List<GroupHardNo>? = null,
    val environmentalAllergies: List<GroupAllergy>? = null,
    val photoPermissionPrefs: List<NamedPref>? = null,
    val walkingComforts: List<NamedPref>? = null,
    val walkingComfortFloor: NamedPref? = null,
    val travelPersonaTags: List<NamedTag>? = null,
    val birthdaysInTrip: List<BirthdayInTrip>? = null,
    val memberBlackouts: List<MemberBlackout>? = null,
    val memberPTOLimits: List<MemberPTOLimit>? = null,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val memberCountriesVisited: List<MemberCountriesVisited> = emptyList(),
    val memberBucketList: List<MemberBucketList> = emptyList()
)

// ─── API Response Wrappers ───────────────────────────────────────────────────

data class ApiSuccess(val success: Boolean)
data class ApiError(val error: String)
