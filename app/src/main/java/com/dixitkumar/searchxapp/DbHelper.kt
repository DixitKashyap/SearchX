package com.dixitkumar.searchxapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = arrayOf(History::class), exportSchema = false, version = 1)
abstract class  DbHelper() : RoomDatabase() {

    companion object{
        private  var DB_NAME : String = "historyDb"
        private  var instance : DbHelper? = null

        @Synchronized
        public fun getDb(context: Context) : DbHelper{
            if(instance == null){
                instance  = Room.databaseBuilder(context, DbHelper::class.java,DB_NAME)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()

            }
            return instance as DbHelper
        }
    }


    public abstract val historyDao : HistoryDao
}