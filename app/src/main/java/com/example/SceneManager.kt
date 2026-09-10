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
import kotlin.math.sin
import kotlin.math.cos

class SceneManager(private val context: Context, private val engine: Engine) {
    
    var playerNode: ModelNode? = null
        private set

    private var currentAnimationIndex = -1
    private var animationStartTime = 0L

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
            
            node.position = Position(state.positionX, state.positionY, state.positionZ)
            node.rotation = Rotation(0f, state.rotationY, 0f)

            // Animation logic
            val animationIndex = when (state.movementState) {
                MovementState.IDLE -> 0 // Survey
                MovementState.WALKING -> 1 // Walk
                MovementState.RUNNING -> 2 // Run
            }

            if (currentAnimationIndex != animationIndex) {
                currentAnimationIndex = animationIndex
                node.playAnimation(animationIndex, loop = true)
            }
        } catch (e: Exception) {
            // Ignore rendering errors
        }
    }
}
