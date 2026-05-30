/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.sandesh.nil.R

object NILColors {
    @Composable fun bg(): Color = colorResource(R.color.nil_bg)
    @Composable fun surface(): Color = colorResource(R.color.nil_surface)
    @Composable fun surfaceElevated(): Color = colorResource(R.color.nil_surface_elevated)
    @Composable fun border(): Color = colorResource(R.color.nil_border)

    @Composable fun accent(): Color = colorResource(R.color.nil_accent)
    @Composable fun warning(): Color = colorResource(R.color.nil_warning)
    @Composable fun error(): Color = colorResource(R.color.nil_error)
    @Composable fun info(): Color = colorResource(R.color.nil_info)

    @Composable fun textPrimary(): Color = colorResource(R.color.nil_text_primary)
    @Composable fun textSecondary(): Color = colorResource(R.color.nil_text_secondary)

    @Composable fun jsonKey(): Color = colorResource(R.color.nil_json_key)
    @Composable fun jsonString(): Color = colorResource(R.color.nil_json_string)
    @Composable fun jsonNumber(): Color = colorResource(R.color.nil_json_number)
    @Composable fun jsonBoolNull(): Color = colorResource(R.color.nil_json_bool_null)
    @Composable fun jsonMatch(): Color = colorResource(R.color.nil_json_match)
    @Composable fun jsonActiveMatch(): Color = colorResource(R.color.nil_json_active_match)
}
