package com.example.ui.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ProductionStudy
import com.example.ui.signature.SignatureHelper
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfExporter {

    fun generateAndSharePdf(context: Context, study: ProductionStudy) {
        try {
            val pdfDocument = PdfDocument()
            // Standard A4 dimensions in PDF points (72 points/inch) are 595 x 842.
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Perfect Coordinate Scaler: Keep our high-resolution 1200 x 1720 coordinate logic intact, 
            // but map it exactly to standard 595 x 842 A4 pages.
            canvas.scale(595f / 1200f, 842f / 1720f)

            val borderPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            val doubleBorderPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }

            val thinGridPaint = Paint().apply {
                color = Color.rgb(180, 180, 180)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val fillHeaderPaint = Paint().apply {
                color = Color.rgb(240, 244, 248) // Clean light blue-gray
                style = Paint.Style.FILL
            }

            val fillEffectiveHeaderPaint = Paint().apply {
                color = Color.rgb(232, 245, 233) // Gentle pastel green for effective activities
                style = Paint.Style.FILL
            }

            val fillNonEffectiveHeaderPaint = Paint().apply {
                color = Color.rgb(255, 235, 235) // Gentle pastel red for non-effective activities
                style = Paint.Style.FILL
            }

            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val boldTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            // Green style mappings (Effective/Productive)
            val greenTextPaint = Paint().apply {
                color = Color.rgb(46, 125, 50)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val greenBoldTextPaint = Paint().apply {
                color = Color.rgb(46, 125, 50)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            // Red style mappings (Non-effective/Non-productive)
            val redTextPaint = Paint().apply {
                color = Color.rgb(198, 40, 40)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val redBoldTextPaint = Paint().apply {
                color = Color.rgb(198, 40, 40)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val mainHeaderPaint = Paint().apply {
                color = Color.rgb(20, 50, 90) // Industrial Navy Blue
                textSize = 28f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val subHeaderPaint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            // Outline margins
            val leftMargin = 50f
            val rightMargin = 1150f
            val topMargin = 50f
            val bottomMargin = 1710f

            // Draw Double outer border
            canvas.drawRect(leftMargin, topMargin, rightMargin, bottomMargin, doubleBorderPaint)
            canvas.drawRect(leftMargin + 4f, topMargin + 4f, rightMargin - 4f, bottomMargin - 4f, borderPaint)

            // 1. HEADER SECTION
            var currentY = topMargin + 30f
            val factoryTitle = study.factoryName.ifEmpty { "TMS FASHIONS LTD" }.uppercase(Locale.US)
            canvas.drawText(factoryTitle, 600f, currentY, mainHeaderPaint)
            currentY += 25f
            canvas.drawText("IE DEPARTMENT", 600f, currentY, subHeaderPaint)
            currentY += 25f
            
            // Draw a styled title block
            canvas.drawText("Production Study Sheet", 600f, currentY, Paint(boldTextPaint).apply { 
                textSize = 22f 
                textAlign = Paint.Align.CENTER 
                color = Color.rgb(20, 50, 90)
                isUnderlineText = true
            })
            
            currentY += 35f

            // 2. METADATA SECTION (4 Rows of height 40f)
            val metaYStart = currentY
            val rowHeight = 40f
            val metaCols = floatArrayOf(50f, 160f, 330f, 520f, 760f, 880f, 1150f)

            // Draw bounding rectangles for rows
            for (i in 0 until 4) {
                val y1 = metaYStart + i * rowHeight
                val y2 = y1 + rowHeight
                canvas.drawRect(metaCols[0], y1, metaCols[6], y2, borderPaint)
            }
            // Vertical split lines
            for (col in metaCols) {
                canvas.drawLine(col, metaYStart, col, metaYStart + 4 * rowHeight, borderPaint)
            }

            // Row 1 values
            canvas.drawText("UNIT", metaCols[0] + 12f, metaYStart + 26f, boldTextPaint)
            canvas.drawText(study.unit, metaCols[1] + 12f, metaYStart + 26f, textPaint)
            canvas.drawText("Buyer", metaCols[2] + 12f, metaYStart + 26f, boldTextPaint)
            canvas.drawText(study.buyer, metaCols[3] + 12f, metaYStart + 26f, textPaint)
            canvas.drawText("Date", metaCols[4] + 12f, metaYStart + 26f, boldTextPaint)
            canvas.drawText(study.date, metaCols[5] + 12f, metaYStart + 26f, textPaint)

            // Row 2 values
            val r2Y = metaYStart + rowHeight
            canvas.drawText("Line", metaCols[0] + 12f, r2Y + 26f, boldTextPaint)
            canvas.drawText(study.lineNo, metaCols[1] + 12f, r2Y + 26f, textPaint)
            canvas.drawText("Style", metaCols[2] + 12f, r2Y + 26f, boldTextPaint)
            canvas.drawText(study.style, metaCols[3] + 12f, r2Y + 26f, textPaint)
            canvas.drawText("Start time", metaCols[4] + 12f, r2Y + 26f, boldTextPaint)
            canvas.drawText(study.startTime, metaCols[5] + 12f, r2Y + 26f, textPaint)

            // Row 3 values
            val r3Y = metaYStart + 2 * rowHeight
            canvas.drawText("Operation", metaCols[0] + 12f, r3Y + 26f, boldTextPaint)
            canvas.drawText(study.operationName, metaCols[1] + 12f, r3Y + 26f, textPaint)
            canvas.drawText("Study by", metaCols[2] + 12f, r3Y + 26f, boldTextPaint)
            canvas.drawText(study.studyBy, metaCols[3] + 12f, r3Y + 26f, textPaint)
            canvas.drawText("End time", metaCols[4] + 12f, r3Y + 26f, boldTextPaint)
            canvas.drawText(study.endTime, metaCols[5] + 12f, r3Y + 26f, textPaint)

            // Row 4 values
            val r4Y = metaYStart + 3 * rowHeight
            canvas.drawText("SMV", metaCols[0] + 12f, r4Y + 26f, boldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", study.smv), metaCols[1] + 12f, r4Y + 26f, textPaint)
            canvas.drawText("Worker no/name.", metaCols[2] + 12f, r4Y + 26f, boldTextPaint)
            canvas.drawText(study.operatorName, metaCols[3] + 12f, r4Y + 26f, textPaint)
            canvas.drawText("Total time", metaCols[4] + 12f, r4Y + 26f, boldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.1f mins", study.totalStudyTime), metaCols[5] + 12f, r4Y + 26f, textPaint)

            currentY += 4 * rowHeight + 30f

            // 3. CYCLES GRID SECTION
            val gridYStart = currentY
            val gridRowHeight = 30f
            val gridCols = FloatArray(1 + 10 + 2 + 7 + 1) // 21 boundary values
            
            gridCols[0] = 50f
            gridCols[1] = 95f
            for (i in 1..10) {
                gridCols[1 + i] = gridCols[i] + 48f
            }
            gridCols[12] = gridCols[11] + 60f
            gridCols[13] = gridCols[12] + 60f
            for (i in 1..7) {
                gridCols[13 + i] = gridCols[12 + i] + 65f
            }

            // Draw Header blocks
            val hY1 = gridYStart
            val hY2 = gridYStart + 35f
            val hY3 = gridYStart + 180f // Height for vertical headers increased to 145f
            val hY_cycle_bottom = hY3 - 35f

            // Group-specific background fills
            canvas.drawRect(gridCols[0], hY1, gridCols[1], hY3, fillHeaderPaint)
            canvas.drawRect(gridCols[1], hY1, gridCols[13], hY3, fillEffectiveHeaderPaint)
            canvas.drawRect(gridCols[13], hY1, gridCols[20], hY3, fillNonEffectiveHeaderPaint)

            // Draw bounding lines for headers
            canvas.drawRect(gridCols[0], hY1, gridCols[20], hY3, borderPaint)
            canvas.drawLine(gridCols[0], hY2, gridCols[20], hY2, borderPaint)
            canvas.drawLine(gridCols[1], hY_cycle_bottom, gridCols[11], hY_cycle_bottom, borderPaint)
            canvas.drawLine(gridCols[6], hY1, gridCols[6], hY2, borderPaint)

            // Group annotations
            val prevBestLabelPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("PREVIOUS BEST ACHIEVED PCS/HR", (gridCols[1] + gridCols[6]) / 2f, hY1 + 22f, prevBestLabelPaint)
            canvas.drawText("EFFECTIVE", (gridCols[11] + gridCols[13]) / 2f, hY1 + 22f, Paint(greenBoldTextPaint).apply { textAlign = Paint.Align.CENTER; textSize = 11f })
            canvas.drawText("NON EFFECTIVE ACTIVITIES", (gridCols[13] + gridCols[20]) / 2f, hY1 + 22f, Paint(redBoldTextPaint).apply { textAlign = Paint.Align.CENTER; textSize = 11f })

            // Draw vertical labels for column headings
            val greenLabelPaint = Paint().apply {
                color = Color.rgb(46, 125, 50)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val redLabelPaint = Paint().apply {
                color = Color.rgb(198, 40, 40)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            // Column Indexes 1..10 under Cycle Time
            canvas.drawText("Cycle Time", (gridCols[1] + gridCols[11]) / 2f, hY2 + 65f, Paint(greenBoldTextPaint).apply { textSize = 14f; textAlign = Paint.Align.CENTER })
            for (col in 1..10) {
                val cx = (gridCols[col] + gridCols[col+1]) / 2f
                canvas.drawText(col.toString(), cx, hY_cycle_bottom + 23f, Paint(greenLabelPaint).apply { textAlign = Paint.Align.CENTER })
            }

            // Effective labels (Bundle handling, Bobbin change)
            drawRotatedText(canvas, "Bundle handling", (gridCols[11] + gridCols[12]) / 2f, hY3 - 15f, -90f, greenLabelPaint)
            drawRotatedText(canvas, "Bobbin change", (gridCols[12] + gridCols[13]) / 2f, hY3 - 15f, -90f, greenLabelPaint)

            // Non-effective labels to match the template precisely
            val nonEffLabels = listOf("Thread breakage", "Needle breakage", "Machine Break down", "Waiting for work", "Rework", "Personal & Fatigue", "Others")
            for (i in nonEffLabels.indices) {
                val cx = (gridCols[13 + i] + gridCols[14 + i]) / 2f
                drawRotatedText(canvas, nonEffLabels[i], cx, hY3 - 15f, -90f, redLabelPaint)
            }

            // Vertical column dividers with precise heights to avoid cutting through header text
            for (i in gridCols.indices) {
                val col = gridCols[i]
                val startY = when (i) {
                    0, 1, 11, 13, 20 -> hY1
                    12, 14, 15, 16, 17, 18, 19 -> hY2
                    else -> hY_cycle_bottom
                }
                canvas.drawLine(col, startY, col, hY3 + 20 * gridRowHeight, borderPaint)
            }

            // Draw 20 rows of cells with green values for effective and red for non-effective
            val greenCellTextPaint = Paint().apply {
                color = Color.rgb(46, 125, 50)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val redCellTextPaint = Paint().apply {
                color = Color.rgb(198, 40, 40)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            for (row in 0 until 20) {
                val rY1 = hY3 + row * gridRowHeight
                val rY2 = rY1 + gridRowHeight
                
                // Draw horizontal row line
                canvas.drawLine(leftMargin, rY2, gridCols[20], rY2, borderPaint)

                // Row Number (Standard/neutral black)
                canvas.drawText((row + 1).toString(), (gridCols[0] + gridCols[1]) / 2f, rY1 + 20f, Paint(boldTextPaint).apply { textSize = 11f; textAlign = Paint.Align.CENTER })

                val rowData = if (row < study.rows.size) study.rows[row] else null
                
                if (rowData != null) {
                    // 10 cycles (Effective - GREEN)
                    for (c in 0 until 10) {
                        val valStr = if (c < rowData.cycleTimes.size && rowData.cycleTimes[c] > 0.0) {
                            String.format(Locale.US, "%.1f", rowData.cycleTimes[c])
                        } else ""
                        val cx = (gridCols[1 + c] + gridCols[2 + c]) / 2f
                        canvas.drawText(valStr, cx, rY1 + 20f, greenCellTextPaint)
                    }

                    // Effective handling & bobbin change (Effective - GREEN)
                    val bhStr = if (rowData.effectiveBundleHandling > 0.0) String.format(Locale.US, "%.1f", rowData.effectiveBundleHandling) else ""
                    canvas.drawText(bhStr, (gridCols[11] + gridCols[12]) / 2f, rY1 + 20f, greenCellTextPaint)

                    val bcStr = if (rowData.effectiveBobbinChange > 0.0) String.format(Locale.US, "%.1f", rowData.effectiveBobbinChange) else ""
                    canvas.drawText(bcStr, (gridCols[12] + gridCols[13]) / 2f, rY1 + 20f, greenCellTextPaint)

                    // Non effective (RED)
                    val nonEffVals = listOf(
                        rowData.nonEffectiveThreadBreakage, rowData.nonEffectiveNeedleBreakage,
                        rowData.nonEffectiveMachineBreakdown, rowData.nonEffectiveWaitingForWork,
                        rowData.nonEffectiveRework, rowData.nonEffectivePersonalFatigue,
                        rowData.nonEffectiveOthers
                    )
                    for (i in nonEffVals.indices) {
                        val neStr = if (nonEffVals[i] > 0.0) String.format(Locale.US, "%.1f", nonEffVals[i]) else ""
                        canvas.drawText(neStr, (gridCols[13 + i] + gridCols[14 + i]) / 2f, rY1 + 20f, redCellTextPaint)
                    }
                }
            }

            // Left divider and Right divider inside the grid section
            canvas.drawLine(leftMargin, hY3, leftMargin, hY3 + 20 * gridRowHeight, borderPaint)
            canvas.drawLine(gridCols[20], hY3, gridCols[20], hY3 + 20 * gridRowHeight, borderPaint)

            // Header for product flow totals
            val footerYStart = hY3 + 20 * gridRowHeight
            val footH = 30f

            // Color-coded backgrounds for total row headers
            canvas.drawRect(leftMargin, footerYStart, gridCols[11], footerYStart + footH, fillEffectiveHeaderPaint)
            canvas.drawRect(gridCols[11], footerYStart, gridCols[20], footerYStart + footH, fillNonEffectiveHeaderPaint)

            canvas.drawRect(leftMargin, footerYStart, gridCols[20], footerYStart + footH, borderPaint)

            canvas.drawText("PRODUCTIVE TIME", (leftMargin + gridCols[11]) / 2f, footerYStart + 20f, Paint(greenBoldTextPaint).apply { textSize = 11f; textAlign = Paint.Align.CENTER })
            canvas.drawText("NON PRODUCTIVE TIME", (gridCols[11] + gridCols[20]) / 2f, footerYStart + 20f, Paint(redBoldTextPaint).apply { textSize = 11f; textAlign = Paint.Align.CENTER })
            canvas.drawLine(gridCols[11], footerYStart, gridCols[11], footerYStart + footH, borderPaint)

            currentY = footerYStart + footH + 20f

            // 4. AUTOMATIC CALCULATIONS - 1100f Full-Width sequential layout
            val calcTableY = currentY
            val calcTableH = 185f
            val splitCol = (leftMargin + rightMargin) / 2f // 600f

            // Draw Calculations Outer Border and vertical split line
            canvas.drawRect(leftMargin, calcTableY, rightMargin, calcTableY + calcTableH, borderPaint)
            canvas.drawLine(splitCol, calcTableY, splitCol, calcTableY + calcTableH, borderPaint)

            // Math calculations
            val cycleTimesSum = study.rows.flatMap { it.cycleTimes }.filter { it > 0.0 }.sum()
            val totalProduced = study.calculatedTotalProducedPcs
            val previousBest = study.previousBestAchieved
            // Target Improvement = Total Production during study - Previous Best achieved
            val targetImprovement = totalProduced.toDouble() - previousBest
            val improvementPercent = if (totalProduced > 0) {
                (targetImprovement / totalProduced.toDouble()) * 100.0
            } else {
                0.0
            }
            val avgCapacityTime = if (totalProduced > 0) cycleTimesSum / totalProduced else 0.0

            val totalEffective = study.rows.sumOf { it.effectiveBundleHandling + it.effectiveBobbinChange }
            val totalNonEffective = study.rows.sumOf {
                it.nonEffectiveThreadBreakage + it.nonEffectiveNeedleBreakage +
                it.nonEffectiveMachineBreakdown + it.nonEffectiveWaitingForWork +
                it.nonEffectiveRework + it.nonEffectivePersonalFatigue +
                it.nonEffectiveOthers
            }
            val totalNonProductiveTime = totalEffective + totalNonEffective
            val totalObserveTime = cycleTimesSum + totalNonProductiveTime
            
            val studyTimeSecs = study.totalStudyTime * 60.0
            val errorPercent = if (studyTimeSecs > 0.0) ((studyTimeSecs - totalObserveTime) / studyTimeSecs) * 100.0 else 0.0

            // Column 1: Performance Metrics (GREEN)
            var localY = calcTableY + 23f
            val lineOffset = 27f
            val labelX = leftMargin + 15f
            val valX = splitCol - 15f
            val greenRightAlignPaint = Paint(greenTextPaint).apply { textAlign = Paint.Align.RIGHT }

            canvas.drawText("Total Produced PCS During Study", labelX, localY, greenBoldTextPaint)
            canvas.drawText("$totalProduced pcs", valX, localY, greenRightAlignPaint)
            
            localY += lineOffset
            canvas.drawText("Previous Best Achieved", labelX, localY, greenBoldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.1f pcs/Hr", previousBest), valX, localY, greenRightAlignPaint)

            localY += lineOffset
            canvas.drawText("Target Improvement", labelX, localY, greenBoldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.1f pcs", targetImprovement), valX, localY, greenRightAlignPaint)

            localY += lineOffset
            canvas.drawText("Improvement %", labelX, localY, greenBoldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.2f %%", improvementPercent), valX, localY, greenRightAlignPaint)

            localY += lineOffset
            canvas.drawText("Avg. Capacity Time", labelX, localY, greenBoldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.1f secs", avgCapacityTime), valX, localY, greenRightAlignPaint)

            localY += lineOffset
            canvas.drawText("Capacity Target", labelX, localY, greenBoldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.1f pcs/Hr", study.calculatedCapacityTarget), valX, localY, greenRightAlignPaint)

            // Column 2: Observations and Error (Putting Observe and Study times above Error %)
            localY = calcTableY + 23f
            val label2X = splitCol + 15f
            val val2X = rightMargin - 15f
            val navyBoldTextPaint = Paint(boldTextPaint).apply { color = Color.rgb(20, 50, 90) }
            val navyTextPaint = Paint(textPaint).apply { color = Color.rgb(20, 50, 90) }
            val navyRightAlignPaint = Paint(navyTextPaint).apply { textAlign = Paint.Align.RIGHT }
            val redRightAlignPaint = Paint(redTextPaint).apply { textAlign = Paint.Align.RIGHT }

            canvas.drawText("Total Productive Time", label2X, localY, greenBoldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.1f s", cycleTimesSum), val2X, localY, greenRightAlignPaint)

            localY += lineOffset
            canvas.drawText("Total Non Productive Time", label2X, localY, redBoldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.1f s", totalNonProductiveTime), val2X, localY, redRightAlignPaint)

            localY += lineOffset
            canvas.drawText("Total Observe Time", label2X, localY, navyBoldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.1f s", totalObserveTime), val2X, localY, navyRightAlignPaint)

            localY += lineOffset
            canvas.drawText("Total Study Time (mins)", label2X, localY, navyBoldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.1f s (%.1f m)", studyTimeSecs, study.totalStudyTime), val2X, localY, navyRightAlignPaint)

            localY += lineOffset
            canvas.drawText("Error %", label2X, localY, redBoldTextPaint)
            canvas.drawText(String.format(Locale.US, "%.2f %%", errorPercent), val2X, localY, redRightAlignPaint)


            // 5. HOURLY PRODUCTION AFTER STUDY SECTION - Full width table below metrics
            val hourlyYStart = calcTableY + calcTableH + 20f
            
            // Draw general Section Heading before the table
            canvas.drawText("Hourly Production After Study (8 Hours)", leftMargin, hourlyYStart + 15f, Paint(boldTextPaint).apply { 
                textSize = 13f 
                color = Color.rgb(20, 50, 90)
            })

            val tableY = hourlyYStart + 23f
            canvas.drawRect(leftMargin, tableY, rightMargin, tableY + 55f, borderPaint)
            
            // Background for hourly production header
            canvas.drawRect(leftMargin, tableY, rightMargin, tableY + 22f, fillHeaderPaint)
            canvas.drawLine(leftMargin, tableY + 22f, rightMargin, tableY + 22f, borderPaint)

            // Draw 8 hour blocks
            val divWidth = 1100f / 8f
            for (i in 0..8) {
                val cx = leftMargin + i * divWidth
                canvas.drawLine(cx, tableY, cx, tableY + 55f, borderPaint)
            }
            
            for (i in 0 until 8) {
                val cellLeft = leftMargin + i * divWidth
                val cx = cellLeft + divWidth / 2f
                
                // Draw Hours headings in Row 1
                canvas.drawText("Hour ${i + 1}", cx, tableY + 15f, Paint(boldTextPaint).apply { textSize = 9.5f; textAlign = Paint.Align.CENTER })
                
                // Draw Hour counts in Row 2
                val countVal = if (i < study.hourlyProduction.size) study.hourlyProduction[i] else 0
                val countStr = if (countVal > 0) countVal.toString() else "-"
                canvas.drawText(countStr, cx, tableY + 41f, Paint(textPaint).apply { textAlign = Paint.Align.CENTER })
            }


            // 6. REMARKS SECTION - Full width box below Hourly Production
            val remarksYStart = tableY + 55f + 20f
            canvas.drawRect(leftMargin, remarksYStart, rightMargin, remarksYStart + 50f, borderPaint)
            canvas.drawRect(leftMargin, remarksYStart, leftMargin + 100f, remarksYStart + 50f, fillHeaderPaint)
            canvas.drawLine(leftMargin + 100f, remarksYStart, leftMargin + 100f, remarksYStart + 50f, borderPaint)
            canvas.drawText("Remarks", leftMargin + 50f, remarksYStart + 31f, Paint(boldTextPaint).apply { 
                textSize = 12f
                textAlign = Paint.Align.CENTER
                color = Color.rgb(20, 50, 90)
            })
            canvas.drawText(study.remarks.ifEmpty { "N/A" }, leftMargin + 115f, remarksYStart + 31f, Paint(textPaint).apply { 
                textSize = 12f 
            })


            // 7. SIGNATURES SECTION - Side-by-side row at the very bottom
            val sigYStart = remarksYStart + 50f + 20f
            val sigBoxH = 90f
            val colW = 1100f / 6f // ~ 183.33f
            
            canvas.drawRect(leftMargin, sigYStart, rightMargin, sigYStart + sigBoxH, borderPaint)
            
            // Draw vertical grid dividers for 6 columns
            for (i in 1..5) {
                val cx = leftMargin + i * colW
                canvas.drawLine(cx, sigYStart, cx, sigYStart + sigBoxH, borderPaint)
            }

            val sigPaints = Paint().apply { isFilterBitmap = true }
            
            // Draw each signature cell inside its respective 183.33f slot
            drawSignatureCell(canvas, "Worker Signature", study.workerSignature, leftMargin + 4f, sigYStart + 2f, colW - 8f, sigBoxH - 4f, boldTextPaint, sigPaints)
            drawSignatureCell(canvas, "Line Supervisor", study.lineSupervisorSignature, leftMargin + colW + 4f, sigYStart + 2f, colW - 8f, sigBoxH - 4f, boldTextPaint, sigPaints)
            drawSignatureCell(canvas, "Line Chief", study.lineChiefSignature, leftMargin + 2 * colW + 4f, sigYStart + 2f, colW - 8f, sigBoxH - 4f, boldTextPaint, sigPaints)
            drawSignatureCell(canvas, "APM / Floor Incharge", study.apmFloorInchargeSignature, leftMargin + 3 * colW + 4f, sigYStart + 2f, colW - 8f, sigBoxH - 4f, boldTextPaint, sigPaints)
            drawSignatureCell(canvas, "Production Manager", study.productionMgrSignature, leftMargin + 4 * colW + 4f, sigYStart + 2f, colW - 8f, sigBoxH - 4f, boldTextPaint, sigPaints)
            drawSignatureCell(canvas, "IE Executive", study.ieExecutiveSignature, leftMargin + 5 * colW + 4f, sigYStart + 2f, colW - 8f, sigBoxH - 4f, boldTextPaint, sigPaints)

            pdfDocument.finishPage(page)

            // Save document to internal cache directory
            val cachePath = File(context.cacheDir, "pdf")
            cachePath.mkdirs()
            val file = File(cachePath, "Production_Study_${study.id}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Open share chooser
            val uri = FileProvider.getUriForFile(context, "com.aistudio.productionstudy.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Production Study: ${study.buyer} - ${study.style}")
                putExtra(Intent.EXTRA_TEXT, "Attached is the Production Study Sheet PDF for ${study.buyer} Style: ${study.style}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Production Study PDF via:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error generating PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawRotatedText(canvas: Canvas, text: String, x: Float, y: Float, angle: Float, paint: Paint) {
        canvas.save()
        canvas.rotate(angle, x, y)
        canvas.drawText(text, x, y, paint)
        canvas.restore()
    }

    private fun drawSignatureCell(canvas: Canvas, title: String, sigBase64: String, x: Float, y: Float, w: Float, h: Float, titlePaint: Paint, sigPaint: Paint) {
        val destPaint = Paint(titlePaint).apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            color = Color.rgb(30, 30, 30)
        }
        
        // Draw the designation title at the bottom of the cell, centered
        canvas.drawText(title, x + w / 2f, y + h - 8f, destPaint)
        
        // Draw the signature line above the title text
        val lineY = y + h - 25f
        val linePaint = Paint().apply {
            color = Color.rgb(180, 180, 180)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(x + 15f, lineY, x + w - 15f, lineY, linePaint)
        
        // Draw the bitmap if present, centered in the upper space
        if (sigBase64.isNotEmpty()) {
            val bitmap = SignatureHelper.decodeBase64ToBitmap(sigBase64)
            if (bitmap != null) {
                // Calculate scale to fit signature inside bounds perfectly
                val maxWidth = w - 24f
                val maxHeight = h - 35f
                
                val scaleWidth = maxWidth / bitmap.width
                val scaleHeight = maxHeight / bitmap.height
                val scale = Math.min(scaleWidth, scaleHeight)
                
                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale
                
                val drawLeft = x + (w - drawW) / 2f
                val drawTop = y + 5f + (maxHeight - drawH) / 2f
                
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val targetRect = Rect(
                    drawLeft.toInt(),
                    drawTop.toInt(),
                    (drawLeft + drawW).toInt(),
                    (drawTop + drawH).toInt()
                )
                canvas.drawBitmap(bitmap, srcRect, targetRect, sigPaint)
            }
        }
    }
}
