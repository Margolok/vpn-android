package com.example.vpnclient.data.repository

import com.example.vpnclient.data.db.ProfileDao
import com.example.vpnclient.data.db.ProfileEntity
import com.example.vpnclient.data.db.ProfileJson
import com.example.vpnclient.data.db.SubscriptionGroupEntity
import com.example.vpnclient.data.model.Profile
import com.example.vpnclient.data.model.SubscriptionGroup
import com.example.vpnclient.data.parser.ConfigLinkParser
import com.example.vpnclient.data.parser.SubscriptionFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProfileRepository(private val dao: ProfileDao) {

    fun observeProfiles(): Flow<List<Profile>> =
        dao.observeAll().map { list -> list.map { ProfileJson.fromJson(it.json) } }

    fun observeGroups(): Flow<List<SubscriptionGroup>> =
        dao.observeGroups().map { list ->
            list.map { SubscriptionGroup(it.id, it.name, it.url, it.lastUpdated, it.autoUpdateHours) }
        }

    /** Добавляет один профиль по вставленной ссылке (vmess/vless/trojan/ss) или WireGuard-конфигу. */
    suspend fun addFromLink(rawInput: String): Profile = withContext(Dispatchers.IO) {
        val profile = ConfigLinkParser.parse(rawInput)
        dao.upsert(profile.toEntity())
        profile
    }

    suspend fun addManual(profile: Profile) = withContext(Dispatchers.IO) {
        dao.upsert(profile.toEntity())
    }

    suspend fun delete(profileId: String) = withContext(Dispatchers.IO) {
        dao.delete(profileId)
    }

    /** Добавляет подписку и сразу загружает список профилей из неё. */
    suspend fun addSubscription(name: String, url: String): List<Profile> = withContext(Dispatchers.IO) {
        val profiles = SubscriptionFetcher.fetchAndParse(url)
        val group = SubscriptionGroup(name = name, url = url, lastUpdated = System.currentTimeMillis())
        dao.upsertGroup(SubscriptionGroupEntity(group.id, group.name, group.url, group.lastUpdated, group.autoUpdateHours))
        val withGroup = profiles.mapIndexed { index, p -> p.copy(groupId = group.id, order = index) }
        dao.upsertAll(withGroup.map { it.toEntity() })
        withGroup
    }

    /** Перечитывает подписку и заменяет старые профили этой группы новыми. */
    suspend fun refreshSubscription(group: SubscriptionGroup): List<Profile> = withContext(Dispatchers.IO) {
        val profiles = SubscriptionFetcher.fetchAndParse(group.url)
        dao.deleteByGroup(group.id)
        val withGroup = profiles.mapIndexed { index, p -> p.copy(groupId = group.id, order = index) }
        dao.upsertAll(withGroup.map { it.toEntity() })
        dao.upsertGroup(SubscriptionGroupEntity(group.id, group.name, group.url, System.currentTimeMillis(), group.autoUpdateHours))
        withGroup
    }

    private fun Profile.toEntity() = ProfileEntity(
        id = id, name = name, protocol = protocol.name, groupId = groupId, order = order,
        json = ProfileJson.toJson(this)
    )
}
