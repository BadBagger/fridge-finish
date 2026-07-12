package com.fridgefinish.app.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

object FridgeFinishMotion {
    const val Quick = 150
    const val Standard = 240
    const val Emphasis = 320

    val standardTween = tween<Float>(durationMillis = Standard, easing = FastOutSlowInEasing)
    val quickTween = tween<Float>(durationMillis = Quick, easing = LinearOutSlowInEasing)

    fun fadeInQuick(): EnterTransition = fadeIn(tween(Quick, easing = LinearOutSlowInEasing))
    fun fadeOutQuick(): ExitTransition = fadeOut(tween(Quick, easing = LinearOutSlowInEasing))
    fun fadeInStandard(): EnterTransition = fadeIn(tween(Standard, easing = LinearOutSlowInEasing))
    fun fadeOutStandard(): ExitTransition = fadeOut(tween(Standard, easing = LinearOutSlowInEasing))

    fun listItemEnter(): EnterTransition =
        fadeIn(tween(Standard, easing = LinearOutSlowInEasing)) +
            slideInVertically(tween(Standard, easing = FastOutSlowInEasing)) { it / 8 }

    fun listItemExit(): ExitTransition =
        fadeOut(tween(Quick, easing = LinearOutSlowInEasing)) +
            slideOutVertically(tween(Quick, easing = FastOutSlowInEasing)) { -it / 10 }

    @OptIn(ExperimentalAnimationApi::class)
    fun screenTransform(forward: Boolean): ContentTransform {
        val direction = if (forward) 1 else -1
        return (
            fadeIn(tween(Standard, easing = LinearOutSlowInEasing)) +
                slideInHorizontally(tween(Standard, easing = FastOutSlowInEasing)) { width -> width * direction / 12 }
            ).togetherWith(
                fadeOut(tween(Quick, easing = LinearOutSlowInEasing)) +
                    slideOutHorizontally(tween(Quick, easing = FastOutSlowInEasing)) { width -> -width * direction / 16 }
            )
    }

    fun popIn(): EnterTransition =
        fadeIn(tween(Standard, easing = LinearOutSlowInEasing)) +
            scaleIn(tween(Standard, easing = FastOutSlowInEasing), initialScale = 0.98f)

    fun popOut(): ExitTransition =
        fadeOut(tween(Quick, easing = LinearOutSlowInEasing)) +
            scaleOut(tween(Quick, easing = FastOutSlowInEasing), targetScale = 0.98f)
}
