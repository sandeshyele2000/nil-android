/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun NILSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
     textStyle: TextStyle? = null,
    enabled: Boolean = true
) {
    val resolvedStyle = textStyle ?: MaterialTheme.typography.bodyMedium
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(24.dp),
        textStyle = resolvedStyle,
        enabled = enabled,
        placeholder = {
            Text(
                text = placeholder,
                style = resolvedStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        singleLine = true
    )
}
