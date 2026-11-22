package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.BananaModePersona
import com.example.ui.theme.TuulitCharcoal
import com.example.ui.theme.TuulitParchment
import com.example.ui.theme.TuulitThemeTokens

/**
 * Minimalist flat-vector Half-Peeled Banana mascot.
 * Strictly flat solid colors, crisp charcoal linework, zero gradients, zero green, zero emojis.
 */
@Composable
fun BananaMascot(
    persona: BananaModePersona = BananaModePersona.NEUTRAL,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    showContainer: Boolean = true,
) {
    val extended = TuulitThemeTokens.extended
    val bgColor = extended.warmTan
    val borderColor = extended.hairlineBorder
    val yellow = extended.bananaYellow
    val innerTan = extended.toastedSand
    val clay = extended.periodClay
    val ink = TuulitCharcoal
    val flesh = TuulitParchment

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showContainer) {
                    Modifier
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(1.dp, borderColor, CircleShape)
                } else Modifier
            )
            .semantics {
                contentDescription = "Tuulit mascot in ${persona.title} expression"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.78f)) {
            val w = this.size.width
            val h = this.size.height
            val strokeW = (w * 0.042f).coerceAtLeast(2.2f)
            val outlineStroke = Stroke(
                width = strokeW,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            // 1. Upper banana flesh (peeled top)
            val fleshPath = Path().apply {
                moveTo(w * 0.42f, h * 0.56f)
                cubicTo(w * 0.36f, h * 0.34f, w * 0.44f, h * 0.16f, w * 0.56f, h * 0.12f)
                cubicTo(w * 0.65f, h * 0.10f, w * 0.67f, h * 0.22f, w * 0.62f, h * 0.38f)
                cubicTo(w * 0.59f, h * 0.48f, w * 0.58f, h * 0.54f, w * 0.58f, h * 0.56f)
                close()
            }
            drawPath(fleshPath, color = flesh)
            drawPath(fleshPath, color = ink, style = outlineStroke)

            // Tip dot
            drawCircle(
                color = ink,
                radius = strokeW * 0.75f,
                center = Offset(w * 0.59f, h * 0.13f)
            )

            // 2. Gym Banana headband (if GYM mode)
            if (persona == BananaModePersona.GYM) {
                val band = Path().apply {
                    moveTo(w * 0.43f, h * 0.28f)
                    lineTo(w * 0.63f, h * 0.25f)
                    lineTo(w * 0.62f, h * 0.31f)
                    lineTo(w * 0.42f, h * 0.34f)
                    close()
                }
                drawPath(band, color = clay)
                drawPath(band, color = ink, style = Stroke(width = strokeW * 0.75f))
            }

            // 3. Lower unpeeled body curving right
            val bodyPath = Path().apply {
                moveTo(w * 0.42f, h * 0.54f)
                cubicTo(w * 0.42f, h * 0.74f, w * 0.56f, h * 0.87f, w * 0.75f, h * 0.89f)
                lineTo(w * 0.80f, h * 0.83f)
                cubicTo(w * 0.64f, h * 0.76f, w * 0.59f, h * 0.66f, w * 0.58f, h * 0.54f)
                close()
            }
            drawPath(bodyPath, color = yellow)
            drawPath(bodyPath, color = ink, style = outlineStroke)

            // Bottom stem cap
            drawRoundRect(
                color = ink,
                topLeft = Offset(w * 0.74f, h * 0.82f),
                size = Size(w * 0.08f, h * 0.08f),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // 4. Back left peel (tan inner shadow)
            val backPeel = Path().apply {
                moveTo(w * 0.44f, h * 0.52f)
                cubicTo(w * 0.26f, h * 0.46f, w * 0.14f, h * 0.58f, w * 0.14f, h * 0.74f)
                cubicTo(w * 0.22f, h * 0.72f, w * 0.27f, h * 0.60f, w * 0.42f, h * 0.56f)
                close()
            }
            drawPath(backPeel, color = innerTan)
            drawPath(backPeel, color = ink, style = outlineStroke)

            // 5. Front left curled peel (banana yellow)
            val leftPeel = Path().apply {
                moveTo(w * 0.47f, h * 0.53f)
                cubicTo(w * 0.30f, h * 0.52f, w * 0.23f, h * 0.69f, w * 0.29f, h * 0.84f)
                cubicTo(w * 0.37f, h * 0.82f, w * 0.38f, h * 0.66f, w * 0.50f, h * 0.58f)
                close()
            }
            drawPath(leftPeel, color = yellow)
            drawPath(leftPeel, color = ink, style = outlineStroke)

            // 6. Right curled peel (banana yellow + tan underside)
            val rightPeel = Path().apply {
                moveTo(w * 0.55f, h * 0.53f)
                cubicTo(w * 0.72f, h * 0.47f, w * 0.86f, h * 0.57f, w * 0.84f, h * 0.72f)
                cubicTo(w * 0.75f, h * 0.70f, w * 0.71f, h * 0.59f, w * 0.56f, h * 0.58f)
                close()
            }
            drawPath(rightPeel, color = yellow)
            drawPath(rightPeel, color = ink, style = outlineStroke)

            // 7. Cramps Banana cozy warm compress / scarf wrap
            if (persona == BananaModePersona.CRAMPS) {
                drawRoundRect(
                    color = clay,
                    topLeft = Offset(w * 0.37f, h * 0.50f),
                    size = Size(w * 0.26f, h * 0.11f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(w * 0.37f, h * 0.50f),
                    size = Size(w * 0.26f, h * 0.11f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                    style = Stroke(width = strokeW * 0.7f)
                )
            }

            // 8. Minimalist facial expression on upper flesh
            val leftEye = Offset(w * 0.47f, h * 0.36f)
            val rightEye = Offset(w * 0.55f, h * 0.35f)
            val eyeStroke = Stroke(width = strokeW * 0.75f, cap = StrokeCap.Round)

            when (persona) {
                BananaModePersona.NEUTRAL -> {
                    drawCircle(ink, radius = strokeW * 0.55f, center = leftEye)
                    drawCircle(ink, radius = strokeW * 0.55f, center = rightEye)
                    val smile = Path().apply {
                        moveTo(w * 0.48f, h * 0.42f)
                        quadraticTo(w * 0.515f, h * 0.455f, w * 0.55f, h * 0.415f)
                    }
                    drawPath(smile, color = ink, style = eyeStroke)
                }
                BananaModePersona.CHILL -> {
                    val le = Path().apply {
                        moveTo(leftEye.x - w * 0.025f, leftEye.y)
                        quadraticTo(leftEye.x, leftEye.y + h * 0.025f, leftEye.x + w * 0.025f, leftEye.y)
                    }
                    val re = Path().apply {
                        moveTo(rightEye.x - w * 0.025f, rightEye.y)
                        quadraticTo(rightEye.x, rightEye.y + h * 0.025f, rightEye.x + w * 0.025f, rightEye.y)
                    }
                    drawPath(le, color = ink, style = eyeStroke)
                    drawPath(re, color = ink, style = eyeStroke)
                    val smile = Path().apply {
                        moveTo(w * 0.49f, h * 0.42f)
                        quadraticTo(w * 0.515f, h * 0.445f, w * 0.54f, h * 0.42f)
                    }
                    drawPath(smile, color = ink, style = eyeStroke)
                }
                BananaModePersona.CRAMPS -> {
                    val le = Path().apply {
                        moveTo(leftEye.x - w * 0.02f, leftEye.y + h * 0.01f)
                        lineTo(leftEye.x + w * 0.02f, leftEye.y - h * 0.01f)
                    }
                    val re = Path().apply {
                        moveTo(rightEye.x - w * 0.02f, rightEye.y - h * 0.01f)
                        lineTo(rightEye.x + w * 0.02f, rightEye.y + h * 0.01f)
                    }
                    drawPath(le, color = ink, style = eyeStroke)
                    drawPath(re, color = ink, style = eyeStroke)
                    val wave = Path().apply {
                        moveTo(w * 0.48f, h * 0.43f)
                        quadraticTo(w * 0.515f, h * 0.405f, w * 0.55f, h * 0.43f)
                    }
                    drawPath(wave, color = ink, style = eyeStroke)
                }
                BananaModePersona.SLEEPY -> {
                    drawLine(ink, Offset(leftEye.x - w * 0.025f, leftEye.y), Offset(leftEye.x + w * 0.02f, leftEye.y), strokeWidth = strokeW * 0.75f, cap = StrokeCap.Round)
                    drawLine(ink, Offset(rightEye.x - w * 0.02f, rightEye.y), Offset(rightEye.x + w * 0.025f, rightEye.y), strokeWidth = strokeW * 0.75f, cap = StrokeCap.Round)
                    drawCircle(ink, radius = strokeW * 0.55f, center = Offset(w * 0.515f, h * 0.43f), style = eyeStroke)
                    // Minimal Z line mark in top-right
                    val zPath = Path().apply {
                        moveTo(w * 0.74f, h * 0.16f)
                        lineTo(w * 0.84f, h * 0.16f)
                        lineTo(w * 0.74f, h * 0.25f)
                        lineTo(w * 0.84f, h * 0.25f)
                    }
                    drawPath(zPath, color = ink, style = Stroke(width = strokeW * 0.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                BananaModePersona.DRAMA -> {
                    drawCircle(ink, radius = strokeW * 0.6f, center = leftEye)
                    drawCircle(ink, radius = strokeW * 0.6f, center = rightEye)
                    // Raised brow
                    drawLine(ink, Offset(rightEye.x - w * 0.025f, rightEye.y - h * 0.045f), Offset(rightEye.x + w * 0.03f, rightEye.y - h * 0.06f), strokeWidth = strokeW * 0.65f, cap = StrokeCap.Round)
                    val smirk = Path().apply {
                        moveTo(w * 0.48f, h * 0.43f)
                        quadraticTo(w * 0.53f, h * 0.44f, w * 0.56f, h * 0.405f)
                    }
                    drawPath(smirk, color = ink, style = eyeStroke)
                }
                BananaModePersona.GYM -> {
                    drawCircle(ink, radius = strokeW * 0.55f, center = leftEye)
                    drawCircle(ink, radius = strokeW * 0.55f, center = rightEye)
                    val smile = Path().apply {
                        moveTo(w * 0.48f, h * 0.42f)
                        quadraticTo(w * 0.515f, h * 0.46f, w * 0.55f, h * 0.415f)
                    }
                    drawPath(smile, color = ink, style = eyeStroke)
                }
                BananaModePersona.HAPPY -> {
                    val le = Path().apply {
                        moveTo(leftEye.x - w * 0.025f, leftEye.y + h * 0.01f)
                        quadraticTo(leftEye.x, leftEye.y - h * 0.025f, leftEye.x + w * 0.025f, leftEye.y + h * 0.01f)
                    }
                    val re = Path().apply {
                        moveTo(rightEye.x - w * 0.025f, rightEye.y + h * 0.01f)
                        quadraticTo(rightEye.x, rightEye.y - h * 0.025f, rightEye.x + w * 0.025f, rightEye.y + h * 0.01f)
                    }
                    drawPath(le, color = ink, style = eyeStroke)
                    drawPath(re, color = ink, style = eyeStroke)
                    val bigSmile = Path().apply {
                        moveTo(w * 0.47f, h * 0.41f)
                        quadraticTo(w * 0.515f, h * 0.475f, w * 0.56f, h * 0.41f)
                    }
                    drawPath(bigSmile, color = ink, style = eyeStroke)
                    // Minimal 4-point sparkle in yellow
                    drawLine(yellow, Offset(w * 0.22f, h * 0.20f), Offset(w * 0.22f, h * 0.30f), strokeWidth = strokeW * 0.7f, cap = StrokeCap.Round)
                    drawLine(yellow, Offset(w * 0.17f, h * 0.25f), Offset(w * 0.27f, h * 0.25f), strokeWidth = strokeW * 0.7f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
fun TuulitCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = TuulitThemeTokens.extended.hairlineBorder,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun TuulitSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun TuulitBadge(
    text: String,
    icon: ImageVector? = null,
    containerColor: Color = TuulitThemeTokens.extended.warmTan,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Composable
fun TuulitPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TuulitSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, TuulitThemeTokens.extended.hairlineBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TuulitNoticeBanner(
    title: String,
    message: String,
    icon: ImageVector,
    containerColor: Color = TuulitThemeTokens.extended.estimateOchreSoft,
    accentColor: Color = TuulitThemeTokens.extended.estimateOchre,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
