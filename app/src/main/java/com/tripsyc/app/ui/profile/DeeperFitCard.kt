package com.tripsyc.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.User
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Tier-3 group-fit dimensions. Each axis lifts a friction/strength
 * signal for the compatibility score, and a few of them feed into
 * Smart Plan generation directly (heat tolerance gates beach
 * destinations in hot months, party scale skews nightlife
 * suggestions, etc.). Compact segmented pickers instead of the iOS
 * dropdown sheets — same data, less UI bloat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeeperFitCard(profile: User, onSaved: (User) -> Unit) {
    var heatTolerance by remember(profile.id) { mutableStateOf(profile.heatTolerance ?: "") }
    var altitudeTolerance by remember(profile.id) { mutableStateOf(profile.altitudeTolerance ?: "") }
    var motionSickness by remember(profile.id) { mutableStateOf(profile.motionSickness ?: "") }
    var jetLagTolerance by remember(profile.id) { mutableStateOf(profile.jetLagTolerance ?: "") }
    var refundableOnly by remember(profile.id) { mutableStateOf(profile.refundableOnly == true) }
    var tippingNorm by remember(profile.id) { mutableStateOf(profile.tippingNorm ?: "") }
    var frontingComfort by remember(profile.id) { mutableStateOf(profile.frontingComfort ?: "") }
    var maxFront by remember(profile.id) { mutableStateOf(profile.maxComfortableFront?.toString() ?: "") }
    var morningMinutes by remember(profile.id) { mutableStateOf(profile.morningRoutineMinutes?.toString() ?: "") }
    var phoneCoverage by remember(profile.id) { mutableStateOf(profile.phoneCoveragePref ?: "") }
    var preferredTransport by remember(profile.id) { mutableStateOf(profile.preferredTransport ?: "") }
    var preferredFlightTime by remember(profile.id) { mutableStateOf(profile.preferredFlightTime ?: "") }
    var partyScale by remember(profile.id) { mutableStateOf(profile.partyScale ?: "") }
    var aloneHours by remember(profile.id) { mutableStateOf(profile.aloneTimeHoursPerDay?.toString() ?: "") }
    var conversationEnergy by remember(profile.id) { mutableStateOf(profile.conversationEnergy ?: "") }
    var cookingSkill by remember(profile.id) { mutableStateOf(profile.cookingSkill ?: "") }
    var navPref by remember(profile.id) { mutableStateOf(profile.navigationPreference ?: "") }
    var planningPatience by remember(profile.id) { mutableStateOf(profile.planningPatience ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Deeper Group Fit", fontWeight = FontWeight.SemiBold, color = Chalk900)
            Text(
                "Optional. Sharpens the compatibility score and Smart Plan suggestions.",
                fontSize = 12.sp,
                color = Chalk500
            )

            Section("Physical comfort") {
                SegmentedRow("Heat tolerance", heatTolerance, listOf(
                    "COLD_SENSITIVE" to "Cold-sens.",
                    "TEMPERATE" to "Temperate",
                    "HEAT_LOVER" to "Heat-lover"
                )) { heatTolerance = it }
                SegmentedRow("Altitude tolerance", altitudeTolerance, listOf(
                    "SENSITIVE" to "Sensitive",
                    "NEUTRAL" to "Neutral",
                    "UNAFFECTED" to "Unaffected"
                )) { altitudeTolerance = it }
                SegmentedRow("Motion sickness", motionSickness, listOf(
                    "STRONG" to "Strong",
                    "MILD" to "Mild",
                    "NONE" to "None"
                )) { motionSickness = it }
                SegmentedRow("Jet lag tolerance", jetLagTolerance, listOf(
                    "SLOW" to "Slow",
                    "AVERAGE" to "Average",
                    "BOUNCES_BACK" to "Bounces"
                )) { jetLagTolerance = it }
            }

            Section("Money") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = refundableOnly,
                        onCheckedChange = { refundableOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Coral)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Only book refundable", color = Chalk900, fontSize = 13.sp)
                }
                SegmentedRow("Tipping norm", tippingNorm, listOf(
                    "MINIMAL" to "Minimal",
                    "STANDARD" to "Standard",
                    "GENEROUS" to "Generous"
                )) { tippingNorm = it }
                SegmentedRow("Fronting comfort", frontingComfort, listOf(
                    "NONE" to "None",
                    "SMALL" to "Small",
                    "LARGE" to "Large"
                )) { frontingComfort = it }
                OutlinedTextField(
                    value = maxFront,
                    onValueChange = { maxFront = it.filter { c -> c.isDigit() } },
                    label = { Text("Max comfortable front (USD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
            }

            Section("Logistics") {
                OutlinedTextField(
                    value = morningMinutes,
                    onValueChange = { morningMinutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Morning routine (min)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                SegmentedRow("Phone coverage", phoneCoverage, listOf(
                    "ALWAYS_ON" to "Always on",
                    "WIFI_ONLY" to "Wi-Fi only",
                    "DARK_MODE" to "Off as much as poss."
                )) { phoneCoverage = it }
                SegmentedRow("Preferred transport", preferredTransport, listOf(
                    "PUBLIC" to "Public",
                    "RIDESHARE" to "Rideshare",
                    "WALKING" to "Walking",
                    "RENTAL" to "Rental"
                )) { preferredTransport = it }
                SegmentedRow("Preferred flight time", preferredFlightTime, listOf(
                    "EARLY" to "Early",
                    "MIDDAY" to "Midday",
                    "EVENING" to "Evening",
                    "REDEYE" to "Red-eye"
                )) { preferredFlightTime = it }
            }

            Section("Social vibe") {
                SegmentedRow("Party scale", partyScale, listOf(
                    "QUIET" to "Quiet",
                    "MODERATE" to "Moderate",
                    "FULL_SEND" to "Full send"
                )) { partyScale = it }
                OutlinedTextField(
                    value = aloneHours,
                    onValueChange = { aloneHours = it.filter { c -> c.isDigit() } },
                    label = { Text("Alone-time hours/day") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                SegmentedRow("Conversation energy", conversationEnergy, listOf(
                    "LISTENER" to "Listener",
                    "BALANCED" to "Balanced",
                    "TALKER" to "Talker"
                )) { conversationEnergy = it }
            }

            Section("Skills") {
                SegmentedRow("Cooking", cookingSkill, listOf(
                    "TAKEOUT" to "Takeout",
                    "BASIC" to "Basic",
                    "CONFIDENT" to "Confident"
                )) { cookingSkill = it }
                SegmentedRow("Navigation", navPref, listOf(
                    "MAPS" to "Maps",
                    "INSTINCT" to "Instinct",
                    "DEFER" to "Defer"
                )) { navPref = it }
                SegmentedRow("Planning patience", planningPatience, listOf(
                    "WING_IT" to "Wing it",
                    "MIXED" to "Mixed",
                    "STRUCTURED" to "Structured"
                )) { planningPatience = it }
            }

            if (error != null) Text(error!!, color = Danger, fontSize = 12.sp)

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        error = null
                        try {
                            val updated = ApiClient.apiService.updateProfile(
                                mapOf(
                                    "heatTolerance" to heatTolerance.ifEmpty { null },
                                    "altitudeTolerance" to altitudeTolerance.ifEmpty { null },
                                    "motionSickness" to motionSickness.ifEmpty { null },
                                    "jetLagTolerance" to jetLagTolerance.ifEmpty { null },
                                    "refundableOnly" to refundableOnly,
                                    "tippingNorm" to tippingNorm.ifEmpty { null },
                                    "frontingComfort" to frontingComfort.ifEmpty { null },
                                    "maxComfortableFront" to maxFront.toIntOrNull(),
                                    "morningRoutineMinutes" to morningMinutes.toIntOrNull(),
                                    "phoneCoveragePref" to phoneCoverage.ifEmpty { null },
                                    "preferredTransport" to preferredTransport.ifEmpty { null },
                                    "preferredFlightTime" to preferredFlightTime.ifEmpty { null },
                                    "partyScale" to partyScale.ifEmpty { null },
                                    "aloneTimeHoursPerDay" to aloneHours.toIntOrNull(),
                                    "conversationEnergy" to conversationEnergy.ifEmpty { null },
                                    "cookingSkill" to cookingSkill.ifEmpty { null },
                                    "navigationPreference" to navPref.ifEmpty { null },
                                    "planningPatience" to planningPatience.ifEmpty { null }
                                )
                            )
                            onSaved(updated)
                        } catch (e: Exception) {
                            error = e.message ?: "Couldn't save"
                        }
                        isSaving = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("Save details", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Chalk500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun SegmentedRow(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onPick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Chalk700, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (id, display) ->
                val isOn = selected == id
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOn) Coral else Chalk100,
                    onClick = { onPick(if (isOn) "" else id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        display,
                        color = if (isOn) Color.White else Chalk700,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Coral,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
