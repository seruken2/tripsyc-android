package com.tripsyc.app.ui.trip.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.tripsyc.app.data.api.models.PastCoTraveler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun InviteScreen(trip: Trip) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var emailInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var coTravelers by remember { mutableStateOf<List<PastCoTraveler>>(emptyList()) }
    // Tracks which past-co-traveler emails the viewer has invited *during
    // this session* so the button can flip from "+ Invite" to a checkmark
    // without a full reload. Cleared on screen recompose.
    val invitedEmails = remember { mutableStateListOf<String>() }

    LaunchedEffect(trip.id) {
        try {
            coTravelers = ApiClient.apiService
                .getPastCoTravelers(excludeTripId = trip.id)
                .coTravelers
        } catch (_: Exception) {
            // Past-co-travelers is a nice-to-have; failure shouldn't
            // break the rest of the invite flow.
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Invite People", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Chalk900)

        // Invite code card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBackground,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Share Invite Code", fontWeight = FontWeight.SemiBold, color = Chalk900, fontSize = 16.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Coral.copy(alpha = 0.1f), Dusk.copy(alpha = 0.05f))),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = trip.inviteCode,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Chalk900,
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(trip.inviteCode))
                            copied = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copied) "Copied!" else "Copy Code")
                    }
                }

                if (copied) {
                    Text("Invite code copied to clipboard!", color = Success, fontSize = 13.sp)
                }
            }
        }

        // Email invite
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBackground,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Invite by Email", fontWeight = FontWeight.SemiBold, color = Chalk900)
                Text("Send a magic link invitation directly to their inbox.", fontSize = 13.sp, color = Chalk500)

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it; errorMessage = null; successMessage = null },
                    label = { Text("Email address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                if (errorMessage != null) Text(errorMessage!!, color = Danger, fontSize = 13.sp)
                if (successMessage != null) Text(successMessage!!, color = Success, fontSize = 13.sp)

                Button(
                    onClick = {
                        val email = emailInput.trim().lowercase()
                        if (email.isEmpty()) { errorMessage = "Please enter an email"; return@Button }
                        scope.launch {
                            isSending = true
                            try {
                                ApiClient.apiService.inviteMember(trip.id, mapOf("email" to email))
                                emailInput = ""
                                successMessage = "Invitation sent to $email!"
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to send invitation"
                            }
                            isSending = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isSending,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) {
                    if (isSending) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Send Invite", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Past co-travelers — quick-invite anyone the viewer has
        // already shared a trip with so the email field doesn't have
        // to be retyped every time.
        if (coTravelers.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Past co-travelers",
                        fontWeight = FontWeight.SemiBold,
                        color = Chalk900
                    )
                    Text(
                        "Quick-invite anyone you've already taken a trip with.",
                        fontSize = 12.sp,
                        color = Chalk500
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(coTravelers, key = { it.userId }) { traveler ->
                            PastCoTravelerChip(
                                traveler = traveler,
                                invited = invitedEmails.contains(traveler.email),
                                isBusy = isSending,
                                onInvite = {
                                    val email = traveler.email
                                    scope.launch {
                                        isSending = true
                                        try {
                                            ApiClient.apiService.inviteMember(
                                                trip.id,
                                                mapOf("email" to email)
                                            )
                                            invitedEmails.add(email)
                                            successMessage = "Invited ${traveler.name}!"
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Couldn't invite"
                                        }
                                        isSending = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Current members
        if (!trip.members.isNullOrEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current Members (${trip.members.size})", fontWeight = FontWeight.SemiBold, color = Chalk900)
                    trip.members.forEach { member ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(member.name, color = Chalk900, fontSize = 14.sp)
                                Text(member.email ?: "", color = Chalk500, fontSize = 12.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (member.role.name) {
                                    "CREATOR" -> Coral.copy(alpha = 0.15f)
                                    "CO_ORGANIZER" -> Dusk.copy(alpha = 0.15f)
                                    else -> Chalk200
                                }
                            ) {
                                Text(
                                    text = member.role.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                    color = when (member.role.name) {
                                        "CREATOR" -> Coral
                                        "CO_ORGANIZER" -> Dusk
                                        else -> Chalk500
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastCoTravelerChip(
    traveler: PastCoTraveler,
    invited: Boolean,
    isBusy: Boolean,
    onInvite: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (invited) Sage.copy(alpha = 0.10f) else Chalk100,
        onClick = { if (!invited && !isBusy) onInvite() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .width(96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Coral.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!traveler.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = traveler.avatarUrl,
                        contentDescription = traveler.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        traveler.name.firstOrNull()?.uppercase() ?: "?",
                        color = Coral,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                traveler.name.split(" ").firstOrNull() ?: traveler.name,
                fontSize = 12.sp,
                color = Chalk900,
                maxLines = 1
            )
            Text(
                "${traveler.sharedTripCount} trip${if (traveler.sharedTripCount == 1) "" else "s"}",
                fontSize = 10.sp,
                color = Chalk500,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = if (invited) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (invited) Sage else Coral,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    if (invited) "Invited" else "Invite",
                    color = if (invited) Sage else Coral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
