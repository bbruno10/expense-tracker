package com.brunobrandao.expensetracker.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brunobrandao.expensetracker.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NavyBackground = Color(0xFF1A1A2E)
private val SubtitleColor = Color(0xFF888888)

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val iconScale = remember { Animatable(0.5f) }
    val iconAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            iconScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f))
        }
        launch {
            iconAlpha.animateTo(1f, animationSpec = tween(durationMillis = 800))
        }
        launch {
            delay(400)
            titleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 800))
        }
        launch {
            delay(800)
            subtitleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 800))
        }
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                        alpha = iconAlpha.value
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ExpenseTracker",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Track · Save · Grow",
                color = SubtitleColor,
                fontSize = 14.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value }
            )
        }
    }
}
