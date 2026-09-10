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
import io.github.sceneview.environment.Environment
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Size
import io.github.sceneview.model.ModelInstance
import com.google.android.filament.LightManager
import com.google.android.filament.EntityManager

@Composable
fun GameScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { 
        try {
            androidx.room.Room.databaseBuilder(
                context,
                GameDatabase::class.java,
                "game_db"
            ).fallbackToDestructiveMigration().build()
        } catch (e: Exception) {
            null
        }
    }
    
    val viewModel: GameViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return GameViewModel(database?.saveDao()) as T
            }
        }
    )

    val playerState by viewModel.playerState.collectAsState()
    val gameTime by viewModel.gameTime.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val health by viewModel.health.collectAsState()
    val hunger by viewModel.hunger.collectAsState()
    val thirst by viewModel.thirst.collectAsState()
    val stamina by viewModel.stamina.collectAsState()
    
    var isMapVisible by remember { mutableStateOf(false) }
    var isBackpackVisible by remember { mutableStateOf(false) }
    
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    
    var environment by remember { mutableStateOf<Environment?>(null) }
    val defaultEnvironment = remember(environmentLoader) { environmentLoader.createEnvironment() }

    val groundMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF388E3C)) } // Green
    val placeholderMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color.Gray) }

    LaunchedEffect(environmentLoader) {
        try {
            environment = environmentLoader.loadHDREnvironment(
                url = "https://sceneview.github.io/assets/environments/sky_2k.hdr"
            )
        } catch (e: Exception) {
        }
    }

    val groundNode = remember(engine, groundMaterial) {
        CubeNode(
            engine = engine,
            size = Size(5000f, 1f, 5000f),
            materialInstance = groundMaterial
        ).apply {
            position = Position(0f, -0.5f, 0f)
        }
    }

    val playerPlaceholder = remember(engine, placeholderMaterial) {
        CubeNode(
            engine = engine,
            size = Size(0.5f, 1.8f, 0.5f),
            materialInstance = placeholderMaterial
        ).apply {
            position = Position(0f, 0.9f, 0f)
        }
    }

    val sceneManager = remember(engine) { SceneManager(context, engine) }
    val worldManager = remember(engine, materialLoader) { WorldManager(engine, materialLoader) }
    val weatherManager = remember(engine) { WeatherManager(engine) }
    
    val worldNodes = remember(worldManager) { worldManager.generateWorld() }
    val weatherType by weatherManager.currentWeather.collectAsState()
    
    val playerModelInstance = remember { mutableStateOf<ModelInstance?>(null) }
    var isLoadingModel by remember { mutableStateOf(true) }

    LaunchedEffect(modelLoader) {
        isLoadingModel = true
        try {
            playerModelInstance.value = modelLoader.loadModelInstance(
                fileLocation = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/Fox/glTF-Binary/Fox.glb"
            )
        } catch (e: Exception) {
        } finally {
            isLoadingModel = false
        }
    }
    
    LaunchedEffect(playerModelInstance.value) {
        playerModelInstance.value?.let {
            sceneManager.setPlayerModel(it)
        }
    }
    
    val cameraNode = rememberCameraNode(engine).apply {
        position = Position(0f, 4f, 8f)
    }

    val mainLightNode = remember(engine) {
        val entity = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.SUN)
            .intensity(80_000f)
            .direction(0f, -1f, -0.5f) // Pointing down and slightly forward
            .castShadows(true)
            .build(engine, entity)
        LightNode(engine, entity)
    }

    val torchLightNode = remember(engine) {
        val entity = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.SPOT)
            .intensity(0f)
            .spotLightCone(Math.toRadians(30.0).toFloat(), Math.toRadians(50.0).toFloat())
            .falloff(20f)
            .color(1.0f, 0.95f, 0.8f)
            .build(engine, entity)
        LightNode(engine, entity)
    }

    LaunchedEffect(gameTime, isTorchOn, environment) {
        val hours = gameTime / 60
        val isNight = hours < 5 || hours > 19
        
        weatherManager.updateWeather(gameTime, environment)
        
        val sunIntensity = when {
            hours in 6..18 -> 80_000f
            hours == 5 || hours == 19 -> 20_000f
            else -> 1_000f
        }
        engine.lightManager.setIntensity(mainLightNode.entity, sunIntensity)
        
        if (isNight) {
            environment?.indirectLight?.intensity = 500f
        } else {
            environment?.indirectLight?.intensity = 30_000f
        }
        
        engine.lightManager.setIntensity(torchLightNode.entity, if (isTorchOn) 100_000f else 0f)
    }

    // Optimized node management to prevent flickering
    val allNodes = remember(groundNode, mainLightNode, worldNodes, torchLightNode) {
        mutableListOf<io.github.sceneview.node.Node>().apply {
            add(groundNode)
            add(mainLightNode)
            addAll(worldNodes)
            add(torchLightNode)
        }
    }

    val finalNodes = remember(allNodes, sceneManager.playerNode, playerPlaceholder) {
        allNodes.toMutableList().apply {
            add(sceneManager.playerNode ?: playerPlaceholder)
        }
    }

    LaunchedEffect(playerState, viewModel.cameraRotationX, viewModel.cameraRotationY, sceneManager.playerNode) {
        sceneManager.updatePlayer(playerState, viewModel.cameraRotationX, viewModel.cameraRotationY)
        
        if (sceneManager.playerNode == null) {
            playerPlaceholder.position = Position(playerState.positionX, playerState.positionY + 0.9f, playerState.positionZ)
            playerPlaceholder.rotation = Rotation(0f, playerState.rotationY, 0f)
        }

        torchLightNode.position = Position(playerState.positionX, playerState.positionY + 1.2f, playerState.positionZ)
        torchLightNode.rotation = Rotation(0f, viewModel.cameraRotationY, 0f)

        // Robust camera follow
        val cameraDistance = 8f
        val radY = Math.toRadians(viewModel.cameraRotationY.toDouble()).toFloat()
        val radX = Math.toRadians(viewModel.cameraRotationX.toDouble()).toFloat()
        
        val camX = playerState.positionX + sin(radY) * cos(radX) * cameraDistance
        val camY = playerState.positionY - sin(radX) * cameraDistance + 2.5f
        val camZ = playerState.positionZ + cos(radY) * cos(radX) * cameraDistance
        
        cameraNode.position = Position(camX, (camY).coerceAtLeast(1.0f), camZ)
        cameraNode.lookAt(Position(playerState.positionX, playerState.positionY + 1.0f, playerState.positionZ))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            environment = environment ?: defaultEnvironment,
            childNodes = finalNodes
        )

        Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
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

            StatusBarsHUD(
                health = health,
                hunger = hunger,
                thirst = thirst,
                stamina = stamina
            )

            EnvironmentHUD(
                posX = playerState.positionX,
                posZ = playerState.positionZ,
                weather = weatherType,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
            )

            Column(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalAlignment = Alignment.End
            ) {
                CompassHUD(rotationY = viewModel.cameraRotationY)
                TimeHUD(gameTime = gameTime)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 48.dp, start = 16.dp, end = 16.dp)
            ) {
                Joystick(
                    modifier = Modifier.align(Alignment.BottomStart),
                    onMove = { x, z ->
                        viewModel.updateJoystickInput(x, z)
                    }
                )

                Column(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionButton(
                        icon = if (isTorchOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                        onClick = { viewModel.toggleTorch() },
                        active = isTorchOn
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
                        onClick = { isBackpackVisible = true }
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

            if (isBackpackVisible) {
                BackpackOverlay(
                    onClose = { isBackpackVisible = false }
                )
            }
        }

        if (isLoadingModel && playerModelInstance.value == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading Wilderness...",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            viewModel.saveGame()
        }
    }
}
