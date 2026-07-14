package com.example.vpnclient.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val protocol: String,
    val groupId: String?,
    val order: Int,
    val json: String   // сериализованный Profile целиком (см. ProfileJson)
)

@Entity(tableName = "subscription_groups")
data class SubscriptionGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val lastUpdated: Long?,
    val autoUpdateHours: Int
)
