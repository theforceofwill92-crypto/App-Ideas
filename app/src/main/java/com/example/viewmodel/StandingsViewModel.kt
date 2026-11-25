package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LeagueRepository
import com.example.data.LeagueStandingEntity
import com.example.model.Franchise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StandingsSortMode(val label: String) {
    WIN_PCT("Win %"),
    POINT_DIFF("Point Diff"),
    WINS("Wins")
}

/**
 * Room-backed ViewModel that tracks and exposes live league standings
 * (wins, losses, points for/against, and point differentials) for all franchises.
 */
class StandingsViewModel(
    application: Application,
    private val repository: LeagueRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, LeagueRepository(application))

    private val _sortMode = MutableStateFlow(StandingsSortMode.WIN_PCT)
    val sortMode: StateFlow<StandingsSortMode> = _sortMode.asStateFlow()

    private val _selectedConference = MutableStateFlow<String?>(null)
    val selectedConference: StateFlow<String?> = _selectedConference.asStateFlow()

    private val _fallbackStandings = MutableStateFlow<List<LeagueStandingEntity>>(emptyList())

    val roomStandings: StateFlow<List<LeagueStandingEntity>> = repository.liveStandingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val liveStandings: StateFlow<List<LeagueStandingEntity>> = combine(
        roomStandings,
        _fallbackStandings,
        _sortMode,
        _selectedConference
    ) { persisted, fallback, sort, confFilter ->
        val source = if (persisted.isNotEmpty()) persisted else fallback
        val filtered = if (confFilter.isNullOrBlank()) {
            source
        } else {
            source.filter { it.conferenceName.equals(confFilter, ignoreCase = true) }
        }
        when (sort) {
            StandingsSortMode.WIN_PCT -> filtered.sortedWith(
                compareByDescending<LeagueStandingEntity> { it.winPct }
                    .thenByDescending { it.pointDifferential }
                    .thenByDescending { it.wins }
                    .thenBy { it.teamFullName }
            )
            StandingsSortMode.POINT_DIFF -> filtered.sortedWith(
                compareByDescending<LeagueStandingEntity> { it.pointDifferential }
                    .thenByDescending { it.winPct }
                    .thenBy { it.teamFullName }
            )
            StandingsSortMode.WINS -> filtered.sortedWith(
                compareByDescending<LeagueStandingEntity> { it.wins }
                    .thenByDescending { it.pointDifferential }
                    .thenBy { it.teamFullName }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun syncStandingsFromFranchises(
        franchises: List<Franchise>,
        seasonYear: Int,
        currentWeek: Int
    ) {
        val entities = repository.buildStandingEntities(franchises, seasonYear, currentWeek)
        _fallbackStandings.value = entities
        viewModelScope.launch {
            repository.syncLiveStandings(franchises, seasonYear, currentWeek)
        }
    }

    fun setSortMode(mode: StandingsSortMode) {
        _sortMode.value = mode
    }

    fun setConferenceFilter(conferenceName: String?) {
        _selectedConference.value = conferenceName
    }
}
