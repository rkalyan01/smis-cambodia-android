package com.innovative.smis.data.local.cache

import com.innovative.smis.data.local.dao.TodoItemDao

class OfflineCacheManager(
    private val todoItemDao: TodoItemDao
) {
    
    /**
     * Check if cache contains valid data for a specific status
     */
    suspend fun hasCachedDataForStatus(status: String): Boolean {
        return if (status.equals("All", true)) {
            todoItemDao.getApplicationCountByStatus("Initiated") > 0 ||
            todoItemDao.getApplicationCountByStatus("Completed") > 0 ||
            todoItemDao.getApplicationCountByStatus("In Progress") > 0
        } else {
            todoItemDao.getApplicationCountByStatus(status) > 0
        }
    }
    
    /**
     * Get all cached statuses to know what data we already have
     */
    suspend fun getCachedStatuses(): List<String> {
        return todoItemDao.getAllCachedStatuses()
    }
    
    /**
     * Clean up expired cache entries
     * Should be called periodically or before major sync operations
     */
    suspend fun cleanExpiredCache() {
        todoItemDao.clearExpiredCache()
    }
    
    /**
     * Check cache health and return cache statistics
     */
    suspend fun getCacheStats(): CacheStats {
        val allStatuses = todoItemDao.getAllCachedStatuses()
        val statusCounts = mutableMapOf<String, Int>()
        
        for (status in allStatuses) {
            statusCounts[status] = todoItemDao.getApplicationCountByStatus(status)
        }
        
        return CacheStats(
            totalCachedItems = statusCounts.values.sum(),
            statusBreakdown = statusCounts,
            cachedStatuses = allStatuses
        )
    }
    
    /**
     * Determine if we should make a network call based on cache status
     * Returns true if cache is empty or expired for the requested status
     */
    suspend fun shouldFetchFromNetwork(status: String): Boolean {
        return !hasCachedDataForStatus(status)
    }
}

data class CacheStats(
    val totalCachedItems: Int,
    val statusBreakdown: Map<String, Int>,
    val cachedStatuses: List<String>
)