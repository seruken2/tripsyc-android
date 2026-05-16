package com.tripsyc.app.ui.trip.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.TripMember
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Self-information card — what the viewer wants the group to know
 * about *their* trip-shape: are they bringing a plus-one, what's
 * their arrival plan, and where can the group ping them off-app?
 *
 * Each field is debounced through a single "Save my info" button so
 * partial typing doesn't generate dozens of PATCH calls. The server
 * stores plusOne and arrivalInfo as JSON strings, so we encode here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripInfoCard(member: TripMember, onSaved: () -> Unit) {
    // Decode JSON-on-the-wire into editable form state.
    val initialPlus = remember(member.plusOne) { decodePlusOne(member.plusOne) }
    val initialArrival = remember(member.arrivalInfo) { decodeArrival(member.arrivalInfo) }

    var bringingPlusOne by remember(member.id) { mutableStateOf(initialPlus.bringing) }
    var plusOneName by remember(member.id) { mutableStateOf(initialPlus.name) }
    var plusOneRelationship by remember(member.id) { mutableStateOf(initialPlus.relationship) }
    var plusOneCostSplit by remember(member.id) { mutableStateOf(initialPlus.costSplit) }

    var flightNumber by remember(member.id) { mutableStateOf(initialArrival.flightNumber) }
    var arrivalTime by remember(member.id) { mutableStateOf(initialArrival.arrivalTime) }
    var airport by remember(member.id) { mutableStateOf(initialArrival.airport) }
    var terminal by remember(member.id) { mutableStateOf(initialArrival.terminal) }
    var needsPickup by remember(member.id) { mutableStateOf(initialArrival.needsPickup) }

    val platforms = listOf(
        "IMESSAGE" to "iMessage",
        "WHATSAPP" to "WhatsApp",
        "TELEGRAM" to "Telegram",
        "SIGNAL" to "Signal",
        "DISCORD" to "Discord",
        "MESSENGER" to "Messenger",
        "LINE" to "Line",
        "KAKAOTALK" to "KakaoTalk",
        "OTHER" to "Other"
    )
    var chatPlatform by remember(member.id) { mutableStateOf(member.chatPlatform ?: "") }
    var chatHandle by remember(member.id) { mutableStateOf(member.chatHandle ?: "") }
    var platformMenuOpen by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("My Trip Info", fontWeight = FontWeight.SemiBold, color = Chalk900)
            Text(
                "Optional. Helps the group plan pickups, splits, and reach you off-app.",
                fontSize = 12.sp,
                color = Chalk500
            )

            // ── Plus one ─────────────────────────────────────────
            SectionLabel("Plus-one")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = bringingPlusOne,
                    onCheckedChange = { bringingPlusOne = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Coral)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (bringingPlusOne) "Bringing someone" else "Solo",
                    color = Chalk900,
                    fontSize = 14.sp
                )
            }
            if (bringingPlusOne) {
                OutlinedTextField(
                    value = plusOneName,
                    onValueChange = { if (it.length <= 80) plusOneName = it },
                    label = { Text("Their name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = plusOneRelationship,
                    onValueChange = { if (it.length <= 40) plusOneRelationship = it },
                    label = { Text("Relationship (partner, sibling…)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = plusOneCostSplit,
                    onValueChange = { if (it.length <= 40) plusOneCostSplit = it },
                    label = { Text("Cost split (e.g. I'll cover, half-split)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
            }

            // ── Arrival info ─────────────────────────────────────
            SectionLabel("Arrival")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = flightNumber,
                    onValueChange = { if (it.length <= 20) flightNumber = it },
                    label = { Text("Flight #") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = airport,
                    onValueChange = { if (it.length <= 4) airport = it.uppercase() },
                    label = { Text("Airport") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = arrivalTime,
                    onValueChange = { if (it.length <= 40) arrivalTime = it },
                    label = { Text("Arrival time") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = terminal,
                    onValueChange = { if (it.length <= 10) terminal = it },
                    label = { Text("Terminal") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = needsPickup,
                    onCheckedChange = { needsPickup = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Coral)
                )
                Spacer(Modifier.width(8.dp))
                Text("Need a pickup", color = Chalk900, fontSize = 14.sp)
            }

            // ── Chat handle ──────────────────────────────────────
            SectionLabel("Off-app chat")
            ExposedDropdownMenuBox(
                expanded = platformMenuOpen,
                onExpandedChange = { platformMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = platforms.firstOrNull { it.first == chatPlatform }?.second
                        ?: "Pick a platform",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Platform") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(platformMenuOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                ExposedDropdownMenu(
                    expanded = platformMenuOpen,
                    onDismissRequest = { platformMenuOpen = false }
                ) {
                    platforms.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                chatPlatform = id
                                platformMenuOpen = false
                            }
                        )
                    }
                }
            }
            if (chatPlatform.isNotEmpty()) {
                OutlinedTextField(
                    value = chatHandle,
                    onValueChange = { if (it.length <= 80) chatHandle = it },
                    label = { Text("Your handle / number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
            }

            if (error != null) {
                Text(error!!, color = Danger, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        error = null
                        try {
                            val body = buildMap<String, Any?> {
                                put(
                                    "plusOne",
                                    if (bringingPlusOne) {
                                        mapOf(
                                            "bringing" to true,
                                            "name" to plusOneName.trim().ifBlank { null },
                                            "relationship" to plusOneRelationship.trim().ifBlank { null },
                                            "costSplit" to plusOneCostSplit.trim().ifBlank { null }
                                        )
                                    } else mapOf("bringing" to false)
                                )
                                put(
                                    "arrivalInfo",
                                    mapOf(
                                        "flightNumber" to flightNumber.trim().ifBlank { null },
                                        "arrivalTime" to arrivalTime.trim().ifBlank { null },
                                        "airport" to airport.trim().ifBlank { null },
                                        "terminal" to terminal.trim().ifBlank { null },
                                        "needsPickup" to needsPickup
                                    )
                                )
                                put("chatPlatform", chatPlatform.ifBlank { null })
                                put("chatHandle", chatHandle.trim().ifBlank { null })
                            }
                            ApiClient.apiService.updateMember(body)
                            onSaved()
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
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save my info", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, fontSize = 12.sp, color = Chalk500, fontWeight = FontWeight.Medium)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Coral,
    unfocusedBorderColor = Chalk200,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

private data class ParsedPlus(
    val bringing: Boolean,
    val name: String,
    val relationship: String,
    val costSplit: String
)

private data class ParsedArrival(
    val flightNumber: String,
    val arrivalTime: String,
    val airport: String,
    val terminal: String,
    val needsPickup: Boolean
)

private fun decodePlusOne(json: String?): ParsedPlus {
    val obj = json?.let { runCatching { JSONObject(it) }.getOrNull() }
    return ParsedPlus(
        bringing = obj?.optBoolean("bringing", false) == true,
        name = obj?.optString("name").orEmpty(),
        relationship = obj?.optString("relationship").orEmpty(),
        costSplit = obj?.optString("costSplit").orEmpty()
    )
}

private fun decodeArrival(json: String?): ParsedArrival {
    val obj = json?.let { runCatching { JSONObject(it) }.getOrNull() }
    return ParsedArrival(
        flightNumber = obj?.optString("flightNumber").orEmpty(),
        arrivalTime = obj?.optString("arrivalTime").orEmpty(),
        airport = obj?.optString("airport").orEmpty(),
        terminal = obj?.optString("terminal").orEmpty(),
        needsPickup = obj?.optBoolean("needsPickup", false) == true
    )
}
