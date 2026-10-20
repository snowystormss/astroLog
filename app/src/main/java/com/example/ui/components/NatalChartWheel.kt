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
import com.example.data.HouseCusp
import com.example.data.AnglesResult
import com.example.data.DignityType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NatalChartWheel(
    profile: UserProfile,
    modifier: Modifier = Modifier,
    houseSystem: String = "WHOLE_SIGN",
    onPlanetClick: ((planet: String, sign: String) -> Unit)? = null
) {
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
    
    val planets = remember(year, month, day, hour, profile.latitude, profile.longitude) {
        PlanetCalculator.calculatePlanets(year, month, day, hour, profile.latitude, profile.longitude)
    }

    val angles = remember(year, month, day, hour, profile.latitude, profile.longitude) {
        PlanetCalculator.calculateAngles(year, month, day, hour, profile.latitude, profile.longitude)
    }

    val cusps = remember(houseSystem, angles, profile.latitude, profile.longitude, year, month, day, hour) {
        PlanetCalculator.calculateHouseCusps(
            system = houseSystem,
            ascLon = angles.ascendant.longitude,
            mcLon = angles.midheaven.longitude,
            latitude = profile.latitude,
            longitude = profile.longitude,
            year = year,
            month = month,
            day = day,
            hour = hour
        )
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
            .pointerInput(animatedPlanets, componentSizePx, cusps) {
                detectTapGestures { offset ->
                    val radius = size.width.coerceAtMost(size.height) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    
                    var tapped: String? = null
                    val ascendantOffset = animatedPlanets["Ascendant"]?.longitude?.toFloat() ?: 0f
                    
                    // Check planets
                    val tapPlanets = animatedPlanets.filter { 
                        it.key !in listOf("Ascendant", "Descendant", "Imum Coeli", "Midheaven") 
                    }
                    for ((name, pos) in tapPlanets) {
                        val effectiveLongitude = pos.longitude - ascendantOffset
                        val angleRad = Math.toRadians(180 - effectiveLongitude)
                        val r = radius * 0.62f
                        val pX = center.x + r * cos(angleRad).toFloat()
                        val pY = center.y - r * sin(angleRad).toFloat()
                        
                        val dist = Math.hypot((offset.x - pX).toDouble(), (offset.y - pY).toDouble())
                        // Scale hit target area dynamically based on component size
                        if (dist < 60f * scaleFactor) {
                            val signIndex = (pos.longitude / 30.0).toInt().coerceIn(0, 11)
                            val signName = PlanetCalculator.ZODIAC_SIGNS[signIndex]
                            val houseNum = PlanetCalculator.getPlanetHouse(pos.longitude, cusps, houseSystem)
                            val dignity = PlanetCalculator.calculateDignity(name, signName)
                            val dignityBadge = when (dignity) {
                                DignityType.DOMICILE -> "⭐ Domicile"
                                DignityType.EXALTATION -> "🔺 Exalted"
                                DignityType.DETRIMENT -> "⚠️ Detriment"
                                DignityType.FALL -> "🔻 Fall"
                                DignityType.PEREGRINE -> "Peregrine"
                            }
                            tapped = "$name in $signName (${pos.degree}°${pos.minute}')\nHouse $houseNum • $dignityBadge"
                            onPlanetClick?.invoke(name, signName)
                            break
                        }
                    }
                    
                    if (tapped == null) {
                        // Check signs
                        val distFromCenter = Math.hypot((offset.x - center.x).toDouble(), (offset.y - center.y).toDouble())
                        if (distFromCenter > radius * 0.72f && distFromCenter < radius) {
                            var angle = Math.toDegrees(Math.atan2((center.y - offset.y).toDouble(), (offset.x - center.x).toDouble()))
                            if (angle < 0) angle += 360.0
                            var effectiveLongitude = 180.0 - angle
                            if (effectiveLongitude < 0) effectiveLongitude += 360.0
                            
                            var realLongitude = (effectiveLongitude + ascendantOffset) % 360.0
                            if (realLongitude < 0) realLongitude += 360.0
                            
                            val signIndex = (realLongitude / 30.0).toInt().coerceIn(0, 11)
                            val zodiacSigns = PlanetCalculator.ZODIAC_SIGNS
                            tapped = "${zodiacSigns[signIndex]} (Zodiac Sign)"
                        } else if (distFromCenter > radius * 0.45f && distFromCenter <= radius * 0.72f) {
                            // Check which house was tapped
                            var angle = Math.toDegrees(Math.atan2((center.y - offset.y).toDouble(), (offset.x - center.x).toDouble()))
                            if (angle < 0) angle += 360.0
                            var effectiveLongitude = 180.0 - angle
                            if (effectiveLongitude < 0) effectiveLongitude += 360.0
                            val realLon = (effectiveLongitude + ascendantOffset) % 360.0
                            val houseNum = PlanetCalculator.getPlanetHouse(realLon, cusps, houseSystem)
                            val houseSign = cusps.getOrNull(houseNum - 1)?.sign ?: ""
                            tapped = "House $houseNum (Cusp in $houseSign)"
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

            drawNatalChartInternal(animatedPlanets, cusps, angles, onSurface, primary, scaleFactor)
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
                saveChartToGallery(context, planets, cusps, angles, onSurface, primary, density, layoutDirection, backgroundColor)
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
    cusps: List<HouseCusp>,
    angles: AnglesResult?,
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
        drawNatalChartInternal(planets, cusps, angles, onSurface, primary, sizePx / 800f)
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
    cusps: List<HouseCusp>,
    angles: AnglesResult?,
    onSurface: Color,
    primary: Color,
    scaleFactor: Float
) {
    val radius = size.minDimension / 2
    val center = Offset(size.width / 2, size.height / 2)
    
    val ascendantOffset = planets["Ascendant"]?.longitude?.toFloat() ?: 0f
    
    // Outer boundary circle
    drawCircle(
        color = onSurface.copy(alpha = 0.8f),
        radius = radius,
        center = center,
        style = Stroke(width = (2.5f * scaleFactor).dp.toPx())
    )

    // Inner rim of Zodiac ring (radius * 0.74f)
    drawCircle(
        color = onSurface.copy(alpha = 0.6f),
        radius = radius * 0.74f,
        center = center,
        style = Stroke(width = (1.5f * scaleFactor).dp.toPx())
    )

    // Inner rim of Houses ring (radius * 0.48f)
    drawCircle(
        color = onSurface.copy(alpha = 0.35f),
        radius = radius * 0.48f,
        center = center,
        style = Stroke(width = (1f * scaleFactor).dp.toPx())
    )
    
    val zodiacSigns = PlanetCalculator.ZODIAC_SIGNS

    // 1. Draw 12 Zodiac 30° divisions on outer ring
    for (i in 0 until 12) {
        val signLongitude = i * 30f
        val effectiveLongitude = signLongitude - ascendantOffset
        val angleRad = Math.toRadians(180 - effectiveLongitude.toDouble())
        
        val startX = center.x + (radius * 0.74f) * cos(angleRad).toFloat()
        val startY = center.y - (radius * 0.74f) * sin(angleRad).toFloat()
        val endX = center.x + radius * cos(angleRad).toFloat()
        val endY = center.y - radius * sin(angleRad).toFloat()
        
        drawLine(
            color = onSurface.copy(alpha = 0.4f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = (1f * scaleFactor).dp.toPx()
        )
        
        val labelAngleRad = Math.toRadians(180 - (effectiveLongitude + 15).toDouble())
        val labelR = radius * 0.86f
        val labelX = center.x + labelR * cos(labelAngleRad).toFloat()
        val labelY = center.y - labelR * sin(labelAngleRad).toFloat()
        
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.HSVToColor(floatArrayOf(i * 30f, 0.45f, 0.85f))
                textSize = 34f * scaleFactor
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val sign = zodiacSigns[i].take(3)
            canvas.nativeCanvas.drawText(sign, labelX, labelY + (12f * scaleFactor), paint)
        }
    }

    // 2. Draw 12 House cusps and House numbers
    if (cusps.size == 12) {
        for (i in 0 until 12) {
            val cusp = cusps[i]
            val effectiveCuspLon = cusp.longitude.toFloat() - ascendantOffset
            val cuspAngleRad = Math.toRadians(180 - effectiveCuspLon.toDouble())

            val innerX = center.x + (radius * 0.48f) * cos(cuspAngleRad).toFloat()
            val innerY = center.y - (radius * 0.48f) * sin(cuspAngleRad).toFloat()
            val outerX = center.x + (radius * 0.74f) * cos(cuspAngleRad).toFloat()
            val outerY = center.y - (radius * 0.74f) * sin(cuspAngleRad).toFloat()

            drawLine(
                color = onSurface.copy(alpha = 0.25f),
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = (1f * scaleFactor).dp.toPx()
            )

            // Number placement in midpoint of house
            val nextCusp = cusps[(i + 1) % 12]
            var dSpan = nextCusp.longitude - cusp.longitude
            if (dSpan < 0) dSpan += 360.0
            val midLon = (cusp.longitude + dSpan / 2.0).toFloat() - ascendantOffset
            val midRad = Math.toRadians(180 - midLon.toDouble())
            val midR = radius * 0.53f
            val numX = center.x + midR * cos(midRad).toFloat()
            val numY = center.y - midR * sin(midRad).toFloat()

            drawIntoCanvas { canvas ->
                val numPaint = android.graphics.Paint().apply {
                    color = onSurface.toArgb()
                    alpha = 140
                    textSize = 22f * scaleFactor
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText("${i + 1}", numX, numY + (7f * scaleFactor), numPaint)
            }
        }
    }

    // 3. Major Cross Axes: Horizon (ASC-DSC) & Meridian (MC-IC)
    // Horizon Axis: ASC is at 180° (left), DSC is at 0° (right)
    val horizonLeftX = center.x - radius * 1.02f
    val horizonRightX = center.x + radius * 1.02f
    drawLine(
        color = primary,
        start = Offset(horizonLeftX, center.y),
        end = Offset(horizonRightX, center.y),
        strokeWidth = (2.2f * scaleFactor).dp.toPx()
    )

    drawIntoCanvas { canvas ->
        val axisPaint = android.graphics.Paint().apply {
            color = primary.toArgb()
            textSize = 26f * scaleFactor
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.nativeCanvas.drawText("ASC", center.x - radius * 0.65f, center.y - (8f * scaleFactor), axisPaint)
        canvas.nativeCanvas.drawText("DSC", center.x + radius * 0.65f, center.y - (8f * scaleFactor), axisPaint)
    }

    // Meridian Axis: MC (Midheaven) & IC (Imum Coeli)
    val mcLon = angles?.midheaven?.longitude?.toFloat() ?: (planets["Midheaven"]?.longitude?.toFloat() ?: (ascendantOffset + 270f))
    val effectiveMc = mcLon - ascendantOffset
    val mcRad = Math.toRadians(180 - effectiveMc.toDouble())
    val icRad = Math.toRadians(360 - effectiveMc.toDouble())

    val mcX = center.x + radius * cos(mcRad).toFloat()
    val mcY = center.y - radius * sin(mcRad).toFloat()
    val icX = center.x + radius * cos(icRad).toFloat()
    val icY = center.y - radius * sin(icRad).toFloat()

    drawLine(
        color = primary.copy(alpha = 0.75f),
        start = Offset(mcX, mcY),
        end = Offset(icX, icY),
        strokeWidth = (1.8f * scaleFactor).dp.toPx()
    )

    drawIntoCanvas { canvas ->
        val mcPaint = android.graphics.Paint().apply {
            color = primary.toArgb()
            textSize = 24f * scaleFactor
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val labelMcR = radius * 0.66f
        val labelMcX = center.x + labelMcR * cos(mcRad).toFloat()
        val labelMcY = center.y - labelMcR * sin(mcRad).toFloat()
        canvas.nativeCanvas.drawText("MC", labelMcX, labelMcY + (8f * scaleFactor), mcPaint)

        val labelIcR = radius * 0.66f
        val labelIcX = center.x + labelIcR * cos(icRad).toFloat()
        val labelIcY = center.y - labelIcR * sin(icRad).toFloat()
        canvas.nativeCanvas.drawText("IC", labelIcX, labelIcY + (8f * scaleFactor), mcPaint)
    }

    // 4. Draw Aspect Geometry within central disk (radius * 0.48f)
    val aspectBodies = planets.filter { 
        it.key !in listOf("Ascendant", "Descendant", "Imum Coeli", "Midheaven", "South Node", "Part of Fortune") 
    }.toList()

    for (i in aspectBodies.indices) {
        for (j in i + 1 until aspectBodies.size) {
            val (name1, p1) = aspectBodies[i]
            val (name2, p2) = aspectBodies[j]

            var diff = Math.abs(p1.longitude - p2.longitude)
            if (diff > 180.0) diff = 360.0 - diff

            val hasLuminary = (name1 == "Sun" || name1 == "Moon" || name2 == "Sun" || name2 == "Moon")
            val orbMax = if (hasLuminary) 8.0 else 6.0

            val aspectColor = when {
                Math.abs(diff - 120.0) <= orbMax -> Color(0xFF4CAF50).copy(alpha = 0.65f) // Trine: Green harmony
                Math.abs(diff - 60.0) <= 5.0 -> Color(0xFF00BCD4).copy(alpha = 0.65f)      // Sextile: Cyan opportunity
                Math.abs(diff - 90.0) <= orbMax -> Color(0xFFE91E63).copy(alpha = 0.65f)   // Square: Pink/Red challenge
                Math.abs(diff - 180.0) <= orbMax -> Color(0xFFF44336).copy(alpha = 0.70f)  // Opposition: Crimson polarity
                else -> Color.Transparent
            }

            if (aspectColor != Color.Transparent) {
                val eff1 = p1.longitude - ascendantOffset
                val eff2 = p2.longitude - ascendantOffset
                val rad1 = Math.toRadians(180 - eff1)
                val rad2 = Math.toRadians(180 - eff2)
                val r = radius * 0.45f
                val x1 = center.x + r * cos(rad1).toFloat()
                val y1 = center.y - r * sin(rad1).toFloat()
                val x2 = center.x + r * cos(rad2).toFloat()
                val y2 = center.y - r * sin(rad2).toFloat()

                drawLine(
                    color = aspectColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = (1.5f * scaleFactor).dp.toPx()
                )
            }
        }
    }

    // 5. Draw Celestial Bodies in Middle Ring (radius * 0.62f)
    val glyphMap = mapOf(
        "Sun" to "☉", "Moon" to "☽", "Mercury" to "☿", "Venus" to "♀",
        "Mars" to "♂", "Jupiter" to "♃", "Saturn" to "♄", "Uranus" to "♅",
        "Neptune" to "♆", "Pluto" to "♇", "North Node" to "☊", "South Node" to "☋",
        "Chiron" to "⚷", "Lilith" to "⚸", "Part of Fortune" to "⊕"
    )

    planets.forEach { (name, pos) ->
        if (name !in listOf("Ascendant", "Descendant", "Imum Coeli", "Midheaven")) {
            val effectiveLongitude = pos.longitude - ascendantOffset
            val angleRad = Math.toRadians(180 - effectiveLongitude)
            val r = radius * 0.62f
            val pX = center.x + r * cos(angleRad).toFloat()
            val pY = center.y - r * sin(angleRad).toFloat()

            drawCircle(
                color = primary,
                radius = (4.5f * scaleFactor).dp.toPx(),
                center = Offset(pX, pY)
            )

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = onSurface.toArgb()
                    textSize = 28f * scaleFactor
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                val label = glyphMap[name] ?: name.take(2)
                canvas.nativeCanvas.drawText(label, pX, pY - (12f * scaleFactor), paint)
            }
        }
    }
}
