package com.gogatsun.pakipsi.db

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.icu.text.CaseMap
import android.widget.Toast

class DbData (_date : String, _coord: String){
    var date : String = ""
    var coord : String = ""

    init {
        date = _date
        coord = _coord
    }
}

public class DBManager(val context: Context){
    val DBHelper = DBHelper(context)
    var db: SQLiteDatabase? = null

    fun openDB(){
        db = DBHelper.writableDatabase
        //Toast.makeText(context, "База открыта", Toast.LENGTH_LONG).show()
    }

    fun insertToDB(date: String, coord: String){
        val values = ContentValues().apply {
            put("date", date)
            put("coord", coord)
        }
        db?.insert(DBClass.table_name, null, values)
    }

    @SuppressLint("Range")
    fun readDBData() : ArrayList<DbData>{
        val dataList = ArrayList<DbData>()
        val cursor = db?.query(DBClass.table_name, null, null, null, null, null, null, null)


        while (cursor?.moveToNext()!!){
            val date = cursor.getString(cursor.getColumnIndex("date"))
            val coord = cursor.getString(cursor.getColumnIndex("coord"))
            dataList.add(DbData(date, coord))
        }


        cursor.close()
        return dataList
    }

    fun clearDB(){
        db?.execSQL("delete from ${DBClass.table_name}")
    }

    fun closeDB(){
        DBHelper.close()
    }
}