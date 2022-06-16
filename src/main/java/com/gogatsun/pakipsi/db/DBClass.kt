package com.gogatsun.pakipsi.db

import android.provider.BaseColumns

object DBClass {
    const val table_name = "points"

    const val database_version = 1
    const val database_name = "data.db"

    const val create_table = "create table if not exists $table_name " +
            "(" +
            "${BaseColumns._ID} integer primary key, " +
            "date text, " +
            "coord text " +
            ")"
    const val sql_delete_table = "drop table if exists $table_name"
}