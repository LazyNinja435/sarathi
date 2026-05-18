package com.sarathi.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sarathi.app.ui.theme.IndigoBubble
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold

@Composable
fun SacredTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 8,
    leading: @Composable (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
) {
    val shape = RoundedCornerShape(8.dp)
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SacredGold, shape)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        placeholder = {
            Text(
                text = placeholder,
                color = SoftGold.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        leadingIcon = leading,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = IndigoBubble.copy(alpha = 0.92f),
            unfocusedContainerColor = IndigoBubble.copy(alpha = 0.92f),
            disabledContainerColor = IndigoBubble.copy(alpha = 0.65f),
            disabledTextColor = SoftGold.copy(alpha = 0.45f),
            focusedTextColor = SoftGold,
            unfocusedTextColor = SoftGold,
            cursorColor = SacredGold,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = SoftGold),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onSend = { onImeAction() },
            onDone = { onImeAction() },
        ),
    )
}
