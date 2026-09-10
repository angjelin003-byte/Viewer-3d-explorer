package com.example

import android.content.Context
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.Node
import io.github.sceneview.environment.Environment
import io.github.sceneview.environment.loadEnvironment
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Direction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SceneManager(private val context: Context) {
    val sceneView = SceneView(context)
    
    private val playerNode = Node(engine = sceneView.engine)
    
    private val torchLight = Node(engine = sceneView.engine)
    
    init {
        playerNode.position = Position(x = 0f, y = 0f, z = 0f)
        sceneView.addChild(playerNode)
        
        // Setup fog for atmosphere
        // sceneView.fog = Fog(...)
    }
    
    fun updatePlayer(state: PlayerState, cameraRotX: Float, cameraRotY: Float) {
        // Animation: simple bobbing when walking/running
        val time = System.currentTimeMillis() / 1000f
        val bob = if (state.movementState != MovementState.IDLE) {
            val freq = if (state.movementState == MovementState.RUNNING) 10f else 5f
            kotlin.math.sin(time * freq) * 0.05f
        } else 0f
        
        playerNode.position = Position(state.positionX, state.positionY + bob, state.positionZ)
        playerNode.rotation = Rotation(0f, state.rotationY, 0f)
        
        // Update camera position to follow player (Third Person)
        val cameraDistance = 4f
        val radY = Math.toRadians(cameraRotY.toDouble()).toFloat()
        val radX = Math.toRadians(cameraRotX.toDouble()).toFloat()
        
        val camX = state.positionX + sin(radY) * cos(radX) * cameraDistance
        val camY = state.positionY - sin(radX) * cameraDistance + 1.5f // Height offset
        val camZ = state.positionZ + cos(radY) * cos(radX) * cameraDistance
        
        sceneView.cameraNode.position = Position(camX, camY, camZ)
        sceneView.cameraNode.lookAt(playerNode)
    }
    
    fun updateTime(hour: Int) {
        // Adjust directional light (Sun) based on hour
        val angle = (hour - 6) * 15f // 6 AM is 0 degrees
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val sunDir = Direction(cos(rad), -sin(rad), 0f)
        // sceneView.mainLightNode?.direction = sunDir
    }
    
    private fun sin(rad: Float) = kotlin.math.sin(rad.toDouble()).toFloat()
    private fun cos(rad: Float) = kotlin.math.cos(rad.toDouble()).toFloat()
}
