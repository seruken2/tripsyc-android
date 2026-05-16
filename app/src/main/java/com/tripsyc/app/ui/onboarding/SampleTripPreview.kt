package com.tripsyc.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.ui.theme.*

/**
 * Read-only "what does a Tripsyc trip look like" preview. Shown from
 * the empty trips list so new users can scan the value prop before
 * spinning up their own trip. One of four scenarios is picked at
 * random on each open. Mirrors the iOS SampleTripView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleTripPreview(
    onCreateOwn: () -> Unit,
    onDismiss: () -> Unit
) {
    val scenario = remember { SAMPLE_SCENARIOS.random() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sample trip", fontWeight = FontWeight.Bold, color = Chalk900) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Chalk700)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Chalk50)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp, color = Color.White) {
                Button(
                    onClick = onCreateOwn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) {
                    Text("Create my own trip", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        },
        containerColor = Chalk50
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(Coral, Dusk)))
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(
                            "Sample",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            scenario.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }
            }

            // Members preview
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = CardBackground, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${scenario.memberInitials.size} travelers", fontWeight = FontWeight.SemiBold, color = Chalk900)
                        Row(horizontalArrangement = Arrangement.spacedBy(-8.dp)) {
                            scenario.memberInitials.forEach { initials ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Coral.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(initials, color = Coral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Holidays
            item {
                ScenarioSection(
                    icon = Icons.Default.CalendarMonth,
                    accent = Gold,
                    title = "Holidays during the trip",
                    rows = scenario.holidays
                )
            }

            // Activities
            item {
                ScenarioSection(
                    icon = Icons.Default.ListAlt,
                    accent = Dusk,
                    title = "Itinerary highlights",
                    rows = scenario.activities
                )
            }

            // Splits
            item {
                ScenarioSection(
                    icon = Icons.Default.AccountBalanceWallet,
                    accent = Sage,
                    title = "Expenses split",
                    rows = scenario.splits
                )
            }
        }
    }
}

@Composable
private fun ScenarioSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    title: String,
    rows: List<Pair<String, String>>
) {
    Surface(shape = RoundedCornerShape(14.dp), color = CardBackground, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
                }
                Text(title, fontWeight = FontWeight.SemiBold, color = Chalk900)
            }
            rows.forEach { (primary, secondary) ->
                Column {
                    Text(primary, color = Chalk900, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(secondary, color = Chalk500, fontSize = 12.sp)
                }
            }
        }
    }
}

private data class SampleScenario(
    val title: String,
    val memberInitials: List<String>,
    val holidays: List<Pair<String, String>>,
    val activities: List<Pair<String, String>>,
    val splits: List<Pair<String, String>>
)

private val SAMPLE_SCENARIOS = listOf(
    SampleScenario(
        title = "Bali · December 2026",
        memberInitials = listOf("JK", "MR", "AS", "TC"),
        holidays = listOf(
            "Christmas Day" to "Dec 25",
            "Galungan" to "Dec 18",
            "New Year's Eve" to "Dec 31"
        ),
        activities = listOf(
            "Day 2 · Sunset at Tanah Lot" to "Public temple, sunset 6:14pm",
            "Day 3 · Cooking class in Ubud" to "10am · 4 spots booked",
            "Day 4 · Surf lesson Canggu" to "Beginner-friendly · 8am",
            "Day 6 · Day trip to Nusa Penida" to "Full day · ferry from Sanur"
        ),
        splits = listOf(
            "Villa rental" to "USD 1,200 · split 4 ways",
            "Airport transfer" to "USD 80 · split 4 ways",
            "Group dinner" to "USD 240 · split 4 ways"
        )
    ),
    SampleScenario(
        title = "Lisbon · September 2026",
        memberInitials = listOf("EM", "RS", "DK"),
        holidays = listOf(
            "Day of Portugal" to "Jun 10",
            "Republic Day" to "Oct 5",
            "Saints Festival" to "Jun 13"
        ),
        activities = listOf(
            "Day 1 · Tram 28 ride to Alfama" to "Pickup outside Hotel · 11am",
            "Day 2 · Day trip to Sintra" to "Train from Rossio · 9am",
            "Day 3 · Fado dinner in Bairro Alto" to "Booked at Tasca do Chico · 8pm",
            "Day 5 · Surf morning at Costa da Caparica" to "Lessons + lunch · 9am"
        ),
        splits = listOf(
            "Apartment rental" to "EUR 980 · split 3 ways",
            "Sintra day trip" to "EUR 165 · split 3 ways",
            "Group brunch" to "EUR 84 · split 3 ways"
        )
    ),
    SampleScenario(
        title = "Mexico City · March 2026",
        memberInitials = listOf("LO", "JM", "RS", "AB", "CR"),
        holidays = listOf(
            "Benito Juárez Day" to "Mar 21",
            "Spring Equinox" to "Mar 20",
            "Good Friday" to "Apr 3"
        ),
        activities = listOf(
            "Day 1 · Food walk in Roma Norte" to "Tacos al pastor · 7pm",
            "Day 2 · Teotihuacán pyramids" to "Sunrise tour · 5am",
            "Day 3 · Lucha libre at Arena México" to "Tickets booked · 7:30pm",
            "Day 5 · Xochimilco trajinera" to "Boat + mariachi · all afternoon"
        ),
        splits = listOf(
            "Group apartment" to "USD 1,050 · split 5 ways",
            "Teotihuacán tour" to "USD 240 · split 5 ways",
            "Lucha libre tickets" to "USD 175 · split 5 ways"
        )
    ),
    SampleScenario(
        title = "Tokyo · April 2026",
        memberInitials = listOf("YN", "KH", "AB", "MS"),
        holidays = listOf(
            "Showa Day" to "Apr 29",
            "Constitution Day" to "May 3",
            "Greenery Day" to "May 4"
        ),
        activities = listOf(
            "Day 1 · Tsukiji breakfast" to "Outer market sushi · 7am",
            "Day 2 · Shibuya + Harajuku" to "Walking tour · 11am",
            "Day 4 · Hakone day trip" to "Romance Car · Onsen at night",
            "Day 6 · Cherry blossoms at Ueno" to "Sunset picnic"
        ),
        splits = listOf(
            "Shibuya Airbnb" to "USD 1,360 · split 4 ways",
            "JR Rail Pass × 4" to "USD 1,120 · split 4 ways",
            "Group izakaya dinner" to "USD 200 · split 4 ways"
        )
    )
)
