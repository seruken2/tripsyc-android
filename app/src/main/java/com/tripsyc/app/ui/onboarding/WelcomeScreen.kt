package com.tripsyc.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.R
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * First-run welcome carousel. Four pages explaining the why of the
 * app — collaborate, vote, all-in-one, real-time. Mirrors the iOS
 * WelcomeView. Tap "Get Started" on any page (or after page 4) to
 * proceed to Login. The 'seen' flag is tracked by the caller through
 * an Android SharedPreferences key so it only shows once per device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val pages = listOf(
        WelcomePage(
            icon = Icons.Default.Group,
            title = "Plan Together",
            body = "Coordinate with your group in one shared space. No more scattered texts and lost details.",
            color = Coral
        ),
        WelcomePage(
            icon = Icons.Default.ThumbUp,
            title = "Vote & Decide",
            body = "Suggest destinations, vote on dates, and make decisions democratically. Everyone gets a say.",
            color = Dusk
        ),
        WelcomePage(
            icon = Icons.Default.Inventory2,
            title = "One Place",
            body = "Itineraries, budgets, packing lists, and expenses — everything your trip needs, all in one app.",
            color = Sage
        ),
        WelcomePage(
            icon = Icons.Default.Sensors,
            title = "Live, Everywhere",
            body = "Votes, chat, and expenses sync in real time across web and mobile. Plan together, even when apart.",
            color = Gold
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Chalk50)
            .padding(top = 52.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo header
        Image(
            painter = painterResource(R.drawable.tripsyc_icon),
            contentDescription = "Tripsyc",
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = painterResource(R.drawable.tripsyc_logo),
            contentDescription = "Tripsyc",
            modifier = Modifier
                .height(28.dp)
                .widthIn(max = 160.dp),
            colorFilter = ColorFilter.tint(Coral),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(8.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { index ->
            WelcomePageView(page = pages[index])
        }

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            pages.indices.forEach { i ->
                val selected = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (selected) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (selected) Coral else Chalk200)
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage < pages.lastIndex) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    onGetStarted()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral)
        ) {
            Text(
                if (pagerState.currentPage == pages.lastIndex) "Get started" else "Next",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
        TextButton(
            onClick = onGetStarted,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text("Skip", color = Chalk500, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun WelcomePageView(page: WelcomePage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(page.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.color,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            page.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Chalk900,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            page.body,
            fontSize = 15.sp,
            color = Chalk500,
            textAlign = TextAlign.Center
        )
    }
}

private data class WelcomePage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val body: String,
    val color: Color
)
