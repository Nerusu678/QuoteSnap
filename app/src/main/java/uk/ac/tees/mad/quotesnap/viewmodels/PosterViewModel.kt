package uk.ac.tees.mad.quotesnap.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.ac.tees.mad.quotesnap.data.QuoteSnapRepository
import uk.ac.tees.mad.quotesnap.data.local.SavedPoster
import javax.inject.Inject

@HiltViewModel
class PosterViewModel @Inject constructor(
    private val repository: QuoteSnapRepository
) : ViewModel() {

    // Get all saved posters from Room database
    val savedPosters: StateFlow<List<SavedPoster>> = repository.getAllPosters()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Delete a poster
    fun deletePoster(posterId: String) {
        viewModelScope.launch {
            repository.deletePoster(posterId)
        }
    }

    // Sync from Firestore (optional - for when user logs in on new device)
    fun syncFromFirestore() {
        viewModelScope.launch {
            repository.syncPostersFromFirestore()
        }
    }
}