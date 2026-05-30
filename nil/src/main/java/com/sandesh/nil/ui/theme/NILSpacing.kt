/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class NILSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 6.dp,
    val md: Dp = 8.dp,
    val lg: Dp = 10.dp,
    val xl: Dp = 14.dp
)

val LocalNILSpacing = staticCompositionLocalOf { NILSpacing() }
