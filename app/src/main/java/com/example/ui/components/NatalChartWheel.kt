package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.ui.PlanetCalculator
import com.example.ui.PlanetCalculator.PlanetPosition
import com.example.data.UserProfile
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NatalChartWheel(profile: UserProfile, modifier: Modifier = Modifier, onPlanetClick: ((planet: String, sign: String) -> Unit)? = null) {
    val dateParts = profile.birthDate.split("/")
    val month = dateParts.getOrNull(0)?.toIntOrNull() ?: 1
    val day = dateParts.getOrNull(1)?.toIntOrNull() ?: 1
    val year = dateParts.getOrNull(2)?.toIntOrNull() ?: 2000

    val isUnknown = profile.birthTime.startsWith("Unknown")
    val hour = if (!isUnknown) {
        val timeParts = profile.birthTime.split(":")
        val h = timeParts.getOrNull(0)?.toDoubleOrNull() ?: 12.0
        val m = timeParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        h + (m / 60.0)
    } else {
        12.0
    }
    
    val planets = remember(year, month, day, hour) {
        PlanetCalculator.calculatePlanets(year, month, day, hour)
    }
    
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary

    var selectedElement by remember { mutableStateOf<String?>(null) }
    
    val density = LocalDensity.current

    val animatedPlanets = planets.mapValues { (name, pos) ->
        val animatedLon by animateFloatAsState(
            targetValue = pos.longitude.toFloat(),
            animationSpec = tween(1500, easing = FastOutSlowInEasing),
            label = "planet_anim_$name"
        )
        pos.copy(longitude = animatedLon.toDouble())
    }

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star_rotation"
    )
    
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val wheelBackgroundTint = if (isDark) {
        Color(0xFF1E1612).copy(alpha = 0.5f)
    } else {
        Color(0xFFF3ECE2).copy(alpha = 0.6f)
    }

    val stars = remember { 
        List(150) { 
            androidx.compose.ui.geometry.Offset((Math.random() * 2f - 1f).toFloat(), (Math.random() * 2f - 1f).toFloat()) 
        } 
    }
    val starSizes = remember { List(150) { (Math.random() * 3 + 1).toFloat() } }

    BoxWithConstraints(modifier = modifier.aspectRatio(1f).padding(16.dp)) {
        val componentSizePx = with(density) { maxWidth.toPx() }
        val scaleFactor = (componentSizePx / 800f).coerceAtLeast(0.5f) // Baseline of 800px

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(animatedPlanets, componentSizePx) {
                detectTapGestures { offset ->
                    val radius = size.width.coerceAtMost(size.height) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    
                    var tapped: String? = null
                    val ascendantOffset = animatedPlanets["Ascendant"]?.longitude?.toFloat() ?: 0f
                    
                    // Check planets
                    for ((name, pos) in animatedPlanets) {
                        if (name == "Ascendant") continue
                        val effectiveLongitude = pos.longitude - ascendantOffset
                        val angleRad = Math.toRadians(180 - effectiveLongitude)
                        val r = radius * 0.6f
                        val pX = center.x + r * cos(angleRad).toFloat()
                        val pY = center.y - r * sin(angleRad).toFloat()
                        
                        val dist = Math.hypot((offset.x - pX).toDouble(), (offset.y - pY).toDouble())
                        // Scale hit target area dynamically based on component size
                        if (dist < 60f * scaleFactor) {
                            val signIndex = (pos.longitude / 30.0).toInt().coerceIn(0, 11)
                            val zodiacSigns = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
                            val signName = zodiacSigns[signIndex]
                            tapped = "$name in $signName (${"%.1f".format(pos.longitude % 30)}°)"
                            onPlanetClick?.invoke(name, signName)
                            break
                        }
                    }
                    
                    if (tapped == null) {
                        // Check signs
                        val distFromCenter = Math.hypot((offset.x - center.x).toDouble(), (offset.y - center.y).toDouble())
                        if (distFromCenter > radius * 0.6f && distFromCenter < radius) {
                            var angle = Math.toDegrees(Math.atan2((center.y - offset.y).toDouble(), (offset.x - center.x).toDouble()))
                            if (angle < 0) angle += 360.0
                            var effectiveLongitude = 180.0 - angle
                            if (effectiveLongitude < 0) effectiveLongitude += 360.0
                            
                            var realLongitude = (effectiveLongitude + ascendantOffset) % 360.0
                            if (realLongitude < 0) realLongitude += 360.0
                            
                            val signIndex = (realLongitude / 30.0).toInt().coerceIn(0, 11)
                            val zodiacSigns = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
                            tapped = "${zodiacSigns[signIndex]} (House)"
                        } else {
                            tapped = null
                        }
                    }
                    
                    selectedElement = tapped
                }
            }
        ) {
            val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            
            // Draw background and stars
            drawRect(color = wheelBackgroundTint)
            withTransform({
                rotate(rotation, centerOffset)
            }) {
                stars.forEachIndexed { i, offset ->
                    val rx = centerOffset.x + offset.x * size.width
                    val ry = centerOffset.y + offset.y * size.height
                    drawCircle(if (isDark) androidx.compose.ui.graphics.Color(0xFFF7E7CE).copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color(0xFF8B5E3C).copy(alpha = 0.35f), starSizes[i] * scaleFactor, androidx.compose.ui.geometry.Offset(rx, ry))
                }
            }

            drawNatalChartInternal(animatedPlanets, onSurface, primary, scaleFactor)
        }

        AnimatedVisibility(
            visible = selectedElement != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = selectedElement ?: "",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        val context = LocalContext.current
        val layoutDirection = LocalLayoutDirection.current
        val backgroundColor = MaterialTheme.colorScheme.background
        
        IconButton(
            onClick = {
                saveChartToGallery(context, planets, onSurface, primary, density, layoutDirection, backgroundColor)
            },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "Download Chart",
                tint = primary
            )
        }
    }
}

fun saveChartToGallery(
    context: Context,
    planets: Map<String, PlanetPosition>,
    onSurface: Color,
    primary: Color,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    background: Color
) {
    val sizePx = 2000 // High resolution
    val imageBitmap = ImageBitmap(sizePx, sizePx)
    val canvas = Canvas(imageBitmap)
    val scope = CanvasDrawScope()

    scope.draw(density, layoutDirection, canvas, Size(sizePx.toFloat(), sizePx.toFloat())) {
        drawRect(color = background, size = size)
        drawNatalChartInternal(planets, onSurface, primary, sizePx / 800f)
    }

    val androidBitmap = imageBitmap.asAndroidBitmap()

    val filename = "natal_chart_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(context, "Chart saved to Pictures", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save chart", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
    }
}

fun DrawScope.drawNatalChartInternal(
    planets: Map<String, PlanetPosition>,
    onSurface: Color,
    primary: Color,
    scaleFactor: Float
) {
    val radius = size.minDimension / 2
    val center = Offset(size.width / 2, size.height / 2)
    
    val ascendantOffset = planets["Ascendant"]?.longitude?.toFloat() ?: 0f
    
    // Draw 12 Zodiac segments
    drawCircle(
        color = onSurface,
        radius = radius,
        center = center,
        style = Stroke(width = (2f * scaleFactor).dp.toPx())
    )
    drawCircle(
        color = onSurface,
        radius = radius * 0.7f,
        center = center,
        style = Stroke(width = (1f * scaleFactor).dp.toPx())
    )
    
    val zodiacSigns = listOf(
        "Aries", "Taurus", "Gemini", "Cancer",
        "Leo", "Virgo", "Libra", "Scorpio",
        "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    )

    for (i in 0 until 12) {
        val signLongitude = i * 30f
        val effectiveLongitude = signLongitude - ascendantOffset
        val angleRad = Math.toRadians(180 - effectiveLongitude.toDouble())
        
        val startX = center.x + (radius * 0.7f) * cos(angleRad).toFloat()
        val startY = center.y - (radius * 0.7f) * sin(angleRad).toFloat()
        val endX = center.x + radius * cos(angleRad).toFloat()
        val endY = center.y - radius * sin(angleRad).toFloat()
        
        drawLine(
            color = onSurface,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = (1f * scaleFactor).dp.toPx()
        )
        
        val labelAngleRad = Math.toRadians(180 - (effectiveLongitude + 15).toDouble())
        val labelR = radius * 0.85f
        val labelX = center.x + labelR * cos(labelAngleRad).toFloat()
        val labelY = center.y - labelR * sin(labelAngleRad).toFloat()
        
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.HSVToColor(floatArrayOf(i * 30f, 0.4f, 0.8f))
                textSize = 36f * scaleFactor
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            val sign = zodiacSigns[i].take(3)
            canvas.nativeCanvas.drawText(sign, labelX, labelY + (12f * scaleFactor), paint)
        }
    }

    // Draw Planets
    planets.forEach { (name, pos) ->
        if (name != "Ascendant") { // Skip plotting word "As" over the ascendant line to avoid clutter, optional
            val effectiveLongitude = pos.longitude - ascendantOffset
            val angleRad = Math.toRadians(180 - effectiveLongitude)
            val r = radius * 0.6f
            val pX = center.x + r * cos(angleRad).toFloat()
            val pY = center.y - r * sin(angleRad).toFloat()
            
            drawCircle(
                color = primary,
                radius = (4f * scaleFactor).dp.toPx(),
                center = Offset(pX, pY)
            )
            
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = onSurface.toArgb()
                    textSize = 32f * scaleFactor
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val shortName = name.take(2)
                canvas.nativeCanvas.drawText(shortName, pX, pY - (15f * scaleFactor), paint)
            }
        }
    }
    
    // Draw Aspects
    val planetList = planets.values.toList().filter { it.sign != "Ascendant" } // exclude Asc.
    // wait, sign != "Ascendant"? We don't filter Ascendant from aspects yet.
    // actually, let's keep it simple.
    
    val aspectList = planets.filter { it.key != "Ascendant" }.values.toList()
    for (i in aspectList.indices) {
        for (j in i + 1 until aspectList.size) {
            val p1 = aspectList[i]
            val p2 = aspectList[j]
            
            var diff = Math.abs(p1.longitude - p2.longitude)
            if (diff > 180) diff = 360 - diff
            
            val aspectColor = when {
                Math.abs(diff - 120) < 5 -> Color.Green.copy(alpha = 0.5f)
                Math.abs(diff - 90) < 5 -> Color.Red.copy(alpha = 0.5f)
                Math.abs(diff - 180) < 5 -> Color.Red.copy(alpha = 0.5f)
                Math.abs(diff - 60) < 5 -> Color.Cyan.copy(alpha = 0.5f)
                else -> Color.Transparent
            }
            
            if (aspectColor != Color.Transparent) {
                val eff1 = p1.longitude - ascendantOffset
                val eff2 = p2.longitude - ascendantOffset
                val rad1 = Math.toRadians(180 - eff1)
                val rad2 = Math.toRadians(180 - eff2)
                val r = radius * 0.6f
                val x1 = center.x + r * cos(rad1).toFloat()
                val y1 = center.y - r * sin(rad1).toFloat()
                val x2 = center.x + r * cos(rad2).toFloat()
                val y2 = center.y - r * sin(rad2).toFloat()
                
                drawLine(
                    color = aspectColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = (2f * scaleFactor).dp.toPx()
                )
            }
        }
    }
}

