package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.StudyRow
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val studyRowListType = Types.newParameterizedType(List::class.java, StudyRow::class.java)
    private val intListType = Types.newParameterizedType(List::class.java, java.lang.Integer::class.java)
    
    private val rowListAdapter = moshi.adapter<List<StudyRow>>(studyRowListType)
    private val intListAdapter = moshi.adapter<List<Int>>(intListType)

    @TypeConverter
    fun fromRowList(value: List<StudyRow>?): String {
        return rowListAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toRowList(value: String?): List<StudyRow> {
        if (value.isNullOrEmpty()) return emptyList()
        return rowListAdapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return intListAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        val defaultList = List(8) { 0 }
        if (value.isNullOrEmpty()) return defaultList
        val parsed = intListAdapter.fromJson(value) ?: defaultList
        if (parsed.size < 8) {
            return parsed + List(8 - parsed.size) { 0 }
        }
        return parsed
    }
}
