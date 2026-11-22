package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DashboardTrendPoint
import com.example.model.FinancialSnapshot
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Recharts-style interactive Area Chart for visualizing Cash Reserves and Net Margin over time.
 * Supports smooth cubic Bézier curves, gradient underfill, grid lines, and interactive scrub tooltips.
 */
@Composable
fun RechartsAreaChart(
    data: List<FinancialSnapshot>,
    modifier: Modifier = Modifier,
    primaryColor: Color = EmeraldAccent,
    secondaryColor: Color = GoldPrimary
) {
    if (data.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "area_anim"
    )

    val maxVal = (data.maxOfOrNull { it.cashReserves } ?: 150f).coerceAtLeast(100f) * 1.15f
    val minVal = (data.minOfOrNull { it.cashReserves } ?: 0f).coerceAtLeast(0f) * 0.85f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("recharts_area_chart")
    ) {
        // Chart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CASH RESERVES TREND OVER TIME",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                val currentSnapshot = selectedIndex?.let { data.getOrNull(it) } ?: data.last()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\$${String.format("%.1f", currentSnapshot.cashReserves)}M",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val diff = currentSnapshot.netCashFlow
                    val isPositive = diff >= 0
                    Surface(
                        color = (if (isPositive) EmeraldAccent else CrimsonAlert).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isPositive) EmeraldAccent else CrimsonAlert,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}\$${String.format("%.1f", diff)}M/wk",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositive) EmeraldAccent else CrimsonAlert
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = selectedIndex?.let { "Week ${data[it].weekNumber} Selected" } ?: "Drag to Scrub",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas Area Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(data) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val itemWidth = size.width / (data.size - 1).coerceAtLeast(1)
                            val idx = (offset.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = idx
                        },
                        onDrag = { change, _ ->
                            val itemWidth = size.width / (data.size - 1).coerceAtLeast(1)
                            val idx = (change.position.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = idx
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(data) {
                    detectTapGestures(
                        onTap = { offset ->
                            val itemWidth = size.width / (data.size - 1).coerceAtLeast(1)
                            val idx = (offset.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = if (selectedIndex == idx) null else idx
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingBottom = 20.dp.toPx()
                val graphHeight = height - paddingBottom
                val count = data.size

                // Grid lines (4 horizontal lines)
                val gridLineColor = Color(0xFF334155).copy(alpha = 0.4f)
                val steps = 3
                for (i in 0..steps) {
                    val y = (graphHeight / steps) * i
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }

                if (count < 2) return@Canvas

                val stepX = width / (count - 1)
                val points = data.mapIndexed { index, snapshot ->
                    val normY = (snapshot.cashReserves - minVal) / (maxVal - minVal).coerceAtLeast(1f)
                    val x = index * stepX
                    val y = graphHeight - (normY * graphHeight * animatedProgress)
                    Offset(x, y)
                }

                // Smooth Spline Path
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }

                // Under-fill gradient path
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x, graphHeight)
                    lineTo(points.first().x, graphHeight)
                    close()
                }

                // Draw Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.45f),
                            primaryColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = graphHeight
                    )
                )

                // Draw Smooth Line
                drawPath(
                    path = strokePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw Data Points
                points.forEachIndexed { i, pt ->
                    val isHovered = selectedIndex == i
                    drawCircle(
                        color = if (isHovered) Color.White else primaryColor,
                        radius = if (isHovered) 6.dp.toPx() else 3.5.dp.toPx(),
                        center = pt
                    )
                    if (isHovered) {
                        drawCircle(
                            color = primaryColor,
                            radius = 9.dp.toPx(),
                            center = pt,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        // Vertical guideline
                        drawLine(
                            color = primaryColor.copy(alpha = 0.6f),
                            start = Offset(pt.x, 0f),
                            end = Offset(pt.x, graphHeight),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    }
                }
            }
        }

        // X-Axis Period Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEachIndexed { idx, snapshot ->
                // Show every 2nd or 3rd label if many items
                val shouldShow = data.size <= 8 || idx == 0 || idx == data.size - 1 || idx % (data.size / 4).coerceAtLeast(1) == 0
                if (shouldShow) {
                    Text(
                        text = snapshot.periodLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedIndex == idx) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedIndex == idx) FontWeight.Black else FontWeight.Normal,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Interactive Tooltip Card (if scrubbed)
        if (selectedIndex != null) {
            val item = data[selectedIndex!!]
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${item.periodLabel} Breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = "Revenue: +\$${String.format("%.1f", item.totalRevenue)}M • Expense: -\$${String.format("%.1f", item.totalExpenses)}M",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Net: ${if (item.netCashFlow >= 0) "+" else ""}\$${String.format("%.1f", item.netCashFlow)}M",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = if (item.netCashFlow >= 0) EmeraldAccent else CrimsonAlert
                    )
                }
            }
        }
    }
}

/**
 * Recharts-style Grouped Bar Chart comparing Revenue vs Operating Expenses across periods.
 */
@Composable
fun RechartsBarChart(
    data: List<FinancialSnapshot>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = (data.maxOfOrNull { maxOf(it.totalRevenue, it.totalExpenses) } ?: 50f) * 1.2f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("recharts_bar_chart")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "REVENUE VS OPERATING EXPENSES",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldAccent,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Weekly Financial Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldAccent))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Revenue", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CrimsonAlert))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expense", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bar Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(data) {
                    detectTapGestures(
                        onTap = { offset ->
                            val slotWidth = size.width / data.size
                            val idx = (offset.x / slotWidth).toInt().coerceIn(0, data.size - 1)
                            selectedBarIndex = if (selectedBarIndex == idx) null else idx
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingBottom = 22.dp.toPx()
                val graphHeight = height - paddingBottom
                val slotWidth = width / data.size
                val barWidth = (slotWidth * 0.32f).coerceAtMost(16.dp.toPx())

                // Draw background grid lines
                val gridColor = Color(0xFF334155).copy(alpha = 0.35f)
                for (i in 0..3) {
                    val y = (graphHeight / 3) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                data.forEachIndexed { i, item ->
                    val isSelected = selectedBarIndex == i
                    val centerX = (i * slotWidth) + (slotWidth / 2f)

                    // Revenue Bar (Emerald)
                    val revHeight = (item.totalRevenue / maxVal) * graphHeight
                    val revTop = graphHeight - revHeight
                    val revX = centerX - barWidth - 2.dp.toPx()

                    drawRoundRect(
                        color = if (isSelected) EmeraldLight else EmeraldAccent,
                        topLeft = Offset(revX, revTop),
                        size = Size(barWidth, revHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Expense Bar (Crimson)
                    val expHeight = (item.totalExpenses / maxVal) * graphHeight
                    val expTop = graphHeight - expHeight
                    val expX = centerX + 2.dp.toPx()

                    drawRoundRect(
                        color = if (isSelected) CrimsonAlert else CrimsonAlert.copy(alpha = 0.85f),
                        topLeft = Offset(expX, expTop),
                        size = Size(barWidth, expHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    if (isSelected) {
                        drawRoundRect(
                            color = GoldPrimary,
                            topLeft = Offset(centerX - barWidth - 4.dp.toPx(), 0f),
                            size = Size((barWidth * 2) + 8.dp.toPx(), graphHeight),
                            style = Stroke(width = 1.5.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }
        }

        // X-Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEachIndexed { idx, snapshot ->
                if (data.size <= 8 || idx % 2 == 0) {
                    Text(
                        text = snapshot.periodLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedBarIndex == idx) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedBarIndex == idx) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Bar Details
        if (selectedBarIndex != null) {
            val item = data[selectedBarIndex!!]
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.periodLabel}: +\$${String.format("%.1f", item.totalRevenue)}M Rev | -\$${String.format("%.1f", item.totalExpenses)}M Exp",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${if (item.netCashFlow >= 0) "Profit +" else "Loss "}\$${String.format("%.1f", item.netCashFlow)}M",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = if (item.netCashFlow >= 0) EmeraldAccent else CrimsonAlert
                    )
                }
            }
        }
    }
}

data class SliceItem(
    val label: String,
    val value: Float,
    val color: Color
)

/**
 * Recharts-style Donut Breakdown Chart for Revenue Streams and Operating Cost distribution.
 */
@Composable
fun RechartsDonutChart(
    title: String,
    slices: List<SliceItem>,
    modifier: Modifier = Modifier,
    centerValue: String? = null,
    centerLabel: String? = null
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = GoldPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Canvas
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .pointerInput(slices) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = offset.x - center.x
                                    val dy = offset.y - center.y
                                    var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    var currentAngle = 0f
                                    slices.forEachIndexed { idx, slice ->
                                        val sweep = (slice.value / total) * 360f
                                        if (angle >= currentAngle && angle <= currentAngle + sweep) {
                                            selectedSliceIndex = if (selectedSliceIndex == idx) null else idx
                                            return@detectTapGestures
                                        }
                                        currentAngle += sweep
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 24.dp.toPx()
                        var startAngle = -90f

                        slices.forEachIndexed { idx, slice ->
                            val sweepAngle = (slice.value / total) * 360f
                            val isSelected = selectedSliceIndex == idx

                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle - 2f, // subtle gap
                                useCenter = false,
                                style = Stroke(
                                    width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Center text in cutout hole
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = centerValue ?: "\$${String.format("%.0f", total)}M",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = centerLabel ?: "TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Legend List
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slices.forEachIndexed { idx, slice ->
                        val isSelected = selectedSliceIndex == idx
                        val pct = ((slice.value / total) * 100).toInt()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) slice.color.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(vertical = 2.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "$pct% (\$${String.format("%.0f", slice.value)}M)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = slice.color
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-inspired Dual-Series Interactive Line Chart plotting League Financial Growth (%)
 * alongside Fan Sentiment (%) over simulated weeks and major executive milestones.
 */
@Composable
fun RechartsDualTrendLineChart(
    data: List<DashboardTrendPoint>,
    modifier: Modifier = Modifier,
    financeColor: Color = EmeraldAccent,
    sentimentColor: Color = CyanAccent
) {
    var showFinanceSeries by remember { mutableStateOf(true) }
    var showSentimentSeries by remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700),
        label = "dual_trend_line_anim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_growth_sentiment_line_chart"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(financeColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "League Financial Growth and Fan Sentiment Trend Chart",
                            tint = financeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "GROWTH & SENTIMENT TRENDS",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Valuation Growth vs Fan Sentiment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (data.isNotEmpty()) {
                    val activePoint = selectedIndex?.let { data.getOrNull(it) } ?: data.last()
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = activePoint.periodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Simulate a week to begin plotting league trends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@Column
            }

            val activePoint = selectedIndex?.let { data.getOrNull(it) } ?: data.last()
            val growthPositive = activePoint.financialGrowthPct >= 0f
            val growthSign = if (growthPositive) "+" else ""
            val activeFinanceColor = if (growthPositive) financeColor else CrimsonAlert

            // Headline KPI readout + interactive series toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val next = !showFinanceSeries
                            if (next || showSentimentSeries) showFinanceSeries = next
                        }
                        .testTag("trend_toggle_finance"),
                    color = if (showFinanceSeries) {
                        activeFinanceColor.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (showFinanceSeries) activeFinanceColor.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (showFinanceSeries) activeFinanceColor else TextMuted)
                        )
                        Column {
                            Text(
                                text = "Financial Growth",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$growthSign${String.format("%.1f", activePoint.financialGrowthPct)}% • \$${activePoint.leagueValuationMillions}M",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = if (showFinanceSeries) activeFinanceColor else TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("trend_growth_value")
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val next = !showSentimentSeries
                            if (next || showFinanceSeries) showSentimentSeries = next
                        }
                        .testTag("trend_toggle_sentiment"),
                    color = if (showSentimentSeries) {
                        sentimentColor.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (showSentimentSeries) sentimentColor.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (showSentimentSeries) sentimentColor else TextMuted)
                        )
                        Column {
                            Text(
                                text = "Fan Sentiment",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activePoint.fanSentimentPct}% • ${activePoint.totalFanbaseMillions}M",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = if (showSentimentSeries) sentimentColor else TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("trend_sentiment_value")
                            )
                        }
                    }
                }
            }

            // Dual Line Canvas
            val rawMinGrowth = data.minOf { it.financialGrowthPct }
            val rawMaxGrowth = data.maxOf { it.financialGrowthPct }
            val growthPadding = ((rawMaxGrowth - rawMinGrowth) * 0.18f).coerceAtLeast(5f)
            val minGrowth = rawMinGrowth - growthPadding
            val maxGrowth = rawMaxGrowth + growthPadding

            val rawMinSent = data.minOf { it.fanSentimentPct }.toFloat()
            val rawMaxSent = data.maxOf { it.fanSentimentPct }.toFloat()
            val minSentiment = (rawMinSent - 10f).coerceAtLeast(0f)
            val maxSentiment = (rawMaxSent + 10f).coerceAtMost(100f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
                    .testTag("trend_line_canvas")
                    .pointerInput(data) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val itemWidth = size.width / (data.size - 1).coerceAtLeast(1)
                                selectedIndex = (offset.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                            },
                            onDrag = { change, _ ->
                                val itemWidth = size.width / (data.size - 1).coerceAtLeast(1)
                                selectedIndex = (change.position.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                            },
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null }
                        )
                    }
                    .pointerInput(data) {
                        detectTapGestures(
                            onTap = { offset ->
                                val itemWidth = size.width / (data.size - 1).coerceAtLeast(1)
                                val idx = (offset.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                                selectedIndex = if (selectedIndex == idx) null else idx
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val paddingVertical = 10.dp.toPx()
                    val graphHeight = (height - (paddingVertical * 2f)).coerceAtLeast(1f)
                    val count = data.size

                    // Horizontal dashed gridlines
                    val gridLineColor = Color(0xFF334155).copy(alpha = 0.45f)
                    for (i in 0..3) {
                        val y = paddingVertical + (graphHeight / 3f) * i
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    }

                    if (count == 1) {
                        val cx = width / 2f
                        val cy = paddingVertical + (graphHeight / 2f)
                        if (showFinanceSeries) {
                            drawCircle(color = activeFinanceColor, radius = 5.dp.toPx(), center = Offset(cx, cy))
                        }
                        if (showSentimentSeries) {
                            drawCircle(color = sentimentColor, radius = 5.dp.toPx(), center = Offset(cx, cy))
                        }
                        return@Canvas
                    }

                    val stepX = width / (count - 1).toFloat()

                    val financePoints = data.mapIndexed { idx, pt ->
                        val normY = ((pt.financialGrowthPct - minGrowth) / (maxGrowth - minGrowth).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val x = idx * stepX
                        val y = paddingVertical + graphHeight - (normY * graphHeight * animatedProgress)
                        Offset(x, y)
                    }

                    val sentimentPoints = data.mapIndexed { idx, pt ->
                        val normY = ((pt.fanSentimentPct - minSentiment) / (maxSentiment - minSentiment).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val x = idx * stepX
                        val y = paddingVertical + graphHeight - (normY * graphHeight * animatedProgress)
                        Offset(x, y)
                    }

                    fun buildSmoothPath(pts: List<Offset>): Path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 0 until pts.size - 1) {
                            val p0 = pts[i]
                            val p1 = pts[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    if (showFinanceSeries) {
                        val strokePath = buildSmoothPath(financePoints)
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(financePoints.last().x, paddingVertical + graphHeight)
                            lineTo(financePoints.first().x, paddingVertical + graphHeight)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    activeFinanceColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                startY = paddingVertical,
                                endY = paddingVertical + graphHeight
                            )
                        )
                        drawPath(
                            path = strokePath,
                            color = activeFinanceColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        financePoints.forEachIndexed { idx, pt ->
                            val isSelected = selectedIndex == idx
                            drawCircle(
                                color = if (isSelected) Color.White else activeFinanceColor,
                                radius = if (isSelected) 5.5.dp.toPx() else 3.dp.toPx(),
                                center = pt
                            )
                        }
                    }

                    if (showSentimentSeries) {
                        val sentStrokePath = buildSmoothPath(sentimentPoints)
                        drawPath(
                            path = sentStrokePath,
                            color = sentimentColor,
                            style = Stroke(width = 2.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        sentimentPoints.forEachIndexed { idx, pt ->
                            val isSelected = selectedIndex == idx
                            drawCircle(
                                color = if (isSelected) Color.White else sentimentColor,
                                radius = if (isSelected) 5.5.dp.toPx() else 3.dp.toPx(),
                                center = pt
                            )
                        }
                    }

                    val activeCursorIdx = selectedIndex
                    if (activeCursorIdx != null && activeCursorIdx in financePoints.indices) {
                        val cursorX = financePoints[activeCursorIdx].x
                        drawLine(
                            color = GoldPrimary.copy(alpha = 0.7f),
                            start = Offset(cursorX, paddingVertical),
                            end = Offset(cursorX, paddingVertical + graphHeight),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    }
                }
            }

            // X-Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEachIndexed { idx, point ->
                    val shouldShow = data.size <= 7 || idx == 0 || idx == data.size - 1 || idx % (data.size / 4).coerceAtLeast(1) == 0
                    if (shouldShow) {
                        Text(
                            text = point.periodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedIndex == idx) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selectedIndex == idx) FontWeight.Black else FontWeight.Normal,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Summary / Scrubbed Breakdown Footer
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .testTag("trend_scrub_details")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activePoint.periodLabel} • Cash: \$${activePoint.cashReservesMillions}M",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Morale ${activePoint.avgMoralePct}% • Loyalty ${activePoint.avgLoyaltyPct}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = sentimentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

