package com.example.vpnclient.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY `order` ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ProfileEntity>)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM profiles WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)

    @Query("SELECT * FROM subscription_groups")
    fun observeGroups(): Flow<List<SubscriptionGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: SubscriptionGroupEntity)

    @Query("DELETE FROM subscription_groups WHERE id = :id")
    suspend fun deleteGroup(id: String)
}
