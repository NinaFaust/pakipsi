package com.gogatsun.pakipsi.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper (context: Context) :
    SQLiteOpenHelper(
        context,
        DBClass.database_name,
        null,
        DBClass.database_version) //меняем версию после изменения данных
{
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(DBClass.create_table)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(DBClass.sql_delete_table)
        onCreate(db)
    }

}