package com.example.data.repository

import com.example.data.db.ProductionStudyDao
import com.example.data.model.ProductionStudy
import kotlinx.coroutines.flow.Flow

class ProductionStudyRepository(private val dao: ProductionStudyDao) {
    val allStudies: Flow<List<ProductionStudy>> = dao.getAllStudies()

    fun searchStudies(query: String): Flow<List<ProductionStudy>> {
        return dao.searchStudies("%$query%")
    }

    suspend fun getStudyById(id: Long): ProductionStudy? {
        return dao.getStudyById(id)
    }

    suspend fun insertStudy(study: ProductionStudy): Long {
        return dao.insertStudy(study)
    }

    suspend fun updateStudy(study: ProductionStudy) {
        dao.updateStudy(study)
    }

    suspend fun deleteStudy(study: ProductionStudy) {
        dao.deleteStudy(study)
    }
}
