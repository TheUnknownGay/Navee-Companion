package com.example.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScooterTelemetry
import com.example.ui.theme.NaveeCyan
import com.example.ui.theme.NaveeOrange
import com.example.ui.theme.NaveeRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VehicleModelCanvas(
    telemetry: ScooterTelemetry,
    modifier: Modifier = Modifier
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var tiltAngle by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var showWireframe by remember { mutableStateOf(false) }

    // Wheel rotation animation synced with actual speed
    val infiniteTransition = rememberInfiniteTransition(label = "wheels")
    val wheelSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (telemetry.speedKmh > 0.5f) {
                    (2500 / telemetry.speedKmh.coerceAtLeast(1.0f)).toInt().coerceIn(80, 2000)
                } else 100000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wheelRotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    rotationAngle = (rotationAngle + pan.x * 0.4f).coerceIn(-65f, 65f)
                    tiltAngle = (tiltAngle + pan.y * 0.2f).coerceIn(-20f, 20f)
                    zoomScale = (zoomScale * zoom).coerceIn(0.75f, 1.4f)
                }
            }
            .testTag("vehicle_3d_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f + 25f

            // Ground shadow & ambient lighting
            drawGroundGrid(cx, cy + 80f, rotationAngle, telemetry.speedKmh > 0.5f)

            // Scale and perspective rotation
            scale(zoomScale, pivot = Offset(cx, cy)) {
                // Apply 3D-like parallax offset
                val perspectiveShiftX = (rotationAngle / 45f) * 40f
                val perspectiveShiftY = (tiltAngle / 20f) * 20f

                translate(left = perspectiveShiftX, top = perspectiveShiftY) {
                    drawNaveeNt5Max(
                        telemetry = telemetry,
                        cx = cx,
                        cy = cy,
                        rotationAngle = rotationAngle,
                        wheelSpin = if (telemetry.speedKmh > 0.5f) wheelSpin else 0f,
                        isWireframe = showWireframe
                    )
                }
            }

            // Headlight illumination beam effect on ground
            if (telemetry.headlightOn) {
                drawHeadlightBeam(cx, cy, rotationAngle)
            }
        }

        // Lock shield overlay
        if (telemetry.isLocked) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f),
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒 MOTEUR VERROUILLÉ",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Quick overlay actions (Reset view, wireframe toggle)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            FilledIconButton(
                onClick = {
                    rotationAngle = 0f
                    tiltAngle = 0f
                    zoomScale = 1.0f
                },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Réinitialiser la vue 3D",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Vehicle badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            ) {
                Text(
                    text = "NAVEE NT5 MAX · Rendu Interactif",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaveeCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Gesture hint
        Text(
            text = "Glisser pour pivoter · Pincer pour zoomer",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

private fun DrawScope.drawGroundGrid(cx: Float, groundY: Float, rotationAngle: Float, isMoving: Boolean) {
    // Underglow neon ellipse
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                NaveeCyan.copy(alpha = 0.25f),
                Color.Transparent
            ),
            center = Offset(cx, groundY),
            radius = 160f
        ),
        topLeft = Offset(cx - 160f, groundY - 24f),
        size = Size(320f, 48f)
    )

    // Contact shadows under front and rear tires
    drawOval(
        color = Color(0x77000000),
        topLeft = Offset(cx - 130f, groundY - 8f),
        size = Size(70f, 16f)
    )
    drawOval(
        color = Color(0x77000000),
        topLeft = Offset(cx + 80f, groundY - 8f),
        size = Size(70f, 16f)
    )
}

private fun DrawScope.drawHeadlightBeam(cx: Float, cy: Float, rotationAngle: Float) {
    val lightSource = Offset(cx + 105f, cy - 85f)
    val beamPath = Path().apply {
        moveTo(lightSource.x, lightSource.y)
        lineTo(lightSource.x + 220f, lightSource.y - 20f)
        lineTo(lightSource.x + 240f, lightSource.y + 120f)
        close()
    }

    drawPath(
        path = beamPath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f),
                NaveeCyan.copy(alpha = 0.25f),
                Color.Transparent
            ),
            center = lightSource,
            radius = 240f
        )
    )
}

private fun DrawScope.drawNaveeNt5Max(
    telemetry: ScooterTelemetry,
    cx: Float,
    cy: Float,
    rotationAngle: Float,
    wheelSpin: Float,
    isWireframe: Boolean
) {
    val frameColor = Color(0xFF1E2633)
    val metalHighlight = Color(0xFF475569)
    val orangeAccent = NaveeOrange
    val tireColor = Color(0xFF12151C)
    val rimColor = Color(0xFF334155)

    val rearWheelCenter = Offset(cx - 95f, cy + 45f)
    val frontWheelCenter = Offset(cx + 115f, cy + 45f)
    val wheelRadius = 36f

    // 1. REAR WHEEL (10-inch Fat Tire)
    drawTireAndWheel(rearWheelCenter, wheelRadius, tireColor, rimColor, wheelSpin, isBraking = telemetry.speedKmh <= 0.5f)

    // 2. FRONT WHEEL (with motor / drum brake hub)
    drawTireAndWheel(frontWheelCenter, wheelRadius, tireColor, rimColor, wheelSpin, isBraking = false)

    // 3. NT5 MAX DAMPING ARM SUSPENSIONS (Dual front shock arms)
    val forkTop = Offset(cx + 90f, cy - 40f)
    drawDampingArmSuspension(frontWheelCenter, forkTop, orangeAccent, metalHighlight)

    // 4. MAIN DECK (Châssis robuste avec repose-pieds large)
    val deckStart = Offset(cx - 75f, cy + 30f)
    val deckEnd = Offset(cx + 65f, cy + 30f)

    // Deck extrusion
    val deckPath = Path().apply {
        moveTo(deckStart.x, deckStart.y)
        lineTo(deckEnd.x, deckEnd.y)
        lineTo(deckEnd.x + 10f, deckEnd.y - 12f)
        lineTo(deckStart.x - 10f, deckStart.y - 12f)
        close()
    }
    drawPath(deckPath, color = frameColor)
    drawPath(deckPath, color = metalHighlight, style = Stroke(width = 2.5f))

    // Silicone grip deck top with NAVEE signature orange strip
    drawLine(
        color = orangeAccent,
        start = Offset(deckStart.x + 5f, deckStart.y - 10f),
        end = Offset(deckEnd.x - 5f, deckEnd.y - 10f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    // 5. REAR MUDGUARD & TAIL LIGHT
    val mudguardPath = Path().apply {
        moveTo(deckStart.x - 5f, deckStart.y - 8f)
        cubicTo(
            deckStart.x - 20f, deckStart.y - 30f,
            rearWheelCenter.x - 20f, rearWheelCenter.y - 30f,
            rearWheelCenter.x - 38f, rearWheelCenter.y - 5f
        )
    }
    drawPath(mudguardPath, color = frameColor, style = Stroke(width = 6f, cap = StrokeCap.Round))

    // Tail light LED (glowing red when on or braking)
    val tailLightPos = Offset(rearWheelCenter.x - 36f, rearWheelCenter.y - 18f)
    drawCircle(
        color = NaveeRed,
        radius = 5f,
        center = tailLightPos
    )
    drawCircle(
        color = NaveeRed.copy(alpha = 0.5f),
        radius = 10f,
        center = tailLightPos
    )

    // 6. STEERING COLUMN / STEM (15° Rake Angle characteristic of NT5 Max)
    val stemTop = Offset(cx + 70f, cy - 90f)
    val handlebarCenter = Offset(cx + 68f, cy - 95f)

    drawLine(
        color = frameColor,
        start = forkTop,
        end = stemTop,
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = metalHighlight,
        start = forkTop,
        end = stemTop,
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )

    // Folding latch mechanism (Brightway heavy-duty clamp)
    drawRoundRect(
        color = orangeAccent,
        topLeft = Offset(forkTop.x - 7f, forkTop.y - 12f),
        size = Size(14f, 10f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // 7. WIDE HANDLEBARS & DIGITAL DASHBOARD
    // Handlebar bar
    drawLine(
        color = frameColor,
        start = Offset(handlebarCenter.x - 28f, handlebarCenter.y - 4f),
        end = Offset(handlebarCenter.x + 28f, handlebarCenter.y - 4f),
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )
    // Rubber grips
    drawLine(
        color = tireColor,
        start = Offset(handlebarCenter.x - 28f, handlebarCenter.y - 4f),
        end = Offset(handlebarCenter.x - 16f, handlebarCenter.y - 4f),
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = tireColor,
        start = Offset(handlebarCenter.x + 16f, handlebarCenter.y - 4f),
        end = Offset(handlebarCenter.x + 28f, handlebarCenter.y - 4f),
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )

    // Cockpit Display (Glowing Cyan)
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(handlebarCenter.x - 9f, handlebarCenter.y - 11f),
        size = Size(18f, 14f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = NaveeCyan,
        topLeft = Offset(handlebarCenter.x - 7f, handlebarCenter.y - 9f),
        size = Size(14f, 10f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // 8. DUAL HEADLIGHT POD
    val headlightPod = Offset(cx + 80f, cy - 80f)
    drawCircle(
        color = if (telemetry.headlightOn) Color.White else Color(0xFF64748B),
        radius = 6f,
        center = headlightPod
    )
    if (telemetry.headlightOn) {
        drawCircle(
            color = NaveeCyan.copy(alpha = 0.6f),
            radius = 12f,
            center = headlightPod
        )
    }

    // 9. KICKSTAND (Béquille latérale)
    drawLine(
        color = metalHighlight,
        start = Offset(cx - 20f, cy + 30f),
        end = Offset(cx - 30f, cy + 50f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawTireAndWheel(
    center: Offset,
    radius: Float,
    tireColor: Color,
    rimColor: Color,
    rotationDeg: Float,
    isBraking: Boolean
) {
    // Tire outer ring (Fat 10" x 2.5" pneumatic tire)
    drawCircle(
        color = tireColor,
        radius = radius,
        center = center,
        style = Stroke(width = 12f)
    )

    // Tire tread pattern
    drawCircle(
        color = Color(0xFF262D3D),
        radius = radius + 2f,
        center = center,
        style = Stroke(
            width = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    )

    // Rim
    drawCircle(
        color = rimColor,
        radius = radius - 8f,
        center = center
    )

    // Disc Brake Rotor
    drawCircle(
        color = Color(0xFF94A3B8),
        radius = radius - 15f,
        center = center,
        style = Stroke(width = 2.5f)
    )

    // Brake Caliper
    val caliperColor = if (isBraking) NaveeRed else NaveeOrange
    drawRoundRect(
        color = caliperColor,
        topLeft = Offset(center.x + 8f, center.y - 12f),
        size = Size(8f, 14f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Rotating spokes / hub details
    rotate(degrees = rotationDeg, pivot = center) {
        for (i in 0 until 5) {
            val angle = (i * 72f) * (PI / 180f).toFloat()
            val end = Offset(
                center.x + cos(angle) * (radius - 12f),
                center.y + sin(angle) * (radius - 12f)
            )
            drawLine(
                color = Color(0xFF64748B),
                start = center,
                end = end,
                strokeWidth = 2.5f
            )
        }
        // Wheel hub cap with Navee logo center
        drawCircle(
            color = NaveeOrange,
            radius = 5f,
            center = center
        )
    }
}

private fun DrawScope.drawDampingArmSuspension(
    wheelCenter: Offset,
    forkTop: Offset,
    accentColor: Color,
    metalColor: Color
) {
    // Twin Damping Arm™ geometry
    val armPivot = Offset(wheelCenter.x - 22f, wheelCenter.y - 12f)

    // Swingarm
    drawLine(
        color = metalColor,
        start = armPivot,
        end = wheelCenter,
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )

    // Shock spring / Damper cylinder
    drawLine(
        color = accentColor,
        start = armPivot,
        end = forkTop,
        strokeWidth = 7f,
        cap = StrokeCap.Round
    )

    // Spring coils effect
    drawLine(
        color = Color(0xFF0F172A),
        start = armPivot,
        end = forkTop,
        strokeWidth = 5f,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
    )
}
