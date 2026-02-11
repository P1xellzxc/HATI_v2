package com.hativ2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hativ2.data.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY name ASC")
    fun getAllPeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getPersonById(id: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deletePerson(id: String)
}
