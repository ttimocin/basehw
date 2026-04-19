package com.taytek.basehw.ui.screens.news

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taytek.basehw.domain.model.DiecastNews
import com.taytek.basehw.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NewsDetailUiState {
    data object Loading : NewsDetailUiState
    data object NotFound : NewsDetailUiState
    data object Error : NewsDetailUiState
    data class Success(val news: DiecastNews) : NewsDetailUiState
}

@HiltViewModel
class NewsDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val newsId: String = checkNotNull(savedStateHandle["newsId"])

    private val _state = MutableStateFlow<NewsDetailUiState>(NewsDetailUiState.Loading)
    val state: StateFlow<NewsDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            newsRepository.getById(newsId)
                .onSuccess { news ->
                    _state.value = if (news != null) {
                        NewsDetailUiState.Success(news)
                    } else {
                        NewsDetailUiState.NotFound
                    }
                }
                .onFailure {
                    _state.value = NewsDetailUiState.Error
                }
        }
    }
}
