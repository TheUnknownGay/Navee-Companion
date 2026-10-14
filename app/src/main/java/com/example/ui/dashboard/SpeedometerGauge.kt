package com.example.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RideMode
import com.example.model.ScooterTelemetry
import com.example.ui.theme.NaveeCyan
import com.example.ui.theme.NaveeGreen
import com.example.ui.theme.NaveeOrange
import com.example.ui.theme.NaveeRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    telemetry: ScooterTelemetry,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = telemetry.speedKmh,
        animationSpec = tween(durationMillis = 150),
        label = "speed"
    )

    val maxGaugeSpeed = 40.0f
    val speedRatio = (animatedSpeed / maxGaugeSpeed).coerceIn(0.0f, 1.0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Semi-circular gauge canvas
        Canvas(
            modifier = Modifier
                .size(240.dp, 160.dp)
                .testTag("speedometer_canvas")
        ) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth * 2, size.width - strokeWidth * 2)
            val topLeft = Offset(strokeWidth, strokeWidth)

            // Start at 145 degrees, sweep 250 degrees
            val startAngle = 145f
            val totalSweep = 250f

            // 1. Background Track
            drawArc(
                color = Color(0x33475569),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Active Speed Arc with gradient
            val activeColor = when {
                telemetry.speedKmh > 25.0f -> NaveeOrange
                telemetry.speedKmh > 15.0f -> NaveeCyan
                else -> NaveeGreen
            }

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        NaveeGreen,
                        NaveeCyan,
                        if (telemetry.speedKmh > 25f) NaveeOrange else NaveeCyan
                    )
                ),
                startAngle = startAngle,
                sweepAngle = (totalSweep * speedRatio).coerceAtLeast(1.0f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 3. Target Speed Limit Marker (Indicator line)
            val limitRatio = (telemetry.confirmedEscSpeedLimit / maxGaugeSpeed).coerceIn(0f, 1f)
            val markerAngle = (startAngle + totalSweep * limitRatio) * (PI / 180f).toFloat()
            val radius = (size.width - strokeWidth * 2) / 2f
            val center = Offset(size.width / 2f, size.width / 2f + strokeWidth)

            val markerStart = Offset(
                center.x + cos(markerAngle) * (radius - 10f),
                center.y + sin(markerAngle) * (radius - 10f)
            )
            val markerEnd = Offset(
                center.x + cos(markerAngle) * (radius + strokeWidth / 2f + 4f),
                center.y + sin(markerAngle) * (radius + strokeWidth / 2f + 4f)
            )

            drawLine(
                color = if (telemetry.isSpeedLimitClampedByEsc) NaveeRed else NaveeOrange,
                start = markerStart,
                end = markerEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center Digital Speed Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "%.1f".format(telemetry.speedKmh),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 58.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "km/h",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Power Watts Bar & Limit Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "LIMITE : ${telemetry.confirmedEscSpeedLimit} km/h",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (telemetry.isSpeedLimitClampedByEsc) NaveeRed else NaveeCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${telemetry.powerWatts.toInt()} W",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaveeOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
