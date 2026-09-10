package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

enum class MovementState {
    IDLE, WALKING, RUNNING
}

data class PlayerState(
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val positionZ: Float = 0f,
    val rotationY: Float = 0f,
    val stamina: Float = 100f,
    val isTired: Boolean = false,
    val movementState: MovementState = MovementState.IDLE,
    val isTorchOn: Boolean = false,
    val isTentDeployed: Boolean = false
)

data class GameTime(
    val hour: Int = 12,
    val minute: Int = 0
)

class GameViewModel(private val saveDao: SaveDao? = null) : ViewModel() {
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()

    private val _gameTime = MutableStateFlow(GameTime())
    val gameTime = _gameTime.asStateFlow()

    var cameraRotationY by mutableStateOf(0f)
    var cameraRotationX by mutableStateOf(-20f) // Initial tilt down

    init {
        loadGame()
    }

    private fun loadGame() {
        viewModelScope.launch {
            try {
                saveDao?.getSaveData()?.let { data ->
                    _playerState.value = PlayerState(
                        positionX = data.posX,
                        positionY = data.posY,
                        positionZ = data.posZ,
                        rotationY = data.rotY,
                        stamina = data.stamina,
                        isTentDeployed = data.isTentDeployed
                    )
                    _gameTime.value = GameTime(hour = data.hour, minute = data.minute)
                }
            } catch (e: Exception) {
                // Ignore load errors
            }
        }
    }

    fun saveGame() {
        viewModelScope.launch {
            try {
                val p = _playerState.value
                val t = _gameTime.value
                saveDao?.insertSaveData(
                    SaveData(
                        posX = p.positionX,
                        posY = p.positionY,
                        posZ = p.positionZ,
                        rotY = p.rotationY,
                        hour = t.hour,
                        minute = t.minute,
                        stamina = p.stamina,
                        isTentDeployed = p.isTentDeployed
                    )
                )
            } catch (e: Exception) {
                // Ignore save errors
            }
        }
    }

    fun updatePosition(dx: Float, dz: Float, rotation: Float) {
        val currentState = _playerState.value
        if (currentState.stamina <= 0 && currentState.movementState == MovementState.RUNNING) {
            // Force walk if out of stamina
            return
        }

        val speed = when (currentState.movementState) {
            MovementState.RUNNING -> 0.15f
            MovementState.WALKING -> 0.05f
            else -> 0f
        }

        // Apply rotation to movement
        val rad = Math.toRadians(rotation.toDouble()).toFloat()
        val moveX = (dx * Math.cos(rad.toDouble()) - dz * Math.sin(rad.toDouble())).toFloat() * speed
        val moveZ = (dx * Math.sin(rad.toDouble()) + dz * Math.cos(rad.toDouble())).toFloat() * speed

        _playerState.value = currentState.copy(
            positionX = currentState.positionX + moveX,
            positionZ = currentState.positionZ + moveZ,
            rotationY = rotation
        )

        updateStamina(currentState.movementState)
    }

    private fun updateStamina(state: MovementState) {
        val currentState = _playerState.value
        val staminaDelta = when (state) {
            MovementState.RUNNING -> -0.5f
            MovementState.WALKING -> 0.05f
            MovementState.IDLE -> 0.2f
        }
        val newStamina = (currentState.stamina + staminaDelta).coerceIn(0f, 100f)
        _playerState.value = currentState.copy(
            stamina = newStamina,
            isTired = newStamina < 20f
        )
    }

    fun setMovementState(state: MovementState) {
        _playerState.value = _playerState.value.copy(movementState = state)
    }

    fun toggleTorch() {
        _playerState.value = _playerState.value.copy(isTorchOn = !_playerState.value.isTorchOn)
    }

    fun deployTent() {
        _playerState.value = _playerState.value.copy(isTentDeployed = true)
    }

    fun advanceTime(hours: Int) {
        val currentTime = _gameTime.value
        var newHour = currentTime.hour + hours
        if (newHour >= 24) newHour -= 24
        _gameTime.value = currentTime.copy(hour = newHour)
    }
}
