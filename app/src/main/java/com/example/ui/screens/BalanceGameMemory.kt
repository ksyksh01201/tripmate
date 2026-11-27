package com.example.ui.screens

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import com.google.firebase.auth.FirebaseAuth

/**
 * 밸런스 게임 화면 상태를 앱이 켜져 있는 동안 여행별로 기억해 둡니다.
 * 여행홈에서 일정/가계부 탭으로 갔다가 돌아와도 질문 진행, 답, 결과 화면이 그대로 유지돼요.
 * (앱을 껐다 켜면 Firestore에 저장된 결과에서 다시 불러옵니다.)
 */
internal object BalanceGameMemory {
    private val slots = HashMap<String, MutableState<Any?>>()
    private val objects = HashMap<String, Any>()

    private fun key(tripId: String, name: String): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"
        return "$uid/$tripId/$name"
    }

    /** 여행별로 유지되는 상태. 처음 한 번만 init 값으로 만들어요. */
    @Suppress("UNCHECKED_CAST")
    fun <T> state(tripId: String, name: String, init: () -> T): MutableState<T> =
        slots.getOrPut(key(tripId, name)) { mutableStateOf<Any?>(init()) } as MutableState<T>

    /** 여행별로 유지되는 객체(답 목록 등). */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> obj(tripId: String, name: String, init: () -> T): T =
        objects.getOrPut(key(tripId, name)) { init() } as T

    fun has(tripId: String, name: String): Boolean = slots.containsKey(key(tripId, name))
}

/** 부모의 좌우/위 여백을 무시하고 가장자리까지 꽉 차게 그리기 */
internal fun Modifier.bleed(horizontal: Dp, top: Dp): Modifier = layout { measurable, constraints ->
    val extraX = horizontal.roundToPx()
    val extraTop = top.roundToPx()
    val width = constraints.maxWidth + extraX * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    val height = (placeable.height - extraTop).coerceAtLeast(0)
    layout(constraints.maxWidth, height) { placeable.place(-extraX, -extraTop) }
}
