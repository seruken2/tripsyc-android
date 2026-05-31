package com.tripwave.app.ui.trip.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripwave.app.data.api.ApiClient
import com.tripwave.app.data.api.models.LockType
import com.tripwave.app.ui.theme.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Spending-pace recap card. Mirrors the iOS BudgetView.spendingPaceSection:
 * splits the trip into PRE / DURING / POST and reports total spent,
 * remaining headroom, daily average, projected total, utilization
 * percent, and an on-track / under / over verdict.
 *
 * Computes everything client-side from the user's budgetMax, the trip's
 * locked date range, and the expenses list. Self-suppresses when the
 * trip doesn't have a locked date range yet, since the math leans on it.
 */
@Composable
fun SpendingPaceCard(tripId: String, budgetMax: Int, currency: String) {
    var pace by remember(tripId, budgetMax) { mutableStateOf<SpendingPace?>(null) }

    LaunchedEffect(tripId, budgetMax) {
        pace = runCatching { computeSpendingPace(tripId, budgetMax) }.getOrNull()
    }

    val p = pace ?: return

    val statusColor = when (p.status) {
        Status.OVER -> Coral
        Status.UNDER, Status.ON_TRACK -> Sage
    }
    val statusBg = statusColor.copy(alpha = 0.12f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Spending pace", fontWeight = FontWeight.SemiBold, color = Chalk900)
                    Text(
                        phaseLabel(p),
                        fontSize = 12.sp,
                        color = Chalk500
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                    Text(
                        p.status.label,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Utilization bar — caps visual at 100% but the label
            // shows the real number (e.g. 150%) so a blown budget
            // doesn't get hidden behind a pinned-full bar.
            val filled = (p.utilizationPct.coerceAtMost(100)).toFloat() / 100f
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Chalk100)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(filled)
                            .background(statusColor, RoundedCornerShape(4.dp))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${p.utilizationPct}% of budget",
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Text(
                        "Day ${p.daysElapsed.coerceAtLeast(0)} of ${p.totalTripDays}",
                        color = Chalk500,
                        fontSize = 12.sp
                    )
                }
            }

            // Stat grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(
                        label = "Spent so far",
                        value = "$currency ${p.totalSpent.roundToInt()}",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Remaining",
                        value = "$currency ${p.remaining.roundToInt()}",
                        accent = if (p.remaining < 0) Coral else Sage,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (p.phase == Phase.DURING || p.phase == Phase.POST) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox(
                            label = "Daily avg",
                            value = "$currency ${p.dailyAvg.roundToInt()}",
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            label = if (p.phase == Phase.POST) "Final total" else "Projected total",
                            value = "$currency ${p.projectedTotal.roundToInt()}",
                            accent = if (p.projectedTotal > budgetMax * 1.05) Coral else Chalk900,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    accent: Color = Chalk900,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Chalk100)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(label, color = Chalk500, fontSize = 11.sp)
            Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun phaseLabel(p: SpendingPace): String = when (p.phase) {
    Phase.PRE -> "Before the trip — ${p.totalTripDays} days planned"
    Phase.DURING -> "Day ${p.daysElapsed} of ${p.totalTripDays}"
    Phase.POST -> "Trip wrapped — final tally"
}

private enum class Phase { PRE, DURING, POST }
private enum class Status(val label: String) {
    ON_TRACK("On track"), UNDER("Under budget"), OVER("Over budget")
}

private data class SpendingPace(
    val totalSpent: Double,
    val dailyAvg: Double,
    val projectedTotal: Double,
    val remaining: Double,
    val utilizationPct: Int,
    val totalTripDays: Int,
    val daysElapsed: Int,
    val phase: Phase,
    val status: Status
)

/**
 * Phase logic mirrors iOS:
 * - PRE: trip hasn't started; daily avg/projection are noise. Compare
 *   what's pre-booked vs the cap to flag big up-front spending.
 * - DURING: real days elapsed; projection only stable from day 2 onwards
 *   (day-1 with one big booking would project a wildly inflated total).
 * - POST: trip done; dailyAvg uses the full trip length, projected = actual.
 */
private suspend fun computeSpendingPace(tripId: String, budgetMax: Int): SpendingPace? {
    if (budgetMax <= 0) return null
    val trip = ApiClient.apiService.getTrip(tripId)
    val lockedValue = trip.locks
        ?.firstOrNull { it.lockType == LockType.DATE && it.locked }
        ?.lockedValue ?: return null

    val parts = lockedValue.split(" to ")
    if (parts.size != 2) return null
    val start = runCatching { LocalDate.parse(parts[0].trim()) }.getOrNull() ?: return null
    val end = runCatching { LocalDate.parse(parts[1].trim()) }.getOrNull() ?: return null

    val expenses = ApiClient.apiService.getExpenses(tripId).expenses
    if (expenses.isEmpty()) return null

    val totalSpent = expenses.sumOf { it.amount }
    val totalTripDays = (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)
    val today = LocalDate.now()

    val phase: Phase
    val daysElapsed: Int
    when {
        today.isBefore(start) -> { phase = Phase.PRE; daysElapsed = 0 }
        today.isAfter(end) -> { phase = Phase.POST; daysElapsed = totalTripDays }
        else -> {
            phase = Phase.DURING
            daysElapsed = (ChronoUnit.DAYS.between(start, today).toInt() + 1).coerceAtLeast(1)
        }
    }

    val budget = budgetMax.toDouble()
    val (dailyAvg, projectedTotal) = when (phase) {
        Phase.PRE -> 0.0 to totalSpent
        Phase.DURING -> if (daysElapsed >= 2) {
            val avg = totalSpent / daysElapsed
            avg to avg * totalTripDays
        } else totalSpent to totalSpent
        Phase.POST -> {
            val avg = totalSpent / totalTripDays
            avg to totalSpent
        }
    }
    val remaining = budget - totalSpent
    val utilizationPct = ((totalSpent / budget) * 100).roundToInt().coerceAtLeast(0)

    val status: Status = when (phase) {
        Phase.PRE -> {
            val ratio = totalSpent / budget
            when {
                ratio > 0.8 -> Status.OVER
                ratio < 0.3 -> Status.UNDER
                else -> Status.ON_TRACK
            }
        }
        Phase.DURING -> if (daysElapsed < 2) Status.ON_TRACK
        else when {
            projectedTotal > budget * 1.05 -> Status.OVER
            projectedTotal < budget * 0.85 -> Status.UNDER
            else -> Status.ON_TRACK
        }
        Phase.POST -> when {
            projectedTotal > budget * 1.05 -> Status.OVER
            projectedTotal < budget * 0.85 -> Status.UNDER
            else -> Status.ON_TRACK
        }
    }

    return SpendingPace(
        totalSpent = totalSpent,
        dailyAvg = dailyAvg,
        projectedTotal = projectedTotal,
        remaining = remaining,
        utilizationPct = utilizationPct,
        totalTripDays = totalTripDays,
        daysElapsed = daysElapsed,
        phase = phase,
        status = status
    )
}
