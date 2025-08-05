package com.kuehlconsulting.johnbirchsociety.data.db

import android.content.Context

fun getDownloadDao(context: Context): DownloadDao {
    return AppDatabase.getInstance(context).downloadDao()
}