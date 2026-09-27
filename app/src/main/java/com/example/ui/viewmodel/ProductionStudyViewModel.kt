package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ProductionStudy
import com.example.data.model.StudyRow
import com.example.data.repository.ProductionStudyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class ProductionStudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductionStudyRepository
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val studyAdapter = moshi.adapter(ProductionStudy::class.java)
    
    val searchQuery = MutableStateFlow("")
    val sortBy = MutableStateFlow(SortOption.DATE_DESC)
    val activeStudy = MutableStateFlow<ProductionStudy?>(null)

    // Scoped study UI session state flows
    val activeRowIndex = MutableStateFlow(1)
    val headerExpanded = MutableStateFlow(false)
    val sheetExpanded = MutableStateFlow(true)
    val stopwatchExpanded = MutableStateFlow(true)
    val metricsExpanded = MutableStateFlow(false)
    val hourlyExpanded = MutableStateFlow(false)
    val signaturesExpanded = MutableStateFlow(false)
    val remarksExpanded = MutableStateFlow(false)

    // Stopwatch tracking state
    val activeTimingRow = MutableStateFlow<Int?>(null) // 1 to 20
    val activeTimingCol = MutableStateFlow<Int?>(null) // 0 to 18
    val runningElapsedSecs = MutableStateFlow(0.0)
    
    private var tickerJob: Job? = null
    private var stopwatchStartTimeMillis = 0L
    private var initialElapsedSecs = 0.0

    private var isRestoring = false

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProductionStudyRepository(database.productionStudyDao())

        // Restore study session from local storage on process startup if any
        val prefs = application.getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        val savedId = prefs.getLong("active_study_id", 0L)
        if (savedId > 0L) {
            isRestoring = true
            viewModelScope.launch {
                try {
                    // Try to restore from SharedPreferences (conceptual localStorage) JSON draft first on IO Context
                    val study = withContext(Dispatchers.IO) {
                        var tempStudy: ProductionStudy? = null
                        val draftJson = prefs.getString("draft_study_${savedId}", null)
                        if (draftJson != null) {
                            try {
                                tempStudy = studyAdapter.fromJson(draftJson)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        if (tempStudy == null) {
                            tempStudy = repository.getStudyById(savedId)
                        }
                        tempStudy
                    }

                    if (study != null) {
                        val states = withContext(Dispatchers.IO) {
                            val ari = prefs.getInt("active_row_index_${savedId}", 1)
                            val he = prefs.getBoolean("header_expanded_${savedId}", false)
                            val se = prefs.getBoolean("sheet_expanded_${savedId}", true)
                            val swe = prefs.getBoolean("stopwatch_expanded_${savedId}", true)
                            val me = prefs.getBoolean("metrics_expanded_${savedId}", false)
                            val hoe = prefs.getBoolean("hourly_expanded_${savedId}", false)
                            val sige = prefs.getBoolean("signatures_expanded_${savedId}", false)
                            val reme = prefs.getBoolean("remarks_expanded_${savedId}", false)
                            listOf(ari, he, se, swe, me, hoe, sige, reme)
                        }

                        activeRowIndex.value = states[0] as Int
                        headerExpanded.value = states[1] as Boolean
                        sheetExpanded.value = states[2] as Boolean
                        stopwatchExpanded.value = states[3] as Boolean
                        metricsExpanded.value = states[4] as Boolean
                        hourlyExpanded.value = states[5] as Boolean
                        signaturesExpanded.value = states[6] as Boolean
                        remarksExpanded.value = states[7] as Boolean

                        activeStudy.value = study
                        restoreStopwatchState()
                    }
                } finally {
                    isRestoring = false
                }
            }
        }

        // Periodically/Automatically store which study session is currently open/active
        viewModelScope.launch {
            activeStudy.collect { study ->
                if (isRestoring && study == null) {
                    return@collect
                }
                withContext(Dispatchers.IO) {
                    val currentPrefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
                    currentPrefs.edit().putLong("active_study_id", study?.id ?: 0L).apply()
                }
            }
        }
    }

    fun updateActiveRowIndex(value: Int) {
        activeRowIndex.value = value
        val studyId = activeStudy.value?.id ?: return
        val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("active_row_index_${studyId}", value).apply()
    }

    fun updateHeaderExpanded(value: Boolean) {
        headerExpanded.value = value
        val studyId = activeStudy.value?.id ?: return
        val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("header_expanded_${studyId}", value).apply()
    }

    fun updateSheetExpanded(value: Boolean) {
        sheetExpanded.value = value
        val studyId = activeStudy.value?.id ?: return
        val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("sheet_expanded_${studyId}", value).apply()
    }

    fun updateStopwatchExpanded(value: Boolean) {
        stopwatchExpanded.value = value
        val studyId = activeStudy.value?.id ?: return
        val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("stopwatch_expanded_${studyId}", value).apply()
    }

    fun updateMetricsExpanded(value: Boolean) {
        metricsExpanded.value = value
        val studyId = activeStudy.value?.id ?: return
        val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("metrics_expanded_${studyId}", value).apply()
    }

    fun updateHourlyExpanded(value: Boolean) {
        hourlyExpanded.value = value
        val studyId = activeStudy.value?.id ?: return
        val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("hourly_expanded_${studyId}", value).apply()
    }

    fun updateSignaturesExpanded(value: Boolean) {
        signaturesExpanded.value = value
        val studyId = activeStudy.value?.id ?: return
        val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("signatures_expanded_${studyId}", value).apply()
    }

    fun updateRemarksExpanded(value: Boolean) {
        remarksExpanded.value = value
        val studyId = activeStudy.value?.id ?: return
        val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("remarks_expanded_${studyId}", value).apply()
    }

    private fun saveStopwatchStateToPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("active_timing_row", activeTimingRow.value ?: -1)
            putInt("active_timing_col", activeTimingCol.value ?: -1)
            putBoolean("stopwatch_running", tickerJob != null)
            putLong("stopwatch_start_time", stopwatchStartTimeMillis)
            putFloat("initial_elapsed_secs", initialElapsedSecs.toFloat())
            apply()
        }
    }

    private fun restoreStopwatchState() {
        val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("stopwatch_running", false)
        val rowId = prefs.getInt("active_timing_row", -1)
        val colCode = prefs.getInt("active_timing_col", -1)

        if (rowId != -1 && colCode != -1 && isRunning) {
            val savedInitial = prefs.getFloat("initial_elapsed_secs", 0f).toDouble()
            val savedStartTime = prefs.getLong("stopwatch_start_time", 0L)

            if (savedStartTime > 0L) {
                val now = System.currentTimeMillis()
                val elapsedSinceStart = (now - savedStartTime) / 1000.0

                activeTimingRow.value = rowId
                activeTimingCol.value = colCode
                stopwatchStartTimeMillis = savedStartTime
                initialElapsedSecs = savedInitial
                runningElapsedSecs.value = savedInitial + elapsedSinceStart

                tickerJob = viewModelScope.launch {
                    var lastSavedSecs = runningElapsedSecs.value
                    while (true) {
                        delay(100)
                        val diffSecs = (System.currentTimeMillis() - stopwatchStartTimeMillis) / 1000.0
                        val currentSecs = initialElapsedSecs + diffSecs
                        runningElapsedSecs.value = currentSecs

                        if (currentSecs - lastSavedSecs >= 1.0) {
                            lastSavedSecs = currentSecs
                            saveCellValue(rowId, colCode, currentSecs)
                        }
                    }
                }
            }
        }
    }

    // List of studies live from Room based on searchQuery and sort option
    val savedStudies: StateFlow<List<ProductionStudy>> = searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allStudies
            } else {
                repository.searchStudies(query)
            }
        }
        .combine(sortBy) { list, sortOpt ->
            when (sortOpt) {
                SortOption.DATE_DESC -> list.sortedByDescending { it.timestamp }
                SortOption.DATE_ASC -> list.sortedBy { it.timestamp }
                SortOption.BUYER_ASC -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.buyer.trim() })
                SortOption.BUYER_DESC -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.buyer.trim() })
                SortOption.STYLE_ASC -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.style.trim() })
                SortOption.LINE_ASC -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.lineNo.trim() })
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNewStudy() {
        // Stop any active timing before shifting
        stopAnyActiveTiming()

        viewModelScope.launch {
            // Build initial ProductionStudy on background context
            val initialStudy = withContext(Dispatchers.IO) {
                ProductionStudy(
                    date = android.text.format.DateFormat.format("yyyy-MM-dd", System.currentTimeMillis()).toString(),
                    startTime = ""
                )
            }
            
            // Perform the insert and Moshi serialization off-main-thread
            val (insertId, initialJson) = withContext(Dispatchers.IO) {
                val id = repository.insertStudy(initialStudy)
                val newStudyWithId = initialStudy.copy(id = id)
                val json = try {
                    studyAdapter.toJson(newStudyWithId)
                } catch (e: Exception) {
                    null
                }
                Pair(id, json)
            }

            val newStudyWithId = initialStudy.copy(id = insertId)

            // Set default flow variables (instantly updates UI)
            activeRowIndex.value = 1
            headerExpanded.value = false
            sheetExpanded.value = true
            stopwatchExpanded.value = true
            metricsExpanded.value = false
            hourlyExpanded.value = false
            signaturesExpanded.value = false
            remarksExpanded.value = false

            // Store in SharedPreferences on background thread
            withContext(Dispatchers.IO) {
                val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putInt("active_row_index_${insertId}", 1)
                    putBoolean("header_expanded_${insertId}", false)
                    putBoolean("sheet_expanded_${insertId}", true)
                    putBoolean("stopwatch_expanded_${insertId}", true)
                    putBoolean("metrics_expanded_${insertId}", false)
                    putBoolean("hourly_expanded_${insertId}", false)
                    putBoolean("signatures_expanded_${insertId}", false)
                    putBoolean("remarks_expanded_${insertId}", false)
                    
                    if (initialJson != null) {
                        putString("draft_study_${insertId}", initialJson)
                    }
                    apply()
                }
            }

            // Post transition to the UI instantly
            activeStudy.value = newStudyWithId
        }
    }

    fun loadStudy(studyId: Long) {
        viewModelScope.launch {
            stopAnyActiveTiming()
            
            // Try to restore from SharedPreferences JSON draft first on IO Context
            val study = withContext(Dispatchers.IO) {
                var s: ProductionStudy? = null
                val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
                val draftJson = prefs.getString("draft_study_${studyId}", null)
                if (draftJson != null) {
                    try {
                        s = studyAdapter.fromJson(draftJson)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
                if (s == null) {
                    s = repository.getStudyById(studyId)
                }
                s
            }

            if (study != null) {
                // Restore UI states from SharedPreferences on background thread
                val states = withContext(Dispatchers.IO) {
                    val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
                    val ari = prefs.getInt("active_row_index_${studyId}", 1)
                    val he = prefs.getBoolean("header_expanded_${studyId}", false)
                    val se = prefs.getBoolean("sheet_expanded_${studyId}", true)
                    val swe = prefs.getBoolean("stopwatch_expanded_${studyId}", true)
                    val me = prefs.getBoolean("metrics_expanded_${studyId}", false)
                    val hoe = prefs.getBoolean("hourly_expanded_${studyId}", false)
                    val sige = prefs.getBoolean("signatures_expanded_${studyId}", false)
                    val reme = prefs.getBoolean("remarks_expanded_${studyId}", false)
                    listOf(ari, he, se, swe, me, hoe, sige, reme)
                }

                // Apply them back safely
                activeRowIndex.value = states[0] as Int
                headerExpanded.value = states[1] as Boolean
                sheetExpanded.value = states[2] as Boolean
                stopwatchExpanded.value = states[3] as Boolean
                metricsExpanded.value = states[4] as Boolean
                hourlyExpanded.value = states[5] as Boolean
                signaturesExpanded.value = states[6] as Boolean
                remarksExpanded.value = states[7] as Boolean

                activeStudy.value = study
            }
        }
    }

    fun deleteStudy(study: ProductionStudy) {
        viewModelScope.launch {
            if (activeStudy.value?.id == study.id) {
                stopAnyActiveTiming()
                activeStudy.value = null
            }
            withContext(Dispatchers.IO) {
                repository.deleteStudy(study)
                
                // Delete accompanying local storage JSON draft
                val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("draft_study_${study.id}").apply()
            }
        }
    }

    fun closeActiveStudy() {
        stopAnyActiveTiming()
        activeStudy.value = null
    }

    fun getCurrentFormattedTime(): String {
        return android.text.format.DateFormat.format("hh:mm a", System.currentTimeMillis()).toString()
    }

    private fun parseTimeToMinutes(timeStr: String): Double? {
        val clean = timeStr.trim().replace(".", ":").uppercase(Locale.US)
        if (clean.isEmpty()) return null
        val formats = listOf("hh:mm a", "h:mm a", "HH:mm", "H:mm")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = true
                val date: Date? = sdf.parse(clean)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    return (hour * 60 + minute).toDouble()
                }
            } catch (e: Exception) {
                // Ignore and try next format
            }
        }
        return null
    }

    fun calculateTotalStudyTime(start: String, end: String): Double {
        val startMins = parseTimeToMinutes(start) ?: return 0.0
        val endMins = parseTimeToMinutes(end) ?: return 0.0
        var diff = endMins - startMins
        if (diff < 0) {
            diff += 1440.0 // Handle midnight crossing robustly
        }
        return diff
    }

    // Auto-saves state immediately to Database & SharedPreferences JSON draft (conceptual localStorage)
    fun saveActiveStudyToDb() {
        val current = activeStudy.value ?: return
        if (current.id == 0L) {
            // Unsaved in-memory study during instant transition. Save is handled post-insertion.
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateStudy(current)
                
                // Save JSON draft to SharedPreferences
                try {
                    val prefs = getApplication<Application>().getSharedPreferences("production_study_prefs", Context.MODE_PRIVATE)
                    val json = studyAdapter.toJson(current)
                    prefs.edit().putString("draft_study_${current.id}", json).apply()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateMetadata(
        factoryName: String? = null,
        buyer: String? = null,
        style: String? = null,
        lineNo: String? = null,
        operationName: String? = null,
        operatorName: String? = null,
        date: String? = null,
        capacityTarget: Double? = null,
        totalProducedPcs: Int? = null,
        previousBestAchieved: Double? = null,
        totalStudyTime: Double? = null,
        smv: Double? = null,
        unit: String? = null,
        studyBy: String? = null,
        startTime: String? = null,
        endTime: String? = null,
        remarks: String? = null
    ) {
        val current = activeStudy.value ?: return
        val newStartTime = startTime ?: current.startTime
        val newEndTime = endTime ?: current.endTime
        
        // Auto-recalculate total duration if startTime or endTime changes
        val resolvedTotalTime = if (startTime != null || endTime != null) {
            val autoTime = calculateTotalStudyTime(newStartTime, newEndTime)
            if (autoTime > 0.0) autoTime else (totalStudyTime ?: current.totalStudyTime)
        } else {
            totalStudyTime ?: current.totalStudyTime
        }

        val updated = current.copy(
            factoryName = factoryName ?: current.factoryName,
            buyer = buyer ?: current.buyer,
            style = style ?: current.style,
            lineNo = lineNo ?: current.lineNo,
            operationName = operationName ?: current.operationName,
            operatorName = operatorName ?: current.operatorName,
            date = date ?: current.date,
            capacityTarget = capacityTarget ?: current.capacityTarget,
            totalProducedPcs = totalProducedPcs ?: current.totalProducedPcs,
            previousBestAchieved = previousBestAchieved ?: current.previousBestAchieved,
            totalStudyTime = resolvedTotalTime,
            smv = smv ?: current.smv,
            unit = unit ?: current.unit,
            studyBy = studyBy ?: current.studyBy,
            startTime = newStartTime,
            endTime = newEndTime,
            remarks = remarks ?: current.remarks
        )
        activeStudy.value = updated
        saveActiveStudyToDb()
    }

    fun updateHourlyProduction(hourIndex: Int, value: Int) {
        val current = activeStudy.value ?: return
        if (hourIndex in 0..7) {
            val newList = current.hourlyProduction.toMutableList()
            while (newList.size <= hourIndex) {
                newList.add(0)
            }
            newList[hourIndex] = value
            val updated = current.copy(hourlyProduction = newList)
            activeStudy.value = updated
            saveActiveStudyToDb()
        }
    }

    fun saveSignature(fieldName: String, base64: String) {
        val current = activeStudy.value ?: return
        val updated = when (fieldName) {
            "worker" -> current.copy(workerSignature = base64)
            "supervisor" -> current.copy(lineSupervisorSignature = base64)
            "chief" -> current.copy(lineChiefSignature = base64)
            "apm" -> current.copy(apmFloorInchargeSignature = base64)
            "manager" -> current.copy(productionMgrSignature = base64)
            "ie" -> current.copy(ieExecutiveSignature = base64)
            else -> current
        }
        activeStudy.value = updated
        saveActiveStudyToDb()
    }

    fun clearSignature(fieldName: String) {
        saveSignature(fieldName, "")
    }

    // ----------------------------------------------------
    // STOPWATCH & CELLS LOGIC
    // ----------------------------------------------------

    private fun findFirstEmptyRowForCol(colCode: Int, defaultRowId: Int): Int {
        for (r in 1..20) {
            val valOfCol = getCellValue(r, colCode)
            if (valOfCol == 0.0) {
                return r
            }
        }
        return defaultRowId
    }

    fun handleCellClick(rowId: Int, colCode: Int) {
        val current = activeStudy.value ?: return

        val prevRow = activeTimingRow.value
        val prevCol = activeTimingCol.value

        // Auto-capture start time if it is blank when starting study timing
        if (current.startTime.trim().isEmpty()) {
            val nowTime = getCurrentFormattedTime()
            updateMetadata(startTime = nowTime)
        }

        // 1. If currently timing the same column (especially for effective/non-effective), toggle-stop it!
        if (prevCol == colCode) {
            if (colCode >= 10 || prevRow == rowId) {
                stopTimerAndRecordEndTime()
                return
            }
        }

        // 2. If currently timing another cell, save and stop its timing first
        if (prevRow != null && prevCol != null) {
            val elapsed = runningElapsedSecs.value
            saveCellValue(prevRow, prevCol, elapsed)
        }

        // 3. Find the target row for starting timing
        val targetRowId = if (colCode >= 10) {
            findFirstEmptyRowForCol(colCode, rowId)
        } else {
            rowId
        }

        // 4. Start timing the new clicked cell
        stopAnyActiveTiming() // Resets ticker job cleanly
        
        // Load whatever time is already in this cell, let stopwatch resume/accumulate for standard cycles.
        // For effective/non-effective (colCode >= 10), we always start from 0.0 (no accumulation).
        val existingValue = if (colCode >= 10) 0.0 else getCellValue(targetRowId, colCode)
        initialElapsedSecs = existingValue
        
        activeTimingRow.value = targetRowId
        activeTimingCol.value = colCode
        stopwatchStartTimeMillis = System.currentTimeMillis()
        runningElapsedSecs.value = existingValue

        tickerJob = viewModelScope.launch {
            var lastSavedSecs = existingValue
            while (true) {
                delay(100)
                val diffSecs = (System.currentTimeMillis() - stopwatchStartTimeMillis) / 1000.0
                val currentSecs = initialElapsedSecs + diffSecs
                runningElapsedSecs.value = currentSecs

                if (currentSecs - lastSavedSecs >= 1.0) {
                    lastSavedSecs = currentSecs
                    saveCellValue(targetRowId, colCode, currentSecs)
                }
            }
        }
        saveStopwatchStateToPrefs()
    }

    fun handleCellDoubleClick() {
        stopAnyActiveTiming()
        val nowTime = getCurrentFormattedTime()
        updateMetadata(endTime = nowTime)
    }

    fun stopTimerAndRecordEndTime() {
        stopAnyActiveTiming()
        val nowTime = getCurrentFormattedTime()
        updateMetadata(endTime = nowTime)
    }

    fun stopAnyActiveTiming() {
        tickerJob?.cancel()
        tickerJob = null
        
        val prevRow = activeTimingRow.value
        val prevCol = activeTimingCol.value
        if (prevRow != null && prevCol != null) {
            val finalElapsed = runningElapsedSecs.value
            saveCellValue(prevRow, prevCol, finalElapsed)
        }

        activeTimingRow.value = null
        activeTimingCol.value = null
        runningElapsedSecs.value = 0.0
        initialElapsedSecs = 0.0
        saveStopwatchStateToPrefs()
    }

    fun manuallySetCellValue(rowId: Int, colCode: Int, value: Double) {
        // Safe check
        val isCurrentRunning = activeTimingRow.value == rowId && activeTimingCol.value == colCode
        if (isCurrentRunning) {
            stopAnyActiveTiming()
        }
        
        // Auto-capture start time if it is blank
        val current = activeStudy.value
        if (current != null && current.startTime.trim().isEmpty()) {
            val nowTime = getCurrentFormattedTime()
            updateMetadata(startTime = nowTime)
        }
        
        saveCellValue(rowId, colCode, value)
    }

    // Helper to get time value of cell
    fun getCellValue(rowId: Int, colCode: Int): Double {
        val current = activeStudy.value ?: return 0.0
        val row = current.rows.find { it.rowId == rowId } ?: return 0.0
        return when (colCode) {
            in 0..9 -> row.cycleTimes.getOrNull(colCode) ?: 0.0
            10 -> row.effectiveBundleHandling
            11 -> row.effectiveBobbinChange
            12 -> row.nonEffectiveThreadBreakage
            13 -> row.nonEffectiveNeedleBreakage
            14 -> row.nonEffectiveMachineBreakdown
            15 -> row.nonEffectiveWaitingForWork
            16 -> row.nonEffectiveRework
            17 -> row.nonEffectivePersonalFatigue
            18 -> row.nonEffectiveOthers
            else -> 0.0
        }
    }

    // Helper to write time value to active study
    private fun saveCellValue(rowId: Int, colCode: Int, value: Double) {
        val current = activeStudy.value ?: return
        val updatedRows = current.rows.map { row ->
            if (row.rowId == rowId) {
                when (colCode) {
                    in 0..9 -> {
                        val cycles = row.cycleTimes.toMutableList()
                        if (colCode < cycles.size) {
                            cycles[colCode] = value
                        }
                        row.copy(cycleTimes = cycles)
                    }
                    10 -> row.copy(effectiveBundleHandling = value)
                    11 -> row.copy(effectiveBobbinChange = value)
                    12 -> row.copy(nonEffectiveThreadBreakage = value)
                    13 -> row.copy(nonEffectiveNeedleBreakage = value)
                    14 -> row.copy(nonEffectiveMachineBreakdown = value)
                    15 -> row.copy(nonEffectiveWaitingForWork = value)
                    16 -> row.copy(nonEffectiveRework = value)
                    17 -> row.copy(nonEffectivePersonalFatigue = value)
                    18 -> row.copy(nonEffectiveOthers = value)
                    else -> row
                }
            } else {
                row
            }
        }
        activeStudy.value = current.copy(rows = updatedRows)
        saveActiveStudyToDb()
    }

    override fun onCleared() {
        stopAnyActiveTiming()
        super.onCleared()
    }
}

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    BUYER_ASC("Buyer (A-Z)"),
    BUYER_DESC("Buyer (Z-A)"),
    STYLE_ASC("Style (A-Z)"),
    LINE_ASC("Line No (Asc)")
}
