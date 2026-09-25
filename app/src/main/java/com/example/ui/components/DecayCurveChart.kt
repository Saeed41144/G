package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CurvePoint
import com.example.domain.DecayCalculator
import com.example.domain.DecayConfig
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary
import kotlin.math.roundToInt

enum class ChartMetric(val titleFa: String, val titleEn: String) {
    MARGINAL("اثر هر تکرار (بازدهی)", "Marginal Gain"),
    CUMULATIVE("تسلط تجمیعی", "Cumulative Mastery")
}

@Composable
fun DecayCurveChart(
    baseGain: Double,
    config: DecayConfig,
    currentRepetition: Int = 0,
    maxReps: Int = 70,
    modifier: Modifier = Modifier,
    showControls: Boolean = true
) {
    var selectedMetric by remember { mutableStateOf(ChartMetric.MARGINAL) }
    var scrubRatio by remember { mutableFloatStateOf(-1f) } // -1f means no scrub

    val points = remember(baseGain, config, maxReps) {
        DecayCalculator.generateCurvePoints(baseGain, config, maxReps)
    }

    val halfLife = remember(config) {
        DecayCalculator.calculateHalfLifeReps(config)
    }

    // Determine current scrubbed point
    val activeScrubPoint: CurvePoint? = if (scrubRatio in 0f..1f && points.isNotEmpty()) {
        val index = (scrubRatio * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
        points[index]
    } else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("decay_curve_chart_container")
    ) {
        // Header & metric toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "نمودار شیب کاهش بازدهی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "فرمول: ${config.formula.displayNameFa.substringBefore(" (")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showControls) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = selectedMetric == ChartMetric.MARGINAL,
                        onClick = { selectedMetric = ChartMetric.MARGINAL },
                        label = { Text("بازدهی", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("chip_metric_marginal")
                    )
                    FilterChip(
                        selected = selectedMetric == ChartMetric.CUMULATIVE,
                        onClick = { selectedMetric = ChartMetric.CUMULATIVE },
                        label = { Text("تجمیعی", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldTertiary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("chip_metric_cumulative")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tooltip bar when user touches or highlights
        val displayPoint = activeScrubPoint ?: if (currentRepetition in points.indices) points[currentRepetition] else points.firstOrNull()

        if (displayPoint != null) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (selectedMetric == ChartMetric.MARGINAL) IndigoPrimary else EmeraldTertiary,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تکرار ${displayPoint.repetition}-ام" + if (displayPoint.repetition == currentRepetition && currentRepetition > 0) " (شما اینجایید)" else "",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "اثر: ${DecayCalculator.formatGainPercent(displayPoint.marginalGain)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = IndigoPrimary
                        )
                        Text(
                            text = "بازدهی: ${DecayCalculator.formatEfficiency(displayPoint.efficiencyRatio)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberAccent
                        )
                        Text(
                            text = "مجموع: ${DecayCalculator.formatGainPercent(displayPoint.cumulativeGain, includePlus = false)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldTertiary
                        )
                    }
                }
            }
        }

        // Canvas Area
        val primaryCurveColor = if (selectedMetric == ChartMetric.MARGINAL) IndigoPrimary else EmeraldTertiary
        val surfaceColor = MaterialTheme.colorScheme.surface
        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(surfaceColor, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            scrubRatio = ratio
                            tryAwaitRelease()
                            scrubRatio = -1f
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            scrubRatio = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = { scrubRatio = -1f },
                        onDragCancel = { scrubRatio = -1f },
                        onDrag = { change, _ ->
                            change.consume()
                            scrubRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
                }
                .testTag("canvas_decay_chart")
        ) {
            Canvas(modifier = Modifier.matchParentSize().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 22.dp)) {
                if (points.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height

                // Draw subtle horizontal grid lines (0%, 25%, 50%, 75%, 100%)
                val gridColor = Color(0x18888888)
                val gridDashes = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                for (i in 0..4) {
                    val y = height * (i / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                        pathEffect = gridDashes
                    )
                }

                // Max values for scaling
                val maxVal = if (selectedMetric == ChartMetric.MARGINAL) {
                    baseGain * 1.05
                } else {
                    points.last().noDecayCumulativeGain.coerceAtLeast(0.0001)
                }

                val minVal = 0.0

                fun getX(index: Int): Float = (index.toFloat() / (points.size - 1)) * width
                fun getY(value: Double): Float {
                    val normalized = ((value - minVal) / (maxVal - minVal)).coerceIn(0.0, 1.0)
                    return height - (normalized.toFloat() * height)
                }

                // If Cumulative, draw linear "No Decay" baseline dashed line
                if (selectedMetric == ChartMetric.CUMULATIVE) {
                    val noDecayPath = Path().apply {
                        moveTo(getX(0), getY(points.first().noDecayCumulativeGain))
                        for (i in 1 until points.size) {
                            lineTo(getX(i), getY(points[i].noDecayCumulativeGain))
                        }
                    }
                    drawPath(
                        path = noDecayPath,
                        color = Color(0xFF94A3B8),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }

                // If Marginal, draw floor dashed line
                if (selectedMetric == ChartMetric.MARGINAL) {
                    val floorGain = baseGain * config.minEfficiency
                    val floorY = getY(floorGain)
                    drawLine(
                        color = AmberAccent.copy(alpha = 0.6f),
                        start = Offset(0f, floorY),
                        end = Offset(width, floorY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // Build main curve path
                val curvePath = Path()
                val fillPath = Path()

                val firstY = if (selectedMetric == ChartMetric.MARGINAL) {
                    getY(points.first().marginalGain)
                } else {
                    getY(points.first().cumulativeGain)
                }

                curvePath.moveTo(0f, firstY)
                fillPath.moveTo(0f, height)
                fillPath.lineTo(0f, firstY)

                for (i in 1 until points.size) {
                    val prevX = getX(i - 1)
                    val prevY = if (selectedMetric == ChartMetric.MARGINAL) getY(points[i - 1].marginalGain) else getY(points[i - 1].cumulativeGain)
                    val curX = getX(i)
                    val curY = if (selectedMetric == ChartMetric.MARGINAL) getY(points[i].marginalGain) else getY(points[i].cumulativeGain)

                    val midX = (prevX + curX) / 2f
                    curvePath.cubicTo(midX, prevY, midX, curY, curX, curY)
                    fillPath.cubicTo(midX, prevY, midX, curY, curX, curY)
                }

                fillPath.lineTo(width, height)
                fillPath.close()

                // Draw gradient fill below curve
                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        primaryCurveColor.copy(alpha = 0.28f),
                        primaryCurveColor.copy(alpha = 0.02f)
                    ),
                    startY = 0f,
                    endY = height
                )
                drawPath(path = fillPath, brush = gradient)

                // Draw curve stroke
                drawPath(
                    path = curvePath,
                    color = primaryCurveColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // Draw current task repetition marker if available
                if (currentRepetition in points.indices) {
                    val markerX = getX(currentRepetition)
                    val markerVal = if (selectedMetric == ChartMetric.MARGINAL) points[currentRepetition].marginalGain else points[currentRepetition].cumulativeGain
                    val markerY = getY(markerVal)

                    // Outer pulse ring
                    drawCircle(
                        color = primaryCurveColor.copy(alpha = 0.35f),
                        radius = 12f,
                        center = Offset(markerX, markerY)
                    )
                    // Inner white dot
                    drawCircle(
                        color = Color.White,
                        radius = 5f,
                        center = Offset(markerX, markerY)
                    )
                    drawCircle(
                        color = primaryCurveColor,
                        radius = 3.5f,
                        center = Offset(markerX, markerY)
                    )
                }

                // If user is scrubbing, draw scrubber line and intersection dot
                if (scrubRatio in 0f..1f) {
                    val scrubIndex = (scrubRatio * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                    val scrubX = getX(scrubIndex)
                    val scrubVal = if (selectedMetric == ChartMetric.MARGINAL) points[scrubIndex].marginalGain else points[scrubIndex].cumulativeGain
                    val scrubY = getY(scrubVal)

                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(scrubX, 0f),
                        end = Offset(scrubX, height),
                        strokeWidth = 2f
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = Offset(scrubX, scrubY)
                    )
                    drawCircle(
                        color = primaryCurveColor,
                        radius = 4f,
                        center = Offset(scrubX, scrubY)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend & Explanatory Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Legend item 1: Curve
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(primaryCurveColor, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedMetric == ChartMetric.MARGINAL) "منحنی شیب اثر" else "رشد واقعی",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Legend item 2: Floor or Baseline
                if (selectedMetric == ChartMetric.MARGINAL) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(AmberAccent, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "حداقل بازدهی (${(config.minEfficiency * 100).toInt()}٪)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF94A3B8), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "بدون کاهش (فرضی)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Half-life info
            if (halfLife > 0) {
                Text(
                    text = "نیمه‌عمر: تکرار $halfLife",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
