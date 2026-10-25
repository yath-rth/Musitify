package com.yatharth.musitify.data.viewmodel

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.yatharth.musitify.data.repository.StatsComputer
import com.yatharth.musitify.data.repository.StatsRepository
import com.yatharth.musitify.data.repository.StatsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)
    val songCount = statsRepository.totalCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0L
    )

    val stats: StateFlow<StatsSnapshot> = combine(
        statsRepository.sessions,
        refreshTrigger
    ) { sessions, _ ->
        StatsComputer.compute(sessions, System.currentTimeMillis())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StatsSnapshot()
    )

    init {
        viewModelScope.launch {
            // Fold the legacy timeHeard counters into the session history once.
            statsRepository.backfillLegacyListenTime()
            refresh()
        }
    }

    /** Forces a recompute so time windows refresh when the screen reopens. */
    fun refresh() {
        refreshTrigger.value++
    }
}