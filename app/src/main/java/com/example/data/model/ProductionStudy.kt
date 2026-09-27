package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StudyRow(
    val rowId: Int, // 1 to 20
    val cycleTimes: List<Double> = List(10) { 0.0 },
    val effectiveBundleHandling: Double = 0.0,
    val effectiveBobbinChange: Double = 0.0,
    val nonEffectiveThreadBreakage: Double = 0.0,
    val nonEffectiveNeedleBreakage: Double = 0.0,
    val nonEffectiveMachineBreakdown: Double = 0.0,
    val nonEffectiveWaitingForWork: Double = 0.0,
    val nonEffectiveRework: Double = 0.0,
    val nonEffectivePersonalFatigue: Double = 0.0,
    val nonEffectiveOthers: Double = 0.0
)

@Entity(tableName = "production_studies")
@JsonClass(generateAdapter = true)
data class ProductionStudy(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val factoryName: String = "",
    val buyer: String = "",
    val style: String = "",
    val lineNo: String = "",
    val operationName: String = "",
    val operatorName: String = "",
    val date: String = "",
    val capacityTarget: Double = 0.0,
    val totalProducedPcs: Int = 0,
    val previousBestAchieved: Double = 0.0,
    val totalStudyTime: Double = 0.0, // in minutes
    val smv: Double = 0.0,
    val unit: String = "",
    val studyBy: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val remarks: String = "",
    
    // Grid Data (serialized as JSON list via TypeConverter)
    val rows: List<StudyRow> = (1..20).map { StudyRow(rowId = it) },
    
    // Hourly Production (8 hours)
    val hourlyProduction: List<Int> = List(8) { 0 },
    
    // Signatures (Base64 encoded PNG or empty string)
    val workerSignature: String = "",
    val lineSupervisorSignature: String = "",
    val lineChiefSignature: String = "",
    val apmFloorInchargeSignature: String = "",
    val productionMgrSignature: String = "",
    val ieExecutiveSignature: String = "",
    
    val timestamp: Long = System.currentTimeMillis()
) {
    val calculatedTotalProducedPcs: Int
        get() = rows.sumOf { row -> row.cycleTimes.count { it > 0.0 } }

    val calculatedAvgCycleTime: Double
        get() {
            val totalProducePcs = calculatedTotalProducedPcs.toDouble()
            val cycleTimesSum = rows.flatMap { it.cycleTimes }.filter { it > 0.0 }.sum()
            return if (totalProducePcs > 0) cycleTimesSum / totalProducePcs else 0.0
        }

    val calculatedCapacityTarget: Double
        get() {
            val avgCapacityTime = calculatedAvgCycleTime
            return if (avgCapacityTime > 0.0) 3600.0 / avgCapacityTime else 0.0
        }
}
