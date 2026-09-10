package com.example

import com.google.android.filament.Engine
import io.github.sceneview.environment.Environment
import io.github.sceneview.node.LightNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WeatherType {
    CLEAR, FOGGY, RAINY
}

class WeatherManager(private val engine: Engine) {
    
    private val _currentWeather = MutableStateFlow(WeatherType.CLEAR)
    val currentWeather = _currentWeather.asStateFlow()
    
    fun updateWeather(gameTime: Int, environment: Environment?) {
        // Simple logic: Foggy early morning, clear day
        val hours = gameTime / 60
        val newWeather = when {
            hours in 5..7 -> WeatherType.FOGGY
            hours in 14..16 && (gameTime % 300 < 60) -> WeatherType.RAINY // Occasional rain
            else -> WeatherType.CLEAR
        }
        
        if (newWeather != _currentWeather.value) {
            _currentWeather.value = newWeather
            applyWeatherEffects(newWeather, environment)
        }
    }
    
    private fun applyWeatherEffects(weather: WeatherType, environment: Environment?) {
        // In SceneView 2.2.1, we can adjust indirect light and potentially fog if available
        // For now, we'll adjust indirect light intensity to simulate overcast/fog
        environment?.indirectLight?.let {
            when (weather) {
                WeatherType.CLEAR -> it.intensity = 30_000f
                WeatherType.FOGGY -> it.intensity = 10_000f
                WeatherType.RAINY -> it.intensity = 5_000f
            }
        }
    }
}
