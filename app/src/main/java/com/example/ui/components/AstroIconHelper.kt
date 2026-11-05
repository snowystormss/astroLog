package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DignityType

object AstroIconHelper {

    fun getPlanetIcon(planetName: String): ImageVector {
        return when (planetName.lowercase()) {
            "sun" -> Icons.Default.WbSunny
            "moon" -> Icons.Default.NightlightRound
            "mercury" -> Icons.Default.Speed
            "venus" -> Icons.Default.Favorite
            "mars" -> Icons.Default.LocalFireDepartment
            "jupiter" -> Icons.Default.AutoAwesome
            "saturn" -> Icons.Default.HourglassBottom
            "uranus" -> Icons.Default.FlashOn
            "neptune" -> Icons.Default.Waves
            "pluto" -> Icons.Default.Transform
            "north node" -> Icons.Default.NorthEast
            "south node" -> Icons.Default.SouthWest
            "chiron" -> Icons.Default.Healing
            "lilith" -> Icons.Default.Brightness3
            "part of fortune" -> Icons.Default.Toll
            "ascendant", "asc" -> Icons.AutoMirrored.Filled.TrendingUp
            "midheaven", "mc" -> Icons.Default.VerticalAlignTop
            else -> Icons.Default.Brightness1
        }
    }

    fun getElementIcon(element: String): ImageVector {
        return when (element.lowercase().trim()) {
            "fire" -> Icons.Default.LocalFireDepartment
            "earth" -> Icons.Default.Spa
            "air" -> Icons.Default.Air
            "water" -> Icons.Default.WaterDrop
            else -> Icons.Default.Circle
        }
    }

    fun getModalityIcon(modality: String): ImageVector {
        return when (modality.lowercase().trim()) {
            "cardinal" -> Icons.Default.Bolt
            "fixed" -> Icons.Default.Diamond
            "mutable" -> Icons.Default.Autorenew
            else -> Icons.Default.Circle
        }
    }
}

@Composable
fun PlanetVectorIcon(
    planetName: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        imageVector = AstroIconHelper.getPlanetIcon(planetName),
        contentDescription = planetName,
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun ElementVectorIcon(
    element: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        imageVector = AstroIconHelper.getElementIcon(element),
        contentDescription = element,
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun ModalityVectorIcon(
    modality: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        imageVector = AstroIconHelper.getModalityIcon(modality),
        contentDescription = modality,
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun DignityBadge(
    dignity: DignityType,
    modifier: Modifier = Modifier
) {
    val (label, icon, color) = when (dignity) {
        DignityType.DOMICILE -> Triple("Ruler", Icons.Default.Verified, Color(0xFF4CAF50))
        DignityType.EXALTATION -> Triple("Exalted", Icons.Default.North, Color(0xFF2196F3))
        DignityType.DETRIMENT -> Triple("Detriment", Icons.Default.Warning, Color(0xFFFF9800))
        DignityType.FALL -> Triple("Fall", Icons.Default.South, Color(0xFFF44336))
        DignityType.PEREGRINE -> Triple("Peregrine", Icons.Default.Circle, Color(0xFF9E9E9E))
    }

    if (dignity == DignityType.PEREGRINE) return

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
