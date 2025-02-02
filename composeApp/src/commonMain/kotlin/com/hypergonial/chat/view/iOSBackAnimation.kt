package com.hypergonial.chat.view

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimator
import com.arkivanov.essenty.backhandler.BackHandler

// Taken from:
// https://arkivanov.github.io/Decompose/extensions/compose/#predictive-back-gesture-on-ios

/**
 * An iOS-like back animation that uses a predictive back gesture.
 *
 * @param backHandler The back handler to use for the back gesture.
 * @param onBack The callback to call when the back gesture is detected.
 */
@OptIn(ExperimentalDecomposeApi::class)
fun <C : Any, T : Any> iOSBackAnimation(backHandler: BackHandler, onBack: () -> Unit): StackAnimation<C, T> =
    predictiveBackAnimation(
        backHandler = backHandler,
        fallbackAnimation = stackAnimation(iosLikeSlide()),
        selector = { initialBackEvent, _, _ ->
            predictiveBackAnimatable(
                initialBackEvent = initialBackEvent,
                exitModifier = { progress, _ -> Modifier.slideExitModifier(progress = progress) },
                enterModifier = { progress, _ -> Modifier.slideEnterModifier(progress = progress) },
            )
        },
        onBack = onBack,
    )

private fun iosLikeSlide(animationSpec: FiniteAnimationSpec<Float> = tween()): StackAnimator =
    stackAnimator(animationSpec = animationSpec) { factor, direction, content ->
        content(
            Modifier.then(if (direction.isFront) Modifier else Modifier.fade(factor + 1F))
                .offsetXFactor(factor = if (direction.isFront) factor else factor * 0.5F)
        )
    }

private fun Modifier.slideExitModifier(progress: Float): Modifier = offsetXFactor(progress)

private fun Modifier.slideEnterModifier(progress: Float): Modifier =
    fade(progress).offsetXFactor((progress - 1f) * 0.5f)

private fun Modifier.fade(factor: Float) = drawWithContent {
    drawContent()
    drawRect(color = Color(red = 0F, green = 0F, blue = 0F, alpha = (1F - factor) / 4F))
}

private fun Modifier.offsetXFactor(factor: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    layout(placeable.width, placeable.height) {
        placeable.placeRelative(x = (placeable.width.toFloat() * factor).toInt(), y = 0)
    }
}
