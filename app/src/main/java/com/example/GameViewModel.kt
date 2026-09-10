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

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn = _isTorchOn.asStateFlow()

    private val _health = MutableStateFlow(100f)
    val health = _health.asStateFlow()

    private val _hunger = MutableStateFlow(100f)
    val hunger = _hunger.asStateFlow()

    private val _thirst = MutableStateFlow(100f)
    val thirst = _thirst.asStateFlow()

    private val _stamina = MutableStateFlow(100f)
    val stamina = _stamina.asStateFlow()

    private val _gameTime = MutableStateFlow(480) // 08:00
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
                    _gameTime.value = data.hour * 60 + data.minute
                    _stamina.value = data.stamina
                }
            } catch (e: Exception) {
                // Ignore load errors
            }
            updateGameSystems()
        }
    }

    fun saveGame() {
        viewModelScope.launch {
            try {
                val p = _playerState.value
                val t = _gameTime.value
                val hour = t / 60
                val minute = t % 60
                saveDao?.insertSaveData(
                    SaveData(
                        posX = p.positionX,
                        posY = p.positionY,
                        posZ = p.positionZ,
                        rotY = p.rotationY,
                        hour = hour,
                        minute = minute,
                        stamina = _stamina.value,
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
        val staminaDelta = when (state) {
            MovementState.RUNNING -> -0.5f
            MovementState.WALKING -> 0.05f
            MovementState.IDLE -> 0.2f
        }
        val newStamina = (_stamina.value + staminaDelta).coerceIn(0f, 100f)
        _stamina.value = newStamina
        _playerState.value = _playerState.value.copy(
            stamina = newStamina,
            isTired = newStamina < 20f
        )
    }

    fun setMovementState(state: MovementState) {
        _playerState.value = _playerState.value.copy(movementState = state)
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
        _playerState.value = _playerState.value.copy(isTorchOn = _isTorchOn.value)
    }

    fun deployTent() {
        _playerState.value = _playerState.value.copy(isTentDeployed = true)
    }

    fun advanceTime(minutes: Int) {
        _gameTime.value = (_gameTime.value + minutes) % 1440
    }

    private fun updateGameSystems() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                
                // Update Time
                _gameTime.value = (_gameTime.value + 1) % 1440
                
                // Hunger and Thirst decay
                _hunger.value = (_hunger.value - 0.05f).coerceIn(0f, 100f)
                _thirst.value = (_thirst.value - 0.08f).coerceIn(0f, 100f)
                
                // Health decay if starving or dehydrated
                if (_hunger.value <= 0f || _thirst.value <= 0f) {
                    _health.value = (_health.value - 0.5f).coerceIn(0f, 100f)
                }
                
                // Stamina regeneration if not moving
                if (_playerState.value.movementState == MovementState.IDLE && _stamina.value < 100f) {
                    _stamina.value = (_stamina.value + 0.2f).coerceIn(0f, 100f)
                }
            }
        }
    }
}
