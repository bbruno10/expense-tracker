package com.brunobrandao.expensetracker.presentation.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy

// GreenPrimary #1D9E75 — a cor canônica do indicador líquido
private val GooeyGreen = Color(0xFF1D9E75)

// Mapeia o índice do item na lista (0-4) para a posição do slot no nav (0-4).
// Índice 2 é o FAB central (nunca selecionável), então:
//   item 0 → slot 0 | item 1 → slot 1 | item 3 → slot 3 | item 4 → slot 4
private fun itemIndexToSlot(index: Int): Float = when (index) {
    0 -> 0f
    1 -> 1f
    3 -> 3f
    4 -> 4f
    else -> 0f
}

@Composable
fun GooeyBottomNavBar(
    items: List<BottomNavItem>,          // lista completa de 5 itens (inclui FAB no índice 2)
    currentDestination: NavDestination?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determina qual item está selecionado (índice na lista completa, pulando o FAB)
    var selectedIndex = 0
    for (i in items.indices) {
        if (i == 2) continue
        if (currentDestination?.hierarchy?.any { it.route == items[i].screen.route } == true) {
            selectedIndex = i
            break
        }
    }

    val animatedSlot by animateFloatAsState(
        targetValue = itemIndexToSlot(selectedIndex),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "gooey_slot"
    )

    val blurPx = with(LocalDensity.current) { 28.dp.toPx() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Camada do blob (efeito gooey em API 31+ ou indicador simples em API < 31)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                GooeyBlobAPI31(animatedSlot = animatedSlot, blurPx = blurPx)
            } else {
                SimpleIndicator(animatedSlot = animatedSlot)
            }

            // Camada de ícones + labels (sobre o blob)
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    if (index == 2) {
                        // Slot central vazio — o FAB flutua acima via offset no NavHost
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val isSelected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onItemClick(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// API 31+: blob verde com blur + threshold via RenderEffect (GPU-accelerated).
// O chain blur→colorMatrix cria o efeito de borda líquida/metaball.
@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun GooeyBlobAPI31(
    animatedSlot: Float,
    blurPx: Float,
    modifier: Modifier = Modifier
) {
    val gooeyEffect = remember(blurPx) {
        val blur = android.graphics.RenderEffect.createBlurEffect(
            blurPx, blurPx, android.graphics.Shader.TileMode.DECAL
        )
        // ColorMatrix: A' = 30*A − 3825  →  threshold ao redor de alpha=127
        // Pixels com alpha < ~128 → transparent; alpha > ~128 → opaque green
        val matrix = android.graphics.ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 30f, -3825f
            )
        )
        val colorEffect = android.graphics.RenderEffect.createColorFilterEffect(
            android.graphics.ColorMatrixColorFilter(matrix)
        )
        // inner = blur primeiro, outer = threshold por cima
        android.graphics.RenderEffect
            .createChainEffect(colorEffect, blur)
            .asComposeRenderEffect()
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { renderEffect = gooeyEffect }
    ) {
        val slotWidth = size.width / 5f
        drawCircle(
            color = GooeyGreen,
            radius = slotWidth * 0.38f,
            center = Offset(x = (animatedSlot + 0.5f) * slotWidth, y = size.height / 2f)
        )
    }
}

// API < 31: indicador simples sem efeito gooey — apenas um círculo animado.
@Composable
private fun SimpleIndicator(
    animatedSlot: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val slotWidth = size.width / 5f
        drawCircle(
            color = GooeyGreen,
            radius = slotWidth * 0.35f,
            center = Offset(x = (animatedSlot + 0.5f) * slotWidth, y = size.height / 2f)
        )
    }
}
