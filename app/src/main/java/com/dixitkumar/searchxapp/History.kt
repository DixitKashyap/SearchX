package com.dixitkumar.searchxapp


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
 data class History (
  @PrimaryKey(autoGenerate = true)
  var id : Int = 0,

  @ColumnInfo(name = "title")
  var title : String?,

  @ColumnInfo(name = "url")
  var url : String?
  )

