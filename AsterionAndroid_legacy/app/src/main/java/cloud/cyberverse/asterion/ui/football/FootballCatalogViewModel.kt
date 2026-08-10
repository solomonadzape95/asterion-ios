package cloud.cyberverse.asterion.ui.football

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.FootballMatch
import cloud.cyberverse.asterion.ui.common.userMessage
import cloud.cyberverse.asterion.data.remote.FootballApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FootballCatalogState {
    data object Loading : FootballCatalogState
    data class Loaded(val matches: List<FootballMatch>) : FootballCatalogState
    data class Error(val message: String) : FootballCatalogState
}

class FootballCatalogViewModel(private val api: FootballApiService) : ViewModel() {
    private val _state = MutableStateFlow<FootballCatalogState>(FootballCatalogState.Loading)
    val state: StateFlow<FootballCatalogState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = FootballCatalogState.Loading
        viewModelScope.launch {
            _state.value = try {
                FootballCatalogState.Loaded(api.schedule().data)
            } catch (error: Exception) {
                FootballCatalogState.Error(error.userMessage())
            }
        }
    }
}
