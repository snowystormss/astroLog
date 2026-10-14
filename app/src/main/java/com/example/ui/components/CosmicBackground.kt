package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier,
    customBackgroundColor: Color? = null,
    parallaxOffset: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current

    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_sky_animation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(180000, easing = LinearEasing), // 3 minutes for a complete majestic rotation
            repeatMode = RepeatMode.Restart
        ),
        label = "stars_rotation"
    )

    val twinkleState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831853f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing), // 8 seconds breathing twinkle loop
            repeatMode = RepeatMode.Restart
        ),
        label = "stars_twinkle"
    )
    
    val targetNebulaColor = if (isDark) {
        Color(0xFF1E1B4B).copy(alpha = 0.45f) // Deep indigo
    } else {
        Color(0xFFEEF2FF).copy(alpha = 0.65f) // Soft slate blue
    }
    val nebulaColor by androidx.compose.animation.animateColorAsState(targetValue = targetNebulaColor, animationSpec = tween(500), label = "nebula")
    
    val targetAccentColor = if (isDark) {
        Color(0xFF3B0764).copy(alpha = 0.35f) // Deep purple
    } else {
        Color(0xFFFDF4FF).copy(alpha = 0.70f) // Soft lavender blush
    }
    val accentColor by androidx.compose.animation.animateColorAsState(targetValue = targetAccentColor, animationSpec = tween(500), label = "accent")

    val targetBackgroundColor = customBackgroundColor ?: if (isDark) {
        Color(0xFF090A17) // Space night
    } else {
        Color(0xFFF8FAFC) // Slate light 50
    }
    val backgroundColor by androidx.compose.animation.animateColorAsState(targetValue = targetBackgroundColor, animationSpec = tween(500), label = "background")

    val targetStarColor = if (isDark) {
        Color(0xFFE2E8F0) // Glowing white/gray
    } else {
        Color(0xFF6366F1).copy(alpha = 0.7f) // Indigo star glow
    }
    val starColor by androidx.compose.animation.animateColorAsState(targetValue = targetStarColor, animationSpec = tween(500), label = "star")

    val targetLineColor = if (isDark) {
        Color.White.copy(alpha = 0.04f)
    } else {
        Color(0xFF4F46E5).copy(alpha = 0.03f)
    }
    val lineColor by androidx.compose.animation.animateColorAsState(targetValue = targetLineColor, animationSpec = tween(500), label = "line")

    // Coordinates of stars (fractional x & y, relative size)
    val starPoints = remember {
        listOf(
            Triple(0.08f, 0.12f, 1.8f),
            Triple(0.32f, 0.06f, 1.2f),
            Triple(0.74f, 0.15f, 2.5f),
            Triple(0.92f, 0.32f, 1.5f),
            Triple(0.20f, 0.42f, 1.8f),
            Triple(0.52f, 0.28f, 3.2f),
            Triple(0.65f, 0.54f, 1.2f),
            Triple(0.12f, 0.68f, 2.2f),
            Triple(0.38f, 0.78f, 1.6f),
            Triple(0.82f, 0.74f, 2.5f),
            Triple(0.04f, 0.86f, 1.2f),
            Triple(0.48f, 0.92f, 2.0f),
            Triple(0.68f, 0.04f, 1.6f),
            Triple(0.25f, 0.58f, 1.4f),
            Triple(0.90f, 0.56f, 1.8f),
            Triple(0.58f, 0.84f, 1.6f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .drawBehind {
                val w = size.width
                val h = size.height

                translate(top = parallaxOffset * h * 0.4f) {
                    // Gentle drifting in the cosmic nebulas over time
                    val driftX = kotlin.math.cos(twinkleState.toDouble()).toFloat() * 0.015f
                    val driftY = kotlin.math.sin(twinkleState.toDouble()).toFloat() * 0.015f

                // Draw background/nebula highlights
                val firstGrad = Brush.radialGradient(
                    colors = listOf(nebulaColor, Color.Transparent),
                    center = Offset(w * (0.25f + driftX), h * (0.22f + driftY)),
                    radius = w * 0.85f
                )
                val secondGrad = Brush.radialGradient(
                    colors = listOf(accentColor, Color.Transparent),
                    center = Offset(w * (0.75f - driftX), h * (0.78f - driftY)),
                    radius = w * 0.8f
                )
                
                drawRect(brush = firstGrad)
                drawRect(brush = secondGrad)

                // Astrological orbit patterns & axis
                val centerOffset = Offset(w * 0.5f, h * 0.5f)
                drawCircle(
                    color = lineColor,
                    radius = w * 0.28f,
                    center = centerOffset,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = lineColor,
                    radius = w * 0.52f,
                    center = centerOffset,
                    style = Stroke(width = 1.dp.toPx())
                )
                
                drawLine(
                    color = lineColor,
                    start = Offset(0f, h * 0.5f),
                    end = Offset(w, h * 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = lineColor,
                    start = Offset(w * 0.5f, 0f),
                    end = Offset(w * 0.5f, h),
                    strokeWidth = 1.dp.toPx()
                )

                // Render Stars with slow rotation and independent pulsing twinkle
                rotate(degrees = rotationAngle, pivot = centerOffset) {
                    starPoints.forEach { (xf, yf, sizeDp) ->
                        val sizePx = sizeDp.dp.toPx()
                        
                        // Calculate a unique phase for each star based on its fractional coordinate
                        val phase = (xf * 11.2f + yf * 23.7f)
                        val sineVal = kotlin.math.sin((twinkleState + phase).toDouble()).toFloat()
                        val twinkleFactor = 0.4f + 0.6f * (sineVal + 1f) / 2f
                        
                        val baseOpacity = 0.25f + (xf * 0.4f) + (yf * 0.15f)
                        val finalOpacity = (baseOpacity * twinkleFactor).coerceIn(0.1f, 0.95f)
                        
                        drawCircle(
                            color = starColor.copy(alpha = finalOpacity),
                            radius = sizePx * (0.8f + twinkleFactor * 0.4f),
                            center = Offset(w * xf, h * yf)
                        )
                    }
                }
                }
            }
    ) {
        content()
    }
}
