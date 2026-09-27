package com.example.ui.screens

import android.graphics.RectF
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    // Timeout to proceed to main screen (2.5 seconds)
    LaunchedEffect(Unit) {
        delay(2500)
        onTimeout()
    }

    // Infinite transition for spinning gear and pulse animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    
    val gearRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gearRotation"
    )

    val armPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "armPulse"
    )

    val alphaAnimation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alphaAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, easing = EaseOutQuad)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030D12)) // Cyber Dark Slate Base Blue
            .testTag("app_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // High-fidelity custom Canvas reproducing the exact visual weight of the Auto Production Study logo
            Canvas(
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer(
                        alpha = alphaAnimation.value,
                        scaleX = alphaAnimation.value * 0.95f + 0.05f,
                        scaleY = alphaAnimation.value * 0.95f + 0.05f
                    )
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerRadius = size.minDimension * 0.42f
                val innerRadius = outerRadius * 0.85f

                // 1. Solid Outer Stroke Accent Ring
                drawCircle(
                    color = Color(0xFF1AA6B7),
                    radius = outerRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.dp.toPx())
                )

                // 2. Main Circle Gradient Backdrop (Dark slate teal)
                val backdropBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFF16526D), Color(0xFF0C2433)),
                    center = Offset(cx, cy),
                    radius = outerRadius
                )
                drawCircle(
                    brush = backdropBrush,
                    radius = outerRadius - 1.5.dp.toPx(),
                    center = Offset(cx, cy)
                )

                // 3. Rotating Tech Gear
                rotate(degrees = gearRotation, pivot = Offset(cx, cy)) {
                    val gearOuterR = innerRadius * 0.65f
                    val gearInnerR = gearOuterR * 0.60f
                    val toothCount = 10
                    val toothHeight = 12.dp.toPx()
                    val toothWidth = 14.dp.toPx()

                    // Draw teeth
                    for (i in 0 until toothCount) {
                        val angle = i * (360f / toothCount)
                        rotate(degrees = angle, pivot = Offset(cx, cy)) {
                            drawRoundRect(
                                color = Color(0x6676A5AF),
                                topLeft = Offset(cx - toothWidth / 2f, cy - gearOuterR - toothHeight / 2f),
                                size = Size(toothWidth, toothHeight),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }

                    // Gear Rim
                    drawCircle(
                        color = Color(0x6676A5AF),
                        radius = gearOuterR - 2.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 6.dp.toPx())
                    )
                }

                // 4. Tech Chart Nodes & Antennas (Top Left)
                val nodeColor = Color(0xFFFF9F43) // Warm Orange In Logo
                val chartYOffset = cy - innerRadius * 0.45f
                val chartXOffset = cx - innerRadius * 0.45f

                drawLine(
                    color = Color(0xFF81ECEC),
                    start = Offset(chartXOffset, chartYOffset),
                    end = Offset(chartXOffset + 35.dp.toPx(), chartYOffset - 15.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF81ECEC),
                    start = Offset(chartXOffset + 35.dp.toPx(), chartYOffset - 15.dp.toPx()),
                    end = Offset(chartXOffset + 70.dp.toPx(), chartYOffset + 10.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )

                drawCircle(
                    color = nodeColor,
                    radius = 5.dp.toPx(),
                    center = Offset(chartXOffset, chartYOffset)
                )
                drawCircle(
                    color = Color(0xFF81ECEC),
                    radius = 4.dp.toPx(),
                    center = Offset(chartXOffset + 35.dp.toPx(), chartYOffset - 15.dp.toPx())
                )
                drawCircle(
                    color = nodeColor,
                    radius = 6.dp.toPx(),
                    center = Offset(chartXOffset + 70.dp.toPx(), chartYOffset + 10.dp.toPx())
                )

                // 5. Stylized Industrial Arrow "A" Path (Center Progress Vector)
                val arrowPath = Path().apply {
                    moveTo(cx - 38.dp.toPx(), cy + 45.dp.toPx())
                    quadraticBezierTo(
                        cx - 15.dp.toPx(), cy - 25.dp.toPx(),
                        cx + 5.dp.toPx(), cy - 15.dp.toPx()
                    )
                    quadraticBezierTo(
                        cx + 25.dp.toPx(), cy - 5.dp.toPx(),
                        cx + 42.dp.toPx(), cy + 15.dp.toPx()
                    )
                }
                drawPath(
                    path = arrowPath,
                    color = Color(0xFF00E5FF), // Electrical Blue
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Styling highlight overlay for the letter "A" / orange wedge
                drawRoundRect(
                    color = Color(0xFFFF9F43),
                    topLeft = Offset(cx - 8.dp.toPx(), cy + 5.dp.toPx()),
                    size = Size(16.dp.toPx(), 40.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = 5.dp.toPx())
                )

                // 6. Cyber Robotic Joint Arm (Right side, holding cubes)
                val armPivotX = cx + innerRadius * 0.50f
                val armPivotY = cy - innerRadius * 0.35f
                val pulseOffset = 5.dp.toPx() * (armPulse - 1f)

                // Primary robotic shoulder and arm segments
                drawLine(
                    color = Color(0xFFA29BFE),
                    start = Offset(armPivotX, armPivotY),
                    end = Offset(armPivotX + 15.dp.toPx(), armPivotY - 25.dp.toPx() + pulseOffset),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFA29BFE),
                    start = Offset(armPivotX + 15.dp.toPx(), armPivotY - 25.dp.toPx() + pulseOffset),
                    end = Offset(cx + 8.dp.toPx(), cy - 18.dp.toPx() + pulseOffset),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Glowing Production Cubes
                drawRoundRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(cx + 12.dp.toPx(), cy - 6.dp.toPx() + pulseOffset),
                    size = Size(11.dp.toPx(), 11.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFFFF9F43),
                    topLeft = Offset(cx + 29.dp.toPx(), cy + 5.dp.toPx() + pulseOffset),
                    size = Size(10.dp.toPx(), 10.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // 7. Circular Outer Curved Arc Text Backdrop/Ribbon
                // Drawing nice curved ring segment at bottom for the text placement
                drawArc(
                    color = Color(0xFF14455C),
                    startAngle = 35f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(cx - innerRadius, cy - innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Text on Curved Path using native Canvas to ensure beautiful alignment inside splash
                drawIntoCanvas { composeCanvas ->
                    val nativeCanvas = composeCanvas.nativeCanvas
                    val bounds = RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius)
                    val textPath = android.graphics.Path().apply {
                        addArc(bounds, 34f, 112f)
                    }
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 10.sp.toPx()
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        textAlign = android.graphics.Paint.Align.CENTER
                        letterSpacing = 0.08f
                    }
                    // Offset text on path vertically slightly to center in the ribbon
                    nativeCanvas.drawTextOnPath("AUTO PRODUCTION STUDY", textPath, 0f, 4.dp.toPx(), paint)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Subtext under the central animated circular logo
            Text(
                text = "AUTO PRODUCTION STUDY",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Serif
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "IE Apparel & Assembly Pro • Industrial Engine",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1AA6B7),
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
