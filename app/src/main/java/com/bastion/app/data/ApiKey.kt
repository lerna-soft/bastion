package com.bastion.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKey(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val keyValue: String,
    val created: Long = System.currentTimeMillis()
)
