package com.sarathi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarathi.app.ui.components.KrishnaHeader
import com.sarathi.app.ui.components.MessageBubble
import com.sarathi.app.ui.components.OfflineBadge
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.components.SacredCard
import com.sarathi.app.ui.components.SacredCardVariant
import com.sarathi.app.ui.components.SacredTextField
import com.sarathi.app.ui.navigation.Routes
import com.sarathi.app.ui.theme.Ink
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

private val SUGGESTIONS = listOf(
    "I feel anxious",
    "I need clarity",
    "I am attached to an outcome",
    "I failed at something",
    "I want to understand my dharma",
    "Teach me a verse",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    onNavigate: (String) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val input by chatViewModel.input.collectAsStateWithLifecycle()
    val typing by chatViewModel.typing.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, typing) {
        val lastIndex = when {
            typing -> messages.size
            messages.isNotEmpty() -> messages.lastIndex
            else -> return@LaunchedEffect
        }
        listState.scrollToItem(lastIndex.coerceAtLeast(0))
    }

    LaunchedEffect(Unit) {
        chatViewModel.ensureWelcomeMessage()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Sarathi",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(20.dp),
                )
                NavigationDrawerItem(
                    label = { Text("Chat") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                    },
                )
                NavigationDrawerItem(
                    label = { Text("Verse of the Day") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Routes.VERSE)
                    },
                )
                NavigationDrawerItem(
                    label = { Text("When I Feel") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Routes.FEEL)
                    },
                )
                NavigationDrawerItem(
                    label = { Text("My Dharma") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Routes.DHARMA)
                    },
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Routes.SETTINGS)
                    },
                )
            }
        },
    ) {
        SacredBackground(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Outlined.Menu, contentDescription = "Menu", tint = SacredGold)
                            }
                        },
                        title = { },
                        actions = {
                            OfflineBadge(Modifier.padding(end = 8.dp))
                        },
                        colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp),
                ) {
                    KrishnaHeader(showEmblem = true)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SUGGESTIONS.forEach { chip ->
                            FilterChip(
                                selected = false,
                                onClick = { chatViewModel.sendUserMessage(chip) },
                                label = { Text(chip, style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageBubble(message = msg)
                        }
                        if (typing) {
                            item("typing") {
                                SacredCard(variant = SacredCardVariant.Parchment) {
                                    Text(
                                        text = "Listening…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Ink,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SacredTextField(
                            value = input,
                            onValueChange = { chatViewModel.setInput(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = "What rests upon your heart?",
                            singleLine = false,
                            maxLines = 4,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send,
                            onImeAction = { chatViewModel.sendFromInput() },
                        )
                        FloatingActionButton(
                            onClick = { chatViewModel.sendFromInput() },
                            modifier = Modifier.size(52.dp),
                            containerColor = SacredGold,
                            contentColor = Ink,
                            shape = CircleShape,
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
