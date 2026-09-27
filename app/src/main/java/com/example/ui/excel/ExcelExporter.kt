package com.example.ui.excel

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ProductionStudy
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

private fun XSSFCellStyle.setGrayBorders(borderColor: XSSFColor) {
    borderTop = BorderStyle.THIN
    borderBottom = BorderStyle.THIN
    borderLeft = BorderStyle.THIN
    borderRight = BorderStyle.THIN
    setTopBorderColor(borderColor)
    setBottomBorderColor(borderColor)
    setLeftBorderColor(borderColor)
    setRightBorderColor(borderColor)
}

private fun XSSFCellStyle.setFill(color: XSSFColor) {
    setFillForegroundColor(color)
    fillPattern = FillPatternType.SOLID_FOREGROUND
}

object ExcelExporter {

    private fun getColLetter(colIndex: Int): String {
        return if (colIndex < 26) {
            ('A' + colIndex).toString()
        } else {
            "A" + ('A' + (colIndex - 26)).toString()
        }
    }

    private fun insertSignature(
        workbook: XSSFWorkbook,
        sheet: Sheet,
        sigBase64: String,
        cStart: Int,
        cEnd: Int,
        row: Int
    ) {
        if (sigBase64.isEmpty()) return
        try {
            val cleanBase64 = if (sigBase64.contains(",")) {
                sigBase64.substringAfter(",")
            } else {
                sigBase64
            }
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG)
            val helper = workbook.creationHelper
            val drawing = sheet.createDrawingPatriarch()
            val anchor = helper.createClientAnchor().apply {
                setCol1(cStart)
                setRow1(row)
                setCol2(cEnd + 1)
                setRow2(row + 1)
            }
            drawing.createPicture(anchor, pictureIdx)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateAndShareExcel(context: Context, study: ProductionStudy) {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = ExcelExporter::class.java.classLoader
            System.setProperty("javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
            System.setProperty("javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
            System.setProperty("javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Production Study")
            sheet.isDisplayGridlines = true

            // Set A4 Print Margins (0.4 inches on sides to fit all content perfectly on A4)
            sheet.setMargin(Sheet.LeftMargin, 0.4)
            sheet.setMargin(Sheet.RightMargin, 0.4)
            sheet.setMargin(Sheet.TopMargin, 0.4)
            sheet.setMargin(Sheet.BottomMargin, 0.4)

            // Setup A4 Portrait Print Scaling to fit onto a single page (prevents spillover)
            val printSetup = sheet.printSetup
            printSetup.paperSize = PrintSetup.A4_PAPERSIZE
            printSetup.landscape = false
            sheet.fitToPage = true
            printSetup.fitWidth = 1
            printSetup.fitHeight = 1

            // Dynamic palette exactly matching PDF aesthetics
            val colorNavy = XSSFColor(byteArrayOf(20, 50, 90), null) // #14325A - Industrial Navy
            val colorHeaderGray = XSSFColor(byteArrayOf(240.toByte(), 244.toByte(), 248.toByte()), null) // #F0F4F8 - Light neutral gray-blue
            val colorEffectiveGreen = XSSFColor(byteArrayOf(232.toByte(), 245.toByte(), 233.toByte()), null) // #E8F5E9 - Pale green background
            val colorNonEffectiveRed = XSSFColor(byteArrayOf(255.toByte(), 235.toByte(), 235.toByte()), null) // #FFEBEB - Soft red background
            val colorBorderGray = XSSFColor(byteArrayOf(180.toByte(), 180.toByte(), 180.toByte()), null) // #B4B4B4 - Grid Border Gray

            // Custom Fonts
            val titleFont = (workbook.createFont() as XSSFFont).apply {
                fontName = "Arial"
                bold = true
                fontHeightInPoints = 16.toShort()
                setColor(colorNavy)
            }

            val subtitleFont = (workbook.createFont() as XSSFFont).apply {
                fontName = "Arial"
                bold = true
                fontHeightInPoints = 11.toShort()
                setColor(colorNavy)
            }

            val docNameFont = (workbook.createFont() as XSSFFont).apply {
                fontName = "Arial"
                bold = true
                fontHeightInPoints = 13.toShort()
                setColor(colorNavy)
                underline = Font.U_SINGLE
            }

            val boldFont = (workbook.createFont() as XSSFFont).apply {
                fontName = "Arial"
                bold = true
                fontHeightInPoints = 9.toShort()
                color = IndexedColors.BLACK.index
            }

            val normalFont = (workbook.createFont() as XSSFFont).apply {
                fontName = "Arial"
                fontHeightInPoints = 9.toShort()
                color = IndexedColors.BLACK.index
            }

            val greenFontVal = (workbook.createFont() as XSSFFont).apply {
                fontName = "Arial"
                bold = true
                fontHeightInPoints = 9.toShort()
                val greenRGB = XSSFColor(byteArrayOf(27.toByte(), 94.toByte(), 32.toByte()), null) // #1B5E20
                setColor(greenRGB)
            }

            val redFontVal = (workbook.createFont() as XSSFFont).apply {
                fontName = "Arial"
                bold = true
                fontHeightInPoints = 9.toShort()
                val redRGB = XSSFColor(byteArrayOf(198.toByte(), 40.toByte(), 40.toByte()), null) // #C62828
                setColor(redRGB)
            }

            // Cell Styles
            val titleStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFont(titleFont)
            }

            val subTitleStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFont(subtitleFont)
            }

            val docNameStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFont(docNameFont)
            }

            val boldLabelStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(boldFont)
                setFill(colorHeaderGray)
                alignment = HorizontalAlignment.LEFT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
            }

            val boldHeaderCenterStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(boldFont)
                setFill(colorHeaderGray)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
            }

            val metadataValueStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(normalFont)
                alignment = HorizontalAlignment.LEFT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
            }

            val gridBorderThin = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(normalFont)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
            }

            val snCellStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(boldFont)
                setFill(colorHeaderGray)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
            }

            val greenHeaderStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFill(colorEffectiveGreen)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFont(greenFontVal)
                setGrayBorders(colorBorderGray)
            }

            val redHeaderStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFill(colorNonEffectiveRed)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFont(redFontVal)
                setGrayBorders(colorBorderGray)
            }

            // Custom Rotated Vertical Text Styles for table column headings
            val greenVerticalStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFill(colorEffectiveGreen)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFont(greenFontVal)
                setGrayBorders(colorBorderGray)
                rotation = 90.toShort()
                wrapText = true
            }

            val redVerticalStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFill(colorNonEffectiveRed)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFont(redFontVal)
                setGrayBorders(colorBorderGray)
                rotation = 90.toShort()
                wrapText = true
            }

            val dataRegularStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(normalFont)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
            }

            // Active calculation value styles
            val formatter = workbook.createDataFormat()

            val styleGreenPcs = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(greenFontVal)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
                dataFormat = formatter.getFormat("0\" pcs\"")
            }

            val styleGreenPcsHr = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(greenFontVal)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
                dataFormat = formatter.getFormat("0.0\" pcs/Hr\"")
            }

            val styleGreenTargetImp = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(greenFontVal)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
                dataFormat = formatter.getFormat("0.0\" pcs\"")
            }

            val styleGreenPercent = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(greenFontVal)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
                dataFormat = formatter.getFormat("0.00%")
            }

            val styleGreenSeconds = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(greenFontVal)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
                dataFormat = formatter.getFormat("0.0\" secs\"")
            }

            // Right side card values
            val styleRightProductiveTime = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(greenFontVal)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
                dataFormat = formatter.getFormat("0.0\" s\"")
            }

            val styleRightNonProductiveTime = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(redFontVal)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
                dataFormat = formatter.getFormat("0.0\" s\"")
            }

            val styleRightObserveTime = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(boldFont)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
                dataFormat = formatter.getFormat("0.0\" s\"")
            }

            val styleRightStudyTime = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(boldFont)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
                dataFormat = formatter.getFormat("0.0\" s (${study.totalStudyTime} m)\"")
            }

            val styleRightErrorPercent = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(redFontVal)
                alignment = HorizontalAlignment.RIGHT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
                dataFormat = formatter.getFormat("0.00%")
            }

            val metricLabelStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(boldFont)
                alignment = HorizontalAlignment.LEFT
                verticalAlignment = VerticalAlignment.CENTER
                setGrayBorders(colorBorderGray)
            }

            // PRODUCTIVE / NON PRODUCTIVE custom bar fonts and styles
            val productiveTimeBarFont = (workbook.createFont() as XSSFFont).apply {
                fontName = "Arial"
                bold = true
                fontHeightInPoints = 10.toShort()
                val greenColor = XSSFColor(byteArrayOf(27.toByte(), 94.toByte(), 32.toByte()), null)
                setColor(greenColor)
            }

            val nonProductiveTimeBarFont = (workbook.createFont() as XSSFFont).apply {
                fontName = "Arial"
                bold = true
                fontHeightInPoints = 10.toShort()
                val redColor = XSSFColor(byteArrayOf(198.toByte(), 40.toByte(), 40.toByte()), null)
                setColor(redColor)
            }

            val productiveTimeBarStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFill(colorEffectiveGreen)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFont(productiveTimeBarFont)
                setGrayBorders(colorBorderGray)
            }

            val nonProductiveTimeBarStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFill(colorNonEffectiveRed)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFont(nonProductiveTimeBarFont)
                setGrayBorders(colorBorderGray)
            }


            // 1. Write Company Header Title (Row 0)
            val row0 = sheet.createRow(0)
            row0.heightInPoints = 26f
            val cell0 = row0.createCell(0)
            cell0.setCellValue(study.factoryName.ifEmpty { "FERDOUS FASHIONS LTD" }.uppercase(Locale.US))
            cell0.setCellStyle(titleStyle)
            sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 19))

            // Subtitle Department (Row 1)
            val row1 = sheet.createRow(1)
            row1.heightInPoints = 18f
            val cell1 = row1.createCell(0)
            cell1.setCellValue("IE DEPARTMENT")
            cell1.setCellStyle(subTitleStyle)
            sheet.addMergedRegion(CellRangeAddress(1, 1, 0, 19))

            // Document Title (Row 2)
            val row2 = sheet.createRow(2)
            row2.heightInPoints = 22f
            val cell2 = row2.createCell(0)
            cell2.setCellValue("Production Study Sheet")
            cell2.setCellStyle(docNameStyle)
            sheet.addMergedRegion(CellRangeAddress(2, 2, 0, 19))

            // Spacer (Row 3)
            sheet.createRow(3).heightInPoints = 8f


            // 2. Metadata Grid (Rows 4 to 7)
            val metadata = listOf(
                listOf("UNIT", study.unit, "Buyer", study.buyer, "Date", study.date),
                listOf("Line", study.lineNo, "Style", study.style, "Start time", study.startTime),
                listOf("Operation", study.operationName, "Study by", study.studyBy, "End time", study.endTime),
                listOf("SMV", String.format(Locale.US, "%.2f", study.smv), "Worker no/name.", study.operatorName, "Total time", "${study.totalStudyTime} mins")
            )

            for (i in metadata.indices) {
                val mRow = sheet.createRow(4 + i)
                mRow.heightInPoints = 20f
                val items = metadata[i]

                mRow.createCell(0).apply { setCellValue(items[0]); setCellStyle(boldLabelStyle) }
                mRow.createCell(1).apply { setCellValue(items[1]); setCellStyle(metadataValueStyle) }
                sheet.addMergedRegion(CellRangeAddress(4 + i, 4 + i, 1, 3))

                mRow.createCell(4).apply { setCellValue(items[2]); setCellStyle(boldLabelStyle) }
                mRow.createCell(5).apply { setCellValue(items[3]); setCellStyle(metadataValueStyle) }
                sheet.addMergedRegion(CellRangeAddress(4 + i, 4 + i, 5, 8))

                mRow.createCell(9).apply { setCellValue(items[4]); setCellStyle(boldLabelStyle) }
                mRow.createCell(10).apply { setCellValue(items[5]); setCellStyle(metadataValueStyle) }
                sheet.addMergedRegion(CellRangeAddress(4 + i, 4 + i, 10, 19))

                // Apply borders cleanly over all cells in merged region boundaries
                for (c in 0..19) {
                    val cell = mRow.getCell(c) ?: mRow.createCell(c)
                    if (c in 1..3 || c in 5..8 || c in 10..19) {
                        cell.setCellStyle(metadataValueStyle)
                    } else {
                        cell.setCellStyle(boldLabelStyle)
                    }
                }
            }

            // Spacer Row before table (Row 8)
            sheet.createRow(8).heightInPoints = 8f


            // 3. Main Multi-Level Table Headers (Rows 9 to 11)
            // Header Row 1 (Row 9): Previous best | EFFECTIVE | NON EFFECTIVE
            val hRow9 = sheet.createRow(9)
            hRow9.heightInPoints = 22f

            // S/N vertically merged from Row 9 to 11
            val snHeader = hRow9.createCell(0).apply { setCellValue("S/N"); setCellStyle(snCellStyle) }
            sheet.addMergedRegion(CellRangeAddress(9, 11, 0, 0))

            // Previous Best Achieved Label Row
            hRow9.createCell(1).apply { setCellValue("PREVIOUS BEST ACHIEVED PCS/HR"); setCellStyle(snCellStyle) }
            sheet.addMergedRegion(CellRangeAddress(9, 9, 1, 7))

            // Value of target previous best
            hRow9.createCell(8).apply { setCellValue(study.previousBestAchieved); setCellStyle(metadataValueStyle) }
            sheet.addMergedRegion(CellRangeAddress(9, 9, 8, 10))

            // Merge borders for the previous best labels & values
            for (c in 1..10) {
                val cell = hRow9.getCell(c) ?: hRow9.createCell(c)
                if (c <= 7) {
                    cell.setCellStyle(snCellStyle)
                } else {
                    cell.setCellStyle(metadataValueStyle)
                }
            }

            // Effective Section Header
            hRow9.createCell(11).apply { setCellValue("EFFECTIVE"); setCellStyle(greenHeaderStyle) }
            sheet.addMergedRegion(CellRangeAddress(9, 9, 11, 12))
            hRow9.createCell(12).setCellStyle(greenHeaderStyle)

            // Non-Effective Section Header
            hRow9.createCell(13).apply { setCellValue("NON EFFECTIVE ACTIVITIES"); setCellStyle(redHeaderStyle) }
            sheet.addMergedRegion(CellRangeAddress(9, 9, 13, 19))
            for (c in 13..19) {
                hRow9.getCell(c)?.setCellStyle(redHeaderStyle) ?: hRow9.createCell(c).setCellStyle(redHeaderStyle)
            }


            // Header Row 2 (Row 10): Cycle Time block & vertical labels
            val hRow10 = sheet.createRow(10)
            hRow10.heightInPoints = 85f // Plenty of vertical height for rotated labels
            hRow10.createCell(0).setCellStyle(snCellStyle) // bottom part of S/N merged region

            // Cycle Time Header
            hRow10.createCell(1).apply { setCellValue("Cycle Time"); setCellStyle(greenHeaderStyle) }
            sheet.addMergedRegion(CellRangeAddress(10, 10, 1, 10))
            for (c in 1..10) {
                hRow10.getCell(c)?.setCellStyle(greenHeaderStyle) ?: hRow10.createCell(c).setCellStyle(greenHeaderStyle)
            }

            // Effective columns merged vertically row 10 to 11 with rotated labels
            hRow10.createCell(11).apply { setCellValue("Bundle handling"); setCellStyle(greenVerticalStyle) }
            sheet.addMergedRegion(CellRangeAddress(10, 11, 11, 11))

            hRow10.createCell(12).apply { setCellValue("Bobbin change"); setCellStyle(greenVerticalStyle) }
            sheet.addMergedRegion(CellRangeAddress(10, 11, 12, 12))

            // Non-effective columns merged vertically row 10 to 11 with rotated labels
            val nonEffLabels = listOf(
                "Thread breakage", "Needle breakage", "Machine Break down",
                "Waiting for work", "Rework", "Personal & Fatigue", "Others"
            )
            for (i in nonEffLabels.indices) {
                val colIndex = 13 + i
                hRow10.createCell(colIndex).apply { setCellValue(nonEffLabels[i]); setCellStyle(redVerticalStyle) }
                sheet.addMergedRegion(CellRangeAddress(10, 11, colIndex, colIndex))
            }


            // Header Row 3 (Row 11): Cycle numbers (1 to 10)
            val hRow11 = sheet.createRow(11)
            hRow11.heightInPoints = 20f
            hRow11.createCell(0).setCellStyle(snCellStyle) // Lowest part of S/N merged region

            for (c in 1..10) {
                hRow11.createCell(c).apply { setCellValue(c.toDouble()); setCellStyle(greenHeaderStyle) }
            }

            // Lower cells for vertical merged header columns
            hRow11.createCell(11).setCellStyle(greenVerticalStyle)
            hRow11.createCell(12).setCellStyle(greenVerticalStyle)
            for (c in 13..19) {
                hRow11.createCell(c).setCellStyle(redVerticalStyle)
            }


            // 4. Data Rows (Rows 12 to 31 - indexes 12 to 31)
            val dataStartRowIndex = 12
            val totalRows = 20

            for (rIndex in 0 until totalRows) {
                val xlRow = sheet.createRow(dataStartRowIndex + rIndex)
                xlRow.heightInPoints = 20f

                // Row S/N with gray fill and bold numbers
                xlRow.createCell(0).apply { setCellValue((rIndex + 1).toDouble()); setCellStyle(snCellStyle) }

                val rowData = if (rIndex < study.rows.size) study.rows[rIndex] else null

                // 10 Cycle columns (White background, thin gray borders)
                for (c in 0 until 10) {
                    val cell = xlRow.createCell(1 + c)
                    cell.setCellStyle(dataRegularStyle)
                    if (rowData != null && c < rowData.cycleTimes.size && rowData.cycleTimes[c] > 0.0) {
                        cell.setCellValue(rowData.cycleTimes[c])
                    }
                }

                // Effective columns
                val cellBundle = xlRow.createCell(11).apply { setCellStyle(dataRegularStyle) }
                val cellBobbin = xlRow.createCell(12).apply { setCellStyle(dataRegularStyle) }
                if (rowData != null) {
                    if (rowData.effectiveBundleHandling > 0.0) cellBundle.setCellValue(rowData.effectiveBundleHandling)
                    if (rowData.effectiveBobbinChange > 0.0) cellBobbin.setCellValue(rowData.effectiveBobbinChange)
                }

                // Non-effective columns
                val nonEffValues = if (rowData != null) {
                    listOf(
                        rowData.nonEffectiveThreadBreakage, rowData.nonEffectiveNeedleBreakage,
                        rowData.nonEffectiveMachineBreakdown, rowData.nonEffectiveWaitingForWork,
                        rowData.nonEffectiveRework, rowData.nonEffectivePersonalFatigue,
                        rowData.nonEffectiveOthers
                    )
                } else {
                    List(7) { 0.0 }
                }

                for (c in 0 until 7) {
                    val cell = xlRow.createCell(13 + c).apply { setCellStyle(dataRegularStyle) }
                    if (nonEffValues[c] > 0.0) {
                        cell.setCellValue(nonEffValues[c])
                    }
                }
            }


            // 5. Productive / Non-Productive Dual Divider Bar (Row 32)
            val barRow = sheet.createRow(32)
            barRow.heightInPoints = 20f

            val barCellL = barRow.createCell(0).apply {
                setCellValue("PRODUCTIVE TIME")
                setCellStyle(productiveTimeBarStyle)
            }
            sheet.addMergedRegion(CellRangeAddress(32, 32, 0, 12))

            val barCellR = barRow.createCell(13).apply {
                setCellValue("NON PRODUCTIVE TIME")
                setCellStyle(nonProductiveTimeBarStyle)
            }
            sheet.addMergedRegion(CellRangeAddress(32, 32, 13, 19))

            // Render backgrounds cleanly in the collapsed bar visual spans
            for (c in 0..19) {
                val cell = barRow.getCell(c) ?: barRow.createCell(c)
                if (c <= 12) {
                    cell.setCellStyle(productiveTimeBarStyle)
                } else {
                    cell.setCellStyle(nonProductiveTimeBarStyle)
                }
            }


            // 6. Side-by-Side Calculations & Statistics (Rows 33 to 38)
            // Left Card: Performance Metrics (Col A to J)
            // Right Card: Summary & Error % (Col K to T)
            val metricRows = listOf(
                // Label Left, Formula Left (Col I), Label Right, Formula Right (Col R)
                listOf("Total Produced PCS During Study", "COUNT(B13:K32)", "Total Productive Time", "SUM(B13:M32)"),
                listOf("Previous Best Achieved", "I10", "Total Non Productive Time", "SUM(N13:T32)"),
                listOf("Target Improvement", "I34-I35", "Total Observe Time", "R34+R35"),
                listOf("Improvement %", "IF(I34>0, I36/I34, 0)", "Total Study Time (mins)", (study.totalStudyTime * 60.0).toString()),
                listOf("Avg. Capacity Time", "IF(I34>0, SUM(B13:K32)/I34, 0)", "Error %", "IF(R37>0, (R37-R36)/R37, 0)"),
                listOf("Capacity Target", "IF(I38>0, 3600/I38, 0)", "", "")
            )

            val startCalcPanelRowIndex = 33
            for (i in metricRows.indices) {
                val panelRow = sheet.createRow(startCalcPanelRowIndex + i)
                panelRow.heightInPoints = 20f

                val rowDataList = metricRows[i]
                val labelL = rowDataList[0]
                val formulaL = rowDataList[1]
                val labelR = rowDataList[2]
                val formulaR = rowDataList[3]

                // Left card labels (Col A to H merged)
                panelRow.createCell(0).apply { setCellValue(labelL); setCellStyle(metricLabelStyle) }
                sheet.addMergedRegion(CellRangeAddress(startCalcPanelRowIndex + i, startCalcPanelRowIndex + i, 0, 7))

                // Left card active values (Col I to J merged)
                val leftValStyle = when (labelL) {
                    "Total Produced PCS During Study" -> styleGreenPcs
                    "Previous Best Achieved" -> styleGreenPcsHr
                    "Target Improvement" -> styleGreenTargetImp
                    "Improvement %" -> styleGreenPercent
                    "Avg. Capacity Time" -> styleGreenSeconds
                    else -> styleGreenPcsHr // Capacity Target
                }

                panelRow.createCell(8).apply {
                    setCellFormula(formulaL)
                    setCellStyle(leftValStyle)
                }
                sheet.addMergedRegion(CellRangeAddress(startCalcPanelRowIndex + i, startCalcPanelRowIndex + i, 8, 9))

                // Adjust layout outlines on left
                for (c in 0..9) {
                    val cell = panelRow.getCell(c) ?: panelRow.createCell(c)
                    if (c in 1..7) cell.setCellStyle(metricLabelStyle)
                    if (c == 9) cell.setCellStyle(leftValStyle)
                }

                // Right card values
                if (labelR.isNotEmpty()) {
                    // Right Card labels (Col K to Q merged)
                    panelRow.createCell(10).apply { setCellValue(labelR); setCellStyle(metricLabelStyle) }
                    sheet.addMergedRegion(CellRangeAddress(startCalcPanelRowIndex + i, startCalcPanelRowIndex + i, 10, 16))

                    // Right Card Active Values (Col R to T merged)
                    val rightValStyle = when (labelR) {
                        "Total Productive Time" -> styleRightProductiveTime
                        "Total Non Productive Time" -> styleRightNonProductiveTime
                        "Total Observe Time" -> styleRightObserveTime
                        "Total Study Time (mins)" -> styleRightStudyTime
                        else -> styleRightErrorPercent // Error %
                    }

                    panelRow.createCell(17).apply {
                        if (formulaR.replace(".", "").all { it.isDigit() }) {
                            setCellValue(formulaR.toDouble())
                        } else {
                            setCellFormula(formulaR)
                        }
                        setCellStyle(rightValStyle)
                    }
                    sheet.addMergedRegion(CellRangeAddress(startCalcPanelRowIndex + i, startCalcPanelRowIndex + i, 17, 19))

                    for (c in 10..19) {
                        val cell = panelRow.getCell(c) ?: panelRow.createCell(c)
                        if (c in 11..16) cell.setCellStyle(metricLabelStyle)
                        if (c in 18..19) cell.setCellStyle(rightValStyle)
                    }
                } else {
                    // Right bottom spacer empty box
                    for (c in 10..19) {
                        val cell = panelRow.createCell(c).apply { setCellStyle(gridBorderThin) }
                        if (c == 10) {
                            sheet.addMergedRegion(CellRangeAddress(startCalcPanelRowIndex + i, startCalcPanelRowIndex + i, 10, 19))
                        }
                    }
                }
            }

            // Spacer Row (Row 39)
            sheet.createRow(39).heightInPoints = 8f


            // 7. Hourly Production Block (Rows 40 to 42)
            val hourlyHeaderRowIndex = 40
            val hHeadingRow = sheet.createRow(hourlyHeaderRowIndex)
            hHeadingRow.heightInPoints = 20f
            hHeadingRow.createCell(0).apply { setCellValue("Hourly Production After Study (8 Hours)"); setCellStyle(boldHeaderCenterStyle) }
            sheet.addMergedRegion(CellRangeAddress(hourlyHeaderRowIndex, hourlyHeaderRowIndex, 0, 19))
            for (c in 0..19) {
                hHeadingRow.getCell(c)?.setCellStyle(boldHeaderCenterStyle) ?: hHeadingRow.createCell(c).setCellStyle(boldHeaderCenterStyle)
            }

            val hLabelsRowIndex = 41
            val hValuesRowIndex = 42
            val hLabelsRow = sheet.createRow(hLabelsRowIndex)
            hLabelsRow.heightInPoints = 18f
            val hValuesRow = sheet.createRow(hValuesRowIndex)
            hValuesRow.heightInPoints = 18f

            // Hourly Columns mapped nicely across width
            for (h in 0 until 8) {
                val colStart = h * 2
                val colEnd = h * 2 + 1

                hLabelsRow.createCell(colStart).apply { setCellValue("Hour ${h + 1}"); setCellStyle(boldHeaderCenterStyle) }
                sheet.addMergedRegion(CellRangeAddress(hLabelsRowIndex, hLabelsRowIndex, colStart, colEnd))
                hLabelsRow.createCell(colEnd).setCellStyle(boldHeaderCenterStyle)

                val count = if (h < study.hourlyProduction.size) study.hourlyProduction[h] else 0
                val countCell = hValuesRow.createCell(colStart).apply {
                    if (count > 0) {
                        setCellValue(count.toDouble())
                    } else {
                        setCellValue("-")
                    }
                    setCellStyle(gridBorderThin)
                }
                sheet.addMergedRegion(CellRangeAddress(hValuesRowIndex, hValuesRowIndex, colStart, colEnd))
                hValuesRow.createCell(colEnd).setCellStyle(gridBorderThin)
            }

            // Hourly end empty spacer (Col 16 to 19 merged)
            hLabelsRow.createCell(16).apply { setCellValue(""); setCellStyle(boldHeaderCenterStyle) }
            sheet.addMergedRegion(CellRangeAddress(hLabelsRowIndex, hLabelsRowIndex, 16, 19))
            hValuesRow.createCell(16).apply { setCellValue(""); setCellStyle(gridBorderThin) }
            sheet.addMergedRegion(CellRangeAddress(hValuesRowIndex, hValuesRowIndex, 16, 19))

            for (c in 17..19) {
                hLabelsRow.createCell(c).setCellStyle(boldHeaderCenterStyle)
                hValuesRow.createCell(c).setCellStyle(gridBorderThin)
            }

            // Spacer Row (Row 43)
            sheet.createRow(43).heightInPoints = 8f


            // 8. Remarks Block (Row 44)
            val remarksRowIndex = 44
            val remarksRow = sheet.createRow(remarksRowIndex)
            remarksRow.heightInPoints = 22f

            remarksRow.createCell(0).apply { setCellValue("Remarks"); setCellStyle(boldHeaderCenterStyle) }
            sheet.addMergedRegion(CellRangeAddress(remarksRowIndex, remarksRowIndex, 0, 2))

            remarksRow.createCell(3).apply { setCellValue(study.remarks.ifEmpty { "N/A" }); setCellStyle(metadataValueStyle) }
            sheet.addMergedRegion(CellRangeAddress(remarksRowIndex, remarksRowIndex, 3, 19))

            for (c in 0..19) {
                val cell = remarksRow.getCell(c) ?: remarksRow.createCell(c)
                if (c <= 2) {
                    cell.setCellStyle(boldHeaderCenterStyle)
                } else {
                    cell.setCellStyle(metadataValueStyle)
                }
            }

            // Spacer Row before signatures (Row 45)
            sheet.createRow(45).heightInPoints = 8f


            // 9. Signature Authorizations (Rows 46 to 47 - indexes 46 & 47)
            val signatureTitles = listOf(
                "Worker Signature", "Line Supervisor", "Line Chief",
                "APM / Floor Incharge", "Production Manager", "IE Executive"
            )
            val sigStatus = listOf(
                if (study.workerSignature.isNotEmpty()) "SIGNED" else "PENDING",
                if (study.lineSupervisorSignature.isNotEmpty()) "SIGNED" else "PENDING",
                if (study.lineChiefSignature.isNotEmpty()) "SIGNED" else "PENDING",
                if (study.apmFloorInchargeSignature.isNotEmpty()) "SIGNED" else "PENDING",
                if (study.productionMgrSignature.isNotEmpty()) "SIGNED" else "PENDING",
                if (study.ieExecutiveSignature.isNotEmpty()) "SIGNED" else "PENDING"
            )
            val sigImages = listOf(
                study.workerSignature, study.lineSupervisorSignature, study.lineChiefSignature,
                study.apmFloorInchargeSignature, study.productionMgrSignature, study.ieExecutiveSignature
            )

            val sigImageRowIndex = 46
            val sigLabelRowIndex = 47
            val sigImageRow = sheet.createRow(sigImageRowIndex)
            sigImageRow.heightInPoints = 48f  // Dedicated sketch/image bounding canvas
            val sigLabelRow = sheet.createRow(sigLabelRowIndex)
            sigLabelRow.heightInPoints = 20f

            val signatureLabelStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFont(boldFont)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFill(colorHeaderGray)
                setGrayBorders(colorBorderGray)
                borderTop = BorderStyle.THIN // Beautiful solid baseline above signature text
                setTopBorderColor(colorBorderGray)
            }

            for (s in signatureTitles.indices) {
                // Symmetrical 6 gaps summing to exactly 20 columns width
                val cStart = when (s) {
                    0 -> 0; 1 -> 3; 2 -> 6; 3 -> 9; 4 -> 12; else -> 16
                }
                val cEnd = when (s) {
                    0 -> 2; 1 -> 5; 2 -> 8; 3 -> 11; 4 -> 15; else -> 19
                }

                // Signature bounding box cell drawing
                sigImageRow.createCell(cStart).apply { setCellValue(""); setCellStyle(gridBorderThin) }
                sheet.addMergedRegion(CellRangeAddress(sigImageRowIndex, sigImageRowIndex, cStart, cEnd))
                for (c in cStart..cEnd) {
                    sigImageRow.getCell(c)?.setCellStyle(gridBorderThin) ?: sigImageRow.createCell(c).setCellStyle(gridBorderThin)
                }

                // Inject png handwritten signature
                insertSignature(workbook, sheet, sigImages[s], cStart, cEnd, sigImageRowIndex)

                // Label & Underline signature text
                sigLabelRow.createCell(cStart).apply {
                    setCellValue("${signatureTitles[s]} (${sigStatus[s]})")
                    setCellStyle(signatureLabelStyle)
                }
                sheet.addMergedRegion(CellRangeAddress(sigLabelRowIndex, sigLabelRowIndex, cStart, cEnd))
                for (c in cStart..cEnd) {
                    sigLabelRow.getCell(c)?.setCellStyle(signatureLabelStyle) ?: sigLabelRow.createCell(c).setCellStyle(signatureLabelStyle)
                }
            }


            // Set column widths so content matches nicely on print portrait output
            for (col in 0..19) {
                when (col) {
                    0 -> sheet.setColumnWidth(col, 1600)  // S/N column
                    in 1..10 -> sheet.setColumnWidth(col, 1600)  // Cycle times 1..10 columns
                    in 11..12 -> sheet.setColumnWidth(col, 2800)  // Effective columns
                    else -> sheet.setColumnWidth(col, 2600)  // Non-Effective activities
                }
            }

            // Print Area: Column A1 to T48 (matches portrait margins perfectly)
            workbook.setPrintArea(0, 0, 19, 0, 47)

            // Cache generated stream and launch trigger share menu
            val cachePath = File(context.cacheDir, "pdf")
            cachePath.mkdirs()
            val file = File(cachePath, "Production_Study_${study.id}.xlsx")
            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            outputStream.flush()
            outputStream.close()
            workbook.close()

            val uri = FileProvider.getUriForFile(context, "com.aistudio.productionstudy.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Excel Production Study Sheet: ${study.buyer} - ${study.style}")
                putExtra(Intent.EXTRA_TEXT, "Hello! Attached is the live formula-based Excel spreadsheet (.xlsx) containing the production study recorded for Worker: ${study.operatorName} Style: ${study.style}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Production Study Excel via:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(context, "Error exporting Excel sheet: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }

    private fun calcHeaderHeaderRegion(rowIndex: Int): Int {
        return rowIndex
    }
}
