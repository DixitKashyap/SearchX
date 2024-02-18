package com.dixitkumar.searchxapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface HistoryDao {

    @Query("select * from history")
    fun getAllHistory() : List<History>

    @Insert
    fun addHistoryItem(historyList : History)

    @Update
    fun updateHistoryItem(history: History)

    @Delete
    fun deleteHistoryItem(history: History)
}