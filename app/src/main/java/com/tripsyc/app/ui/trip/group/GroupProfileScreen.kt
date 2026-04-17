package com.tripsyc.app.ui.trip.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.GroupProfile
import com.tripsyc.app.ui.common.LoadingView
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GroupProfileScreen(tripId: String) {
    var profile by remember { mutableStateOf<GroupProfile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                profile = ApiClient.apiService.getGroupProfile(tripId)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load group profile"
            }
            loading = false
        }
    }

    LaunchedEffect(tripId) { load() }

    when {
        loading && profile == null -> Box(Modifier.fillMaxSize()) { LoadingView("Loading group profile...") }
        error != null && profile == null -> Box(
            modifier = Modifier.fillMaxSize().background(Chalk50),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(error ?: "Error", color = Danger)
                Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Coral)) { Text("Retry") }
            }
        }
        else -> profile?.let { ProfileContent(it) }
    }
}

@Composable
private fun ProfileContent(p: GroupProfile) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Chalk50)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "${p.memberCount} member${if (p.memberCount == 1) "" else "s"}",
            color = Chalk500,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        p.birthdaysInTrip?.takeIf { it.isNotEmpty() }?.let { bdays ->
            Section(icon = Icons.Default.Cake, title = "Birthdays During Trip", accent = Gold) {
                bdays.forEach { b ->
                    BulletRow("${b.name} — ${monthName(b.month)} ${b.day}", color = Gold)
                }
            }
        }

        p.groupHardNos?.takeIf { it.isNotEmpty() }?.let { nos ->
            Section(icon = Icons.Default.Block, title = "Hard Nos", accent = Coral) {
                nos.forEach { n ->
                    Chip(label = "${n.item} (${n.names.joinToString(", ")})", color = Coral)
                }
            }
        }

        if (p.passportWarnings.isNotEmpty()) {
            Section(icon = Icons.Default.Warning, title = "Passport Alerts", accent = Coral) {
                p.passportWarnings.forEach { w ->
                    BulletRow("${w.name} — expires ${w.expiry}", color = Coral)
                }
            }
        }

        if (p.roomingAlerts.isNotEmpty()) {
            Section(icon = Icons.Default.Hotel, title = "Rooming Conflicts", accent = Gold) {
                p.roomingAlerts.forEach { BulletRow(it, color = Gold) }
            }
        }

        if (p.dietaryNeeds.isNotEmpty()) {
            Section(icon = Icons.Default.Restaurant, title = "Dietary Needs", accent = Sage) {
                p.dietaryNeeds.forEach { Chip(label = friendlyLabel(it), color = Sage) }
            }
        }

        p.environmentalAllergies?.takeIf { it.isNotEmpty() }?.let { allergies ->
            Section(icon = Icons.Default.Healing, title = "Allergies", accent = Coral) {
                allergies.forEach { a ->
                    BulletRow("${a.allergy} — ${a.names.joinToString(", ")}", color = Coral)
                }
            }
        }

        if (p.budgetRanges.isNotEmpty()) {
            Section(icon = Icons.Default.AttachMoney, title = "Budget Ranges", accent = Sage) {
                p.budgetRanges.forEach { b ->
                    val range = when {
                        b.min != null && b.max != null -> "${b.currency} ${b.min}–${b.max}"
                        b.min != null -> "${b.currency} ${b.min}+"
                        b.max != null -> "up to ${b.currency} ${b.max}"
                        else -> "no range set"
                    }
                    BulletRow("${b.name}: $range")
                }
            }
        }

        if (p.commonInterests.isNotEmpty()) {
            Section(icon = Icons.Default.Favorite, title = "Common Interests", accent = Dusk) {
                p.commonInterests.forEach { i ->
                    Chip(label = "${friendlyLabel(i.interest)} (${i.count})", color = Dusk)
                }
            }
        }

        if (p.activityLevels.isNotEmpty()) {
            Section(
                icon = Icons.Default.DirectionsRun,
                title = "Activity Levels",
                accent = if (p.hasActivityConflict) Coral else Sage
            ) {
                if (p.hasActivityConflict) {
                    Text(
                        "Mismatch detected — split sub-groups for high-energy activities.",
                        color = Coral, fontSize = 11.sp
                    )
                }
                p.activityLevels.forEach { BulletRow("${it.name}: ${friendlyLabel(it.level)}") }
            }
        }

        if (p.planningStyles.isNotEmpty()) {
            Section(
                icon = Icons.Default.Schedule,
                title = "Planning Styles",
                accent = if (p.hasPlanningConflict) Coral else Dusk
            ) {
                p.planningStyles.forEach { BulletRow("${it.name}: ${friendlyLabel(it.style)}") }
            }
        }

        if (p.tripDurationPrefs.isNotEmpty()) {
            Section(icon = Icons.Default.AccessTime, title = "Trip Duration", accent = Dusk) {
                p.tripDurationPrefs.forEach { BulletRow("${it.name}: ${friendlyLabel(it.pref)}") }
            }
        }

        if (p.accommodationPrefs.isNotEmpty()) {
            Section(icon = Icons.Default.Bed, title = "Accommodation", accent = Sage) {
                p.accommodationPrefs.forEach { BulletRow("${it.name}: ${friendlyLabel(it.pref)}") }
            }
        }

        if (p.splitPreferences.isNotEmpty()) {
            Section(icon = Icons.Default.AccountBalance, title = "Bill Split Preference", accent = Sage) {
                p.dominantSplitPreference?.let {
                    Text(
                        "Group leans: ${friendlyLabel(it)}",
                        color = Chalk700, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                p.splitPreferences.forEach { BulletRow("${it.name}: ${friendlyLabel(it.pref)}") }
            }
        }

        if (p.cashPreferences.isNotEmpty()) {
            Section(icon = Icons.Default.Payments, title = "Cash Preference", accent = Sage) {
                p.cashPreferences.forEach { BulletRow("${it.name}: ${friendlyLabel(it.pref)}") }
            }
        }

        if (p.flexibleWorkers.isNotEmpty() || p.officeWorkers.isNotEmpty()) {
            Section(icon = Icons.Default.Work, title = "Work Schedule", accent = Dusk) {
                if (p.flexibleWorkers.isNotEmpty()) BulletRow("Flexible: ${p.flexibleWorkers.joinToString(", ")}")
                if (p.officeWorkers.isNotEmpty()) BulletRow("In-office: ${p.officeWorkers.joinToString(", ")}")
            }
        }

        if (p.drivers.isNotEmpty() || p.carOwners?.isNotEmpty() == true) {
            Section(icon = Icons.Default.DirectionsCar, title = "Drivers & Cars", accent = Sage) {
                if (p.drivers.isNotEmpty()) BulletRow("Can drive: ${p.drivers.joinToString(", ")}")
                p.carOwners?.takeIf { it.isNotEmpty() }?.let {
                    BulletRow("Has a car: ${it.joinToString(", ")}")
                }
            }
        }

        if (p.homeAirports.isNotEmpty()) {
            Section(icon = Icons.Default.Flight, title = "Home Airports", accent = Dusk) {
                p.homeAirports.forEach { BulletRow("${it.name}: ${it.airport}") }
            }
        }

        if (p.sharedLanguages.isNotEmpty()) {
            Section(icon = Icons.Default.Language, title = "Shared Languages", accent = Dusk) {
                p.sharedLanguages.forEach { Chip("${it.lang} (${it.count})", Dusk) }
            }
        }

        if (p.nationalities.isNotEmpty()) {
            Section(icon = Icons.Default.Public, title = "Nationalities", accent = Sage) {
                p.nationalities.forEach { BulletRow("${it.name}: ${it.nationality}") }
            }
        }

        p.flightSeatPrefs?.takeIf { it.isNotEmpty() }?.let { seats ->
            Section(icon = Icons.Default.AirlineSeatReclineNormal, title = "Flight Seat Prefs", accent = Dusk) {
                seats.forEach { BulletRow("${it.name}: ${friendlyLabel(it.seat)}") }
            }
        }

        p.travelPersonaTags?.takeIf { it.isNotEmpty() }?.let { tags ->
            Section(icon = Icons.Default.EmojiPeople, title = "Travel Personas", accent = Dusk) {
                tags.forEach { Chip("${it.name}: ${friendlyLabel(it.tag)}", Dusk) }
            }
        }

        p.walkingComforts?.takeIf { it.isNotEmpty() }?.let { walks ->
            Section(icon = Icons.Default.DirectionsWalk, title = "Walking Comfort", accent = Sage) {
                p.walkingComfortFloor?.let {
                    Text(
                        "Group floor: ${friendlyLabel(it.pref)} (set by ${it.name})",
                        color = Chalk700, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                walks.forEach { BulletRow("${it.name}: ${friendlyLabel(it.pref)}") }
            }
        }

        p.photoPermissionPrefs?.takeIf { it.isNotEmpty() }?.let { perms ->
            Section(icon = Icons.Default.PhotoCamera, title = "Photo Permissions", accent = Dusk) {
                perms.forEach { BulletRow("${it.name}: ${friendlyLabel(it.pref)}") }
            }
        }

        if (p.memberCountriesVisited.isNotEmpty()) {
            Section(icon = Icons.Default.Map, title = "Countries Visited", accent = Sage) {
                p.memberCountriesVisited.forEach { m ->
                    BulletRow("${m.name}: ${m.countries.size} countries")
                }
            }
        }

        if (p.memberBucketList.isNotEmpty()) {
            Section(icon = Icons.Default.Star, title = "Bucket Lists", accent = Gold) {
                p.memberBucketList.forEach { m ->
                    BulletRow("${m.name}: ${m.items.size} bucket-list items")
                }
            }
        }

        if (p.isOrganizer && p.emergencyContacts.isNotEmpty()) {
            Section(icon = Icons.Default.LocalHospital, title = "Emergency Contacts (organizer-only)", accent = Coral) {
                p.emergencyContacts.forEach { c ->
                    BulletRow("${c.memberName} → ${c.contactName}${c.contactPhone?.let { ", $it" } ?: ""}")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accent: Color,
    body: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                }
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Chalk900
                )
            }
            body()
        }
    }
}

@Composable
private fun BulletRow(text: String, color: Color = Chalk700) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(text, color = color, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Chip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.30f))
    ) {
        Text(
            label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun monthName(m: Int): String = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
).getOrElse(m - 1) { "Mon $m" }

private fun friendlyLabel(raw: String): String =
    raw.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
