package com.example

import com.google.android.filament.Engine
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.Node
import java.util.*

class WorldManager(private val engine: Engine) {

    private val random = Random(42) // Deterministic seed for simple procedural generation
    
    fun generateWorld(): List<Node> {
        val nodes = mutableListOf<Node>()
        
        // Add more "Trees" and vary them
        for (i in 0 until 100) {
            val x = random.nextFloat() * 400f - 200f
            val z = random.nextFloat() * 400f - 200f
            
            if (kotlin.math.abs(x) < 8f && kotlin.math.abs(z) < 8f) continue
            
            nodes.add(createTree(x, z))
        }
        
        // Add more "Rocks"
        for (i in 0 until 60) {
            val x = random.nextFloat() * 350f - 175f
            val z = random.nextFloat() * 350f - 175f
            
            if (kotlin.math.abs(x) < 8f && kotlin.math.abs(z) < 8f) continue
            
            nodes.add(createRock(x, z))
        }
        
        return nodes
    }
    
    private fun createTree(x: Float, z: Float): Node {
        // Trunk
        val trunk = CubeNode(
            engine = engine,
            size = Size(0.4f, 2.0f, 0.4f)
        ).apply {
            position = Position(x, 1.0f, z)
            // Note: In a real app we'd set color/material here, 
            // but CubeNode defaults to white. We'll stick to geometry for now.
        }
        
        // Leaves
        val leaves = CubeNode(
            engine = engine,
            size = Size(2.0f, 2.0f, 2.0f)
        ).apply {
            position = Position(0f, 1.5f, 0f)
        }
        
        // leaves.parent = trunk
        leaves.parent = trunk
        return trunk
    }
    
    private fun createRock(x: Float, z: Float): Node {
        return CubeNode(
            engine = engine,
            size = Size(
                1.0f + random.nextFloat(),
                0.5f + random.nextFloat(),
                1.0f + random.nextFloat()
            )
        ).apply {
            position = Position(x, 0.25f, z)
            rotation = Rotation(0f, random.nextFloat() * 360f, 0f)
        }
    }
}
