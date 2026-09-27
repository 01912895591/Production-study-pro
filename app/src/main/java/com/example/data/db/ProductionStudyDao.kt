package com.example.data.db

import androidx.room.*
import com.example.data.model.ProductionStudy
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductionStudyDao {
    @Query("SELECT * FROM production_studies ORDER BY timestamp DESC")
    fun getAllStudies(): Flow<List<ProductionStudy>>

    @Query("SELECT * FROM production_studies WHERE id = :id")
    suspend fun getStudyById(id: Long): ProductionStudy?

    @Query("SELECT * FROM production_studies WHERE buyer LIKE :query OR style LIKE :query OR operatorName LIKE :query OR operationName LIKE :query ORDER BY timestamp DESC")
    fun searchStudies(query: String): Flow<List<ProductionStudy>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudy(study: ProductionStudy): Long

    @Update
    suspend fun updateStudy(study: ProductionStudy)

    @Delete
    suspend fun deleteStudy(study: ProductionStudy)
}
