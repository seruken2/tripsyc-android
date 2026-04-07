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
    val createdAt: String? = null
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
    val joinedAt: String? = null,
    val avatarUrl: String? = null,
    val email: String? = null,
    val user: User? = null
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
    val email: String,
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
    val email: String,
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
    val email: String,
    val name: String? = null,
    val avatarUrl: String? = null
)

data class TripPhoto(
    val id: String,
    val tripId: String,
    val userId: String,
    val url: String,
    val caption: String? = null,
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
    val status: UnlockStatus,
    val expiresAt: String,
    val createdAt: String? = null,
    val ballots: List<UnlockBallot>? = null,
    val ballotsCount: Int? = null,
    val approveCount: Int? = null,
    val rejectCount: Int? = null
)

// ─── NotificationPref ────────────────────────────────────────────────────────

data class NotificationPref(
    val id: String,
    val userId: String,
    val tripId: String,
    val mode: NotificationMode
)

// ─── API Response Wrappers ───────────────────────────────────────────────────

data class ApiSuccess(val success: Boolean)
data class ApiError(val error: String)
