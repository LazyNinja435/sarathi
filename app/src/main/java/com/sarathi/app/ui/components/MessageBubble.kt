package com.sarathi.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.Sender
import com.sarathi.app.ui.theme.Ink
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold
import java.text.DateFormat
import java.util.Date

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.sender == Sender.User
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = null,
                tint = SacredGold,
                modifier = Modifier.padding(top = 8.dp, end = 8.dp),
            )
        }
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            if (isUser) {
                SacredCard(variant = SacredCardVariant.Indigo) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftGold,
                    )
                    Text(
                        text = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.timestampMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftGold.copy(alpha = 0.55f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            } else {
                SacredCard(variant = SacredCardVariant.Parchment) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}
