package com.example

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.cos

import io.github.sceneview.Scene
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.math.Position
import io.github.sceneview.rememberMainLightNode

@Composable
fun GameScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { 
        androidx.room.Room.databaseBuilder(
            context,
            GameDatabase::class.java,
            "game_db"
        ).build()
    }
    
    val viewModel: GameViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return GameViewModel(database.saveDao()) as T
            }
        }
    )

    val playerState by viewModel.playerState.collectAsState()
    val gameTime by viewModel.gameTime.collectAsState()
    var isMapVisible by remember { mutableStateOf(false) }
    
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    
    val sceneManager = remember { SceneManager(context, modelLoader) }
    
    val cameraNode = rememberCameraNode(engine).apply {
        position = Position(0f, 1.5f, 4f)
    }

    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 100_000f
    }

    LaunchedEffect(playerState, viewModel.cameraRotationX, viewModel.cameraRotationY) {
        sceneManager.updatePlayer(playerState, viewModel.cameraRotationX, viewModel.cameraRotationY)
        
        // Update camera position to follow player (Third Person)
        val cameraDistance = 4f
        val radY = Math.toRadians(viewModel.cameraRotationY.toDouble()).toFloat()
        val radX = Math.toRadians(viewModel.cameraRotationX.toDouble()).toFloat()
        
        val camX = playerState.positionX + sin(radY) * cos(radX) * cameraDistance
        val camY = playerState.positionY - sin(radX) * cameraDistance + 1.5f // Height offset
        val camZ = playerState.positionZ + cos(radY) * cos(radX) * cameraDistance
        
        cameraNode.position = Position(camX, camY, camZ)
        cameraNode.lookAt(sceneManager.playerNode.position)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 3D Scene
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            childNodes = listOf(sceneManager.playerNode, sceneManager.groundNode, mainLightNode)
        )

        // Overlay UI
        Box(modifier = Modifier.fillMaxSize()) {
            // Interaction Area for Camera Control (Background of UI)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            viewModel.cameraRotationY -= dragAmount.x * 0.5f
                            viewModel.cameraRotationX = (viewModel.cameraRotationX - dragAmount.y * 0.5f).coerceIn(-60f, 20f)
                            change.consume()
                        }
                    }
            )

            // Top HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Compass(rotationY = playerState.rotationY)
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%02d:00", gameTime.hour),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    StaminaBar(stamina = playerState.stamina)
                }
            }

            // Controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 48.dp, start = 16.dp, end = 16.dp)
            ) {
                // Left Joystick
                Joystick(
                    modifier = Modifier.align(Alignment.BottomStart),
                    onMove = { x, z ->
                        if (x != 0f || z != 0f) {
                            viewModel.setMovementState(if (playerState.stamina > 20) MovementState.WALKING else MovementState.WALKING) // Logic handled in VM
                            // In a real implementation, rotation would be derived from movement or camera
                            viewModel.updatePosition(x, z, viewModel.cameraRotationY)
                        } else {
                            viewModel.setMovementState(MovementState.IDLE)
                        }
                    }
                )

                // Right Action Buttons
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionButton(
                        icon = if (playerState.isTorchOn) Icons.Default.Lightbulb else Icons.Default.LightbulbCircle,
                        onClick = { viewModel.toggleTorch() },
                        active = playerState.isTorchOn
                    )
                    
                    ActionButton(
                        icon = Icons.Default.DirectionsRun,
                        onClick = { 
                            if (playerState.movementState == MovementState.RUNNING) {
                                viewModel.setMovementState(MovementState.WALKING)
                            } else {
                                viewModel.setMovementState(MovementState.RUNNING)
                            }
                        },
                        active = playerState.movementState == MovementState.RUNNING
                    )
                    
                    ActionButton(
                        icon = Icons.Default.Home,
                        onClick = { viewModel.deployTent() },
                        active = playerState.isTentDeployed
                    )
                    
                    ActionButton(
                        icon = Icons.Default.Map,
                        onClick = { isMapVisible = true }
                    )
                    
                    ActionButton(
                        icon = Icons.Default.Backpack,
                        onClick = { /* Open Backpack */ }
                    )
                }
            }
            
            if (isMapVisible) {
                MapOverlay(
                    playerPosX = playerState.positionX,
                    playerPosZ = playerState.positionZ,
                    onClose = { isMapVisible = false }
                )
            }
        }
    }
    
    // Auto-save or periodic updates
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // Save every 10 seconds
            viewModel.saveGame()
        }
    }
}
