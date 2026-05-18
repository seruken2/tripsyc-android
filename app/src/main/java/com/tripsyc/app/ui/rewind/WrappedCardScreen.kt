package com.tripsyc.app.ui.rewind

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.WrappedResponse
import com.tripsyc.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Tripsyc Wrapped: shareable year-end card. Pulls /api/rewind/wrapped,
 * renders a portrait gradient card with personal stats and the
 * featured trip, and exposes a Share action that captures the card
 * to a PNG and fires Android's ACTION_SEND chooser (Instagram,
 * Messages, anywhere).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrappedCardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var data by remember { mutableStateOf<WrappedResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSharing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            data = ApiClient.apiService.getWrapped()
        } catch (e: Exception) {
            error = e.message ?: "Couldn't load your Wrapped"
        }
    }

    // GraphicsLayer captures the card composable into a snapshottable
    // texture. drawLayer below tees the render into both the screen
    // and the layer, so the user sees what they're about to share.
    val graphicsLayer = rememberGraphicsLayer()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tripsyc Wrapped", fontWeight = FontWeight.Bold, color = Chalk900) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Chalk700)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Guard: data must be loaded, share must not
                            // already be in flight, and the layer must
                            // have been recorded at least once. The last
                            // condition prevents IllegalStateException
                            // on first-frame Share-button mash before
                            // the card has drawn.
                            if (data == null || isSharing || graphicsLayer.size.width == 0) return@IconButton
                            scope.launch {
                                isSharing = true
                                try {
                                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                    val uri = withContext(Dispatchers.IO) {
                                        saveWrappedBitmap(context, bitmap)
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, "Share Wrapped")
                                    )
                                } catch (_: Exception) {
                                    error = "Couldn't share — try again."
                                }
                                isSharing = false
                            }
                        }
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = Coral)
                        } else {
                            Icon(Icons.Default.IosShare, contentDescription = "Share", tint = Coral)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Chalk50)
            )
        },
        containerColor = Chalk50
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            val current = data
            when {
                current == null && error == null -> CircularProgressIndicator(color = Coral)
                error != null -> Text(error!!, color = Danger)
                current != null -> WrappedCard(
                    data = current,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .drawWithContent {
                            graphicsLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(graphicsLayer)
                        }
                )
            }
        }
    }
}

@Composable
private fun WrappedCard(data: WrappedResponse, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Coral, Dusk, Color(0xFF2C2459))))
            .padding(28.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "TRIPSYC WRAPPED",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${data.year}",
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "for ${data.displayName}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))

            val p = data.personal
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WrappedStatLine("Trips", p.tripCount.toString())
                WrappedStatLine("Nights", p.nights.toString())
                WrappedStatLine("Cities", p.cities.toString())
                WrappedStatLine("Countries", p.countries.toString())
                WrappedStatLine("Photos", p.photos.toString())
                WrappedStatLine("Messages", p.chatMessages.toString())
            }

            data.featured?.let { f ->
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "FEATURED TRIP",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                        Text(
                            f.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            listOfNotNull(
                                f.destination,
                                f.dateLabel,
                                "${f.memberCount} travelers",
                                "${f.fitTier} fit (${f.fitScore})"
                            ).joinToString(" · "),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "tripsyc.app",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.3.sp
            )
        }
    }
}

@Composable
private fun WrappedStatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(
            value,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
        )
    }
}

private fun saveWrappedBitmap(context: android.content.Context, bitmap: Bitmap): android.net.Uri {
    val dir = File(context.cacheDir, "wrapped").apply { mkdirs() }
    val file = File(dir, "wrapped-${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
