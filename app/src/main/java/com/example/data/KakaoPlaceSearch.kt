package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 카카오 장소 검색 결과 한 건 */
data class KakaoPlace(
    val id: String,
    val name: String,
    val address: String,     // 도로명 주소가 있으면 도로명, 없으면 지번
    val category: String,    // 예: 관광명소, 음식점
    val lat: Double,
    val lng: Double
)

/**
 * 카카오 로컬 "키워드로 장소 검색" API.
 * https://developers.kakao.com/docs/latest/ko/local/dev-guide#search-by-keyword
 * 키는 .env 의 KAKAO_REST_API_KEY 에서 BuildConfig 로 들어옵니다.
 */
object KakaoPlaceSearch {

    val hasKey: Boolean get() = BuildConfig.KAKAO_REST_API_KEY.isNotBlank()

    suspend fun search(query: String, page: Int = 1): List<KakaoPlace> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL("https://dapi.kakao.com/v2/local/search/keyword.json?query=$q&page=$page&size=15")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}")
        }
        try {
            val code = conn.responseCode
            if (code != 200) {
                val body = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw TripException("장소 검색에 실패했어요. (코드 $code) $body")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val docs = json.optJSONArray("documents") ?: return@withContext emptyList()
            (0 until docs.length()).mapNotNull { i ->
                val d = docs.getJSONObject(i)
                val lat = d.optString("y").toDoubleOrNull() ?: return@mapNotNull null
                val lng = d.optString("x").toDoubleOrNull() ?: return@mapNotNull null
                KakaoPlace(
                    id = d.optString("id"),
                    name = d.optString("place_name"),
                    address = d.optString("road_address_name").ifBlank { d.optString("address_name") },
                    category = d.optString("category_group_name")
                        .ifBlank { d.optString("category_name").substringAfterLast(" > ") },
                    lat = lat,
                    lng = lng
                )
            }
        } finally {
            conn.disconnect()
        }
    }
}
