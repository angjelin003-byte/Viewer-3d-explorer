package com.example

import android.content.Context
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CubeNode
import io.github.sceneview.math.Size
import com.google.android.filament.Engine

class SceneManager(private val context: Context, private val engine: Engine) {
    
    var playerNode: ModelNode? = null
        private set

    fun setPlayerModel(modelInstance: io.github.sceneview.model.ModelInstance) {
        try {
            playerNode = ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 1.0f
            ).apply {
                position = Position(0f, 0f, 0f)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    fun updatePlayer(state: PlayerState, cameraRotX: Float, cameraRotY: Float) {
        try {
            val node = playerNode ?: return
            
            // Animation: simple bobbing when walking/running
            val time = System.currentTimeMillis() / 1000f
            val bob = if (state.movementState != MovementState.IDLE) {
                val freq = if (state.movementState == MovementState.RUNNING) 10f else 5f
                kotlin.math.sin(time * freq) * 0.05f
            } else 0f
            
            node.position = Position(state.positionX, state.positionY + bob, state.positionZ)
            node.rotation = Rotation(0f, state.rotationY, 0f)
        } catch (e: Exception) {
            // Ignore rendering errors
        }
    }
    
    private fun sin(rad: Float) = kotlin.math.sin(rad.toDouble()).toFloat()
    private fun cos(rad: Float) = kotlin.math.cos(rad.toDouble()).toFloat()
}
