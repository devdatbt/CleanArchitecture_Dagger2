package com.example.apper.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.apper.ui.event.EventNote
import com.example.apper.ui.base.BaseViewModel
import com.example.apper.ui.event.EventShowMsg
import com.example.apper.usecase.AppUseCase
import com.example.apper.utils.Resource
import com.example.domain.model.Currency
import com.example.domain.model.Note
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class NoteViewModel @Inject constructor(private val appUseCase: AppUseCase) : BaseViewModel() {

    private val TAG = NoteViewModel::class.java.simpleName

    private val _statusGetCurrency: MutableLiveData<Resource<Currency>> = MutableLiveData()
    val statusGetCurrencyApi: LiveData<Resource<Currency>>
        get() = _statusGetCurrency

    private val _statusMessage = MutableLiveData<EventShowMsg<String>>()
    val statusMessage: LiveData<EventShowMsg<String>>
        get() = _statusMessage

    val listNoteShareIn = appUseCase.getNoteListsUseCase.invoke()
        .catch {
            this.emit(emptyList())
        }.onCompletion { isSuccessfully ->
            if (isSuccessfully == null)
                Log.e(TAG, "Success ${Thread.currentThread().name}")
            else
                Log.e(TAG, "Failed: $isSuccessfully")
        }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))

    /** Flow to StateFlow */
    private var _listNoteStateIn = MutableStateFlow<List<Note>>(emptyList())
    val listNoteStateIn get() = _listNoteStateIn.asStateFlow()

    init {
        getCurrencyFromServer()
        getAllNoteFromDB()
    }

    private fun getAllNoteFromDB() {
        launchDataLoad {
            appUseCase.getNoteListsUseCase.invoke()
                .onEach {
                    _listNoteStateIn.value = it
                }
                .catch { emit(emptyList()) }
                .onCompletion { isSuccessfully ->
                    if (isSuccessfully == null)
                        Log.e(TAG, "getAllNoteFromDB Success ${Thread.currentThread().name}")
                    else
                        Log.e(TAG, "getAllNoteFromDB Failed: $isSuccessfully")
                }.stateIn(viewModelScope)
        }
    }

    private fun insertNote(note: Note) {
        launchDataLoad {
            appUseCase.addNoteUseCase.invoke(note)
        }.invokeOnCompletion {
            _statusMessage.value = EventShowMsg("Add successfully")
        }
    }

    private fun deleteNote(note: Note) {
        launchDataLoad {
            appUseCase.deleteNoteUseCase.invoke(note)
        }.invokeOnCompletion {
            _statusMessage.value = EventShowMsg("Delete successfully")
        }
    }

    private fun updateNote(title: String, content: String, time: Long) {
        launchDataLoad {
            appUseCase.updateNoteUseCase.invoke(title = title, content = content, time = time)
        }.invokeOnCompletion {
            _statusMessage.value = EventShowMsg("Update successfully")
        }
    }

    private fun getCurrencyFromServer() {
        _statusGetCurrency.value = Resource.loading(null)
        launchDataLoad {
            appUseCase.getCurrencyUseCase.invoke().let {
                if (it.success)
                    _statusGetCurrency.value = Resource.success(it)
                else
                    _statusGetCurrency.value = Resource.error(null, "Error success: ${it.success}")
            }
        }
    }

    fun searchListNoteWith(searchValue: String, listFilter: List<Note>): List<Note> {
        return if (searchValue.isEmpty()) {
            listFilter
        } else {
            listFilter.filter { note -> note.getTitleContainsWord(searchValue) }
        }
    }

    fun onEventNote(event: EventNote) {
        when (event) {
            is EventNote.EventInsertNote -> {
                insertNote(event.note)
            }
            is EventNote.EventUpdateNote -> {
                updateNote(event.title, event.content, event.timestamp)
            }
            is EventNote.EventDeleteNote -> {
                deleteNote(event.note)
            }
        }
    }
}