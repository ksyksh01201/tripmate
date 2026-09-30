package com.example.service

/**
 * Tmap 등 외부 지도 API 연동을 위한 인터페이스 및 Mock 구현체.
 * 추후 실제 Tmap API Key 발급 시 TmapMapService로 쉽게 교체할 수 있도록 설계되었습니다.
 */
data class LatLng(val latitude: Double, val longitude: Double)

data class RouteStep(
    val fromPlaceName: String,
    val toPlaceName: String,
    val distanceKm: Double,
    val estimatedMinutes: Int
)

data class RouteResult(
    val totalDistanceKm: Double,
    val totalDurationMinutes: Int,
    val steps: List<RouteStep>,
    val waypoints: List<LatLng>
)

interface MapService {
    fun calculateRoute(places: List<Pair<String, LatLng>>): RouteResult
}

class MockMapService : MapService {
    override fun calculateRoute(places: List<Pair<String, LatLng>>): RouteResult {
        if (places.size <= 1) {
            return RouteResult(0.0, 0, emptyList(), places.map { it.second })
        }

        val steps = mutableListOf<RouteStep>()
        var totalDist = 0.0
        var totalMin = 0

        for (i in 0 until places.size - 1) {
            val from = places[i]
            val to = places[i + 1]
            // Mock realistic distance between Busan travel spots (approx 4~12km)
            val dist = 4.5 + (i * 2.3) % 6.0
            val min = (dist * 2.8).toInt() + 10
            steps.add(RouteStep(from.first, to.first, Math.round(dist * 10) / 10.0, min))
            totalDist += dist
            totalMin += min
        }

        return RouteResult(
            totalDistanceKm = Math.round(totalDist * 10) / 10.0,
            totalDurationMinutes = totalMin,
            steps = steps,
            waypoints = places.map { it.second }
        )
    }
}
