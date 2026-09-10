package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    size: Float = 150f,
    onMove: (x: Float, y: Float) -> Unit
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { (size / 2).dp.toPx() }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        offset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        val newOffset = offset + dragAmount
                        val distance = sqrt(newOffset.x.pow(2) + newOffset.y.pow(2))
                        
                        offset = if (distance <= radiusPx) {
                            newOffset
                        } else {
                            val angle = atan2(newOffset.y, newOffset.x)
                            Offset(cos(angle) * radiusPx, sin(angle) * radiusPx)
                        }
                        
                        onMove(offset.x / radiusPx, -offset.y / radiusPx)
                        change.consume()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .size((size / 2.5).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.7f))
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.4f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.onPrimary else Color.White
        )
    }
}

@Composable
fun StaminaBar(stamina: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(200.dp)) {
        LinearProgressIndicator(
            progress = { stamina / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = if (stamina < 20f) Color.Red else Color.Green,
            trackColor = Color.Gray.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun CompassHUD(rotationY: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size / 2f
            val radius = size.minDimension / 2f * 0.8f
            
            // Draw N, S, E, W
            val directions = listOf("N", "E", "S", "W")
            directions.forEachIndexed { index, label ->
                val angle = Math.toRadians((index * 90 - rotationY - 90).toDouble()).toFloat()
                val x = center.width + cos(angle) * radius
                val y = center.height + sin(angle) * radius
                // Simplified text drawing - just dots for now
                drawCircle(Color.White, radius = 4f, center = Offset(x, y))
            }
            
            // Needle
            val needleAngle = Math.toRadians((-rotationY - 90).toDouble()).toFloat()
            drawLine(
                color = Color.Red,
                start = Offset(center.width, center.height),
                end = Offset(
                    center.width + cos(needleAngle) * radius,
                    center.height + sin(needleAngle) * radius
                ),
                strokeWidth = 4f
            )
        }
        Text("N", color = Color.Red, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun TimeHUD(gameTime: Int) {
    val totalMinutes = gameTime
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    
    val dayProgress = gameTime.toFloat() / 1440f

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = dayProgress,
            modifier = Modifier
                .width(120.dp)
                .height(4.dp)
                .clip(CircleShape),
            color = if (hours in 6..18) Color(0xFFFFD700) else Color(0xFF4B0082),
            trackColor = Color.White.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun StatusBarsHUD(
    health: Float,
    hunger: Float,
    thirst: Float,
    stamina: Float
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .width(150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusIndicator(label = "Health", value = health / 100f, color = Color.Red)
        StatusIndicator(label = "Hunger", value = hunger / 100f, color = Color(0xFFFFA500))
        StatusIndicator(label = "Thirst", value = thirst / 100f, color = Color.Cyan)
        StatusIndicator(label = "Energy", value = stamina / 100f, color = Color.Green)
    }
}

@Composable
private fun StatusIndicator(label: String, value: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
            Text("${(value * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        LinearProgressIndicator(
            progress = value,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = color,
            trackColor = Color.Black.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun MapOverlay(
    playerPosX: Float,
    playerPosZ: Float,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Island Map (1km x 1km)",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .background(Color(0xFF2D5A27)) // Dark green for island
            ) {
                // Map visualization
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val mapSize = 1000f // 1km
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    // Player marker (normalized to map size)
                    val px = centerX + (playerPosX / mapSize) * size.width
                    val py = centerY + (playerPosZ / mapSize) * size.height
                    
                    drawCircle(
                        color = Color.Red,
                        radius = 8f,
                        center = Offset(px, py)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onClose) {
                Text("Close Map")
            }
        }
    }
}

@Composable
fun BackpackOverlay(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Inventory", style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Simplified Inventory Grid
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(12) { index ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when(index) {
                                0 -> Icons.Default.FlashlightOn
                                1 -> Icons.Default.Home
                                2 -> Icons.Default.Restaurant
                                else -> Icons.Default.Inventory2
                            }
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Backpack items collected on the island appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
