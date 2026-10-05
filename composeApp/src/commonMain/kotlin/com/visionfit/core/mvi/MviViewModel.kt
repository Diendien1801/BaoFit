package com.visionfit.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base class for unidirectional data flow:
 *
 * - [state] is the single source of truth the UI renders.
 * - [onEvent] is the only entry point for user intents.
 * - [effects] carries one-shot events (navigation, messages) that must not be replayed
 *   after a configuration change, so they go through a [Channel] instead of the state.
 */
abstract class MviViewModel<State, Event, Effect>(initialState: State) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    protected val currentState: State
        get() = _state.value

    abstract fun onEvent(event: Event)

    protected fun updateState(reducer: State.() -> State) {
        _state.update(reducer)
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
