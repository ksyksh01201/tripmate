package com.example.data.repository

import android.content.Context
import com.example.data.model.Participant
import com.example.data.model.Trip
import com.example.data.model.TripThemeType
import org.json.JSONArray
import org.json.JSONObject

/**
 * 개발용 로컬 영속 저장소 (Local Storage)
 * Firebase 연동 전까지 브라우저/디바이스의 SharedPreferences에 사용자별 여행 데이터를 저장합니다.
 * 새로고침하거나 다시 로그인해도 데이터가 영속적으로 유지됩니다.
 */
class TripRepository(context: Context) {
    private val prefs = context.getSharedPreferences("tripmate_local_storage", Context.MODE_PRIVATE)

    fun getTrips(userEmail: String): List<Trip> {
        val key = getStorageKey(userEmail)
        val jsonString = prefs.getString(key, null) ?: return emptyList()
        val trips = mutableListOf<Trip>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                trips.add(deserializeTrip(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return trips
    }

    fun saveTrip(userEmail: String, trip: Trip) {
        val currentTrips = getTrips(userEmail).toMutableList()
        currentTrips.removeAll { it.id == trip.id }
        currentTrips.add(0, trip)
        saveAllTrips(userEmail, currentTrips)
    }

    fun deleteTrip(userEmail: String, tripId: String) {
        val currentTrips = getTrips(userEmail).toMutableList()
        currentTrips.removeAll { it.id == tripId }
        saveAllTrips(userEmail, currentTrips)
    }

    fun clearTrips(userEmail: String) {
        prefs.edit().remove(getStorageKey(userEmail)).apply()
    }

    private fun saveAllTrips(userEmail: String, trips: List<Trip>) {
        val jsonArray = JSONArray()
        for (trip in trips) {
            jsonArray.put(serializeTrip(trip))
        }
        prefs.edit().putString(getStorageKey(userEmail), jsonArray.toString()).apply()
    }

    private fun getStorageKey(userEmail: String): String {
        val sanitized = if (userEmail.isBlank()) "default_user" else userEmail.trim().lowercase().replace(".", "_")
        return "trips_$sanitized"
    }

    private fun serializeTrip(trip: Trip): JSONObject {
        val obj = JSONObject()
        obj.put("id", trip.id)
        obj.put("title", trip.title)
        obj.put("destination", trip.destination)
        obj.put("startDate", trip.startDate)
        obj.put("endDate", trip.endDate)
        obj.put("memberCount", trip.memberCount)
        if (trip.dDay != null) {
            obj.put("dDay", trip.dDay)
        }
        obj.put("isCompleted", trip.isCompleted)
        obj.put("isPrimaryFeatured", trip.isPrimaryFeatured)
        obj.put("themeType", trip.themeType.name)
        obj.put("isMock", trip.isMock)

        val participantsArray = JSONArray()
        for (p in trip.participants) {
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("name", p.name)
            pObj.put("initial", p.initial)
            pObj.put("backgroundColorHex", p.backgroundColorHex)
            participantsArray.put(pObj)
        }
        obj.put("participants", participantsArray)
        return obj
    }

    private fun deserializeTrip(obj: JSONObject): Trip {
        val participants = mutableListOf<Participant>()
        if (obj.has("participants")) {
            val pArray = obj.getJSONArray("participants")
            for (i in 0 until pArray.length()) {
                val pObj = pArray.getJSONObject(i)
                participants.add(
                    Participant(
                        id = pObj.optString("id", "p_$i"),
                        name = pObj.optString("name", "참여자"),
                        initial = pObj.optString("initial", "참"),
                        backgroundColorHex = pObj.optLong("backgroundColorHex", 0xFF1E2B3E)
                    )
                )
            }
        }

        val themeName = obj.optString("themeType", TripThemeType.Generic.name)
        val themeType = try {
            TripThemeType.valueOf(themeName)
        } catch (_: Exception) {
            TripThemeType.Generic
        }

        val dDay = if (obj.has("dDay")) obj.getInt("dDay") else null

        return Trip(
            id = obj.optString("id"),
            title = obj.optString("title"),
            destination = obj.optString("destination", ""),
            startDate = obj.optString("startDate"),
            endDate = obj.optString("endDate"),
            memberCount = obj.optInt("memberCount", 1),
            dDay = dDay,
            participants = participants,
            isCompleted = obj.optBoolean("isCompleted", false),
            isPrimaryFeatured = obj.optBoolean("isPrimaryFeatured", false),
            themeType = themeType,
            isMock = obj.optBoolean("isMock", false)
        )
    }
}
