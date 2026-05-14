package com.sarathi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sarathi.app.ui.components.KrishnaHeader
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.components.SacredButton
import com.sarathi.app.ui.components.SacredButtonLabel
import com.sarathi.app.ui.components.SacredCard
import com.sarathi.app.ui.components.SacredCardVariant
import com.sarathi.app.ui.components.SacredTextField
import com.sarathi.app.ui.theme.Ink
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold

@Composable
fun NameScreen(
    onOfferName: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    SacredBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KrishnaHeader()
            Spacer(Modifier.height(8.dp))
            SacredCard(variant = SacredCardVariant.Parchment) {
                Text(
                    text = "My dear devotee,\nwhat is the name I bestowed upon you?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
            }
            SacredTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = false
                },
                placeholder = "Your name",
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                onImeAction = {
                    if (name.isBlank()) error = true else onOfferName(name)
                },
            )
            if (error) {
                Text(
                    text = "Please enter your name.",
                    color = SacredGold,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SacredButton(
                onClick = {
                    if (name.isBlank()) error = true else onOfferName(name)
                },
            ) {
                SacredButtonLabel("Offer my name")
            }
            Spacer(Modifier.height(32.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = SacredGold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your journey is private and available offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftGold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
