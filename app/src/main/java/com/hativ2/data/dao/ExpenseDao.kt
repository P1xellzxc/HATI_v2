package com.hativ2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.data.entity.SplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE dashboardId = :dashboardId ORDER BY createdAt DESC")
    fun getExpensesForDashboard(dashboardId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<SplitEntity>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)
    
    @Query("DELETE FROM splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    @Transaction
    suspend fun saveExpenseWithSplits(expense: ExpenseEntity, splits: List<SplitEntity>) {
        insertExpense(expense)
        deleteSplitsForExpense(expense.id) // Clear old splits if updating
        insertSplits(splits)
    }

    @Query("SELECT * FROM splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpense(expenseId: String): List<SplitEntity>

    @Query("SELECT s.* FROM splits s INNER JOIN expenses e ON s.expenseId = e.id WHERE e.dashboardId = :dashboardId")
    fun getSplitsForDashboard(dashboardId: String): Flow<List<SplitEntity>>

    // Settlement methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Query("SELECT * FROM settlements WHERE dashboardId = :dashboardId ORDER BY createdAt DESC")
    fun getSettlementsForDashboard(dashboardId: String): Flow<List<SettlementEntity>>

    @Query("DELETE FROM settlements WHERE id = :id")
    suspend fun deleteSettlement(id: String)
}
