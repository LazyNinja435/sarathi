package com.sarathi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarathi.app.model.Emotion
import com.sarathi.app.ui.components.ChoiceChipCard
import com.sarathi.app.ui.components.OfflineBadge
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.components.SacredButton
import com.sarathi.app.ui.components.SacredButtonLabel
import com.sarathi.app.ui.components.SacredButtonStyle
import com.sarathi.app.ui.components.SacredCard
import com.sarathi.app.ui.components.SacredCardVariant
import com.sarathi.app.ui.components.SplashCenterLogo
import com.sarathi.app.ui.theme.Ink
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold
import com.sarathi.app.viewmodel.FeelViewModel

private fun replyFor(emotion: Emotion): String = when (emotion) {
    Emotion.Lost -> "When the path is hidden, do not demand the whole map. Light one lamp: one truthful word, one kind act, one small duty completed with care."
    Emotion.Angry -> "Anger can burn the house it was meant to protect. Name the wound beneath the heat. Then return to one measured action that honors both truth and peace."
    Emotion.Afraid -> "Fear visits the mind when it forgets the Self.\n\nCome back to the breath. Come back to the duty before you. Come back to Me within your own heart."
    Emotion.Attached -> "Offer the work without bargaining with the future. Attachment to fruit makes the heart tremble; sincere action makes it steady."
    Emotion.Unmotivated -> "Do not ask the mountain of yourself all at once. Begin with one small motion—tidy a corner, write one line, take one walk. Momentum returns to the sincere."
    Emotion.Heartbroken -> "The heart may ache, yet you are more than this weather. Grieve if you must—gently. Then place one tender hand on the duty that still calls you, however small."
    Emotion.Confused -> "Confusion is often many voices speaking at once. Quiet them by naming one true thing you know today. Then act from that single truth."
    Emotion.Proud -> "Let your joy in good work be humble and bright, like a lamp in a shrine—not a fire that blinds. Pride that serves gratitude becomes grace."
    Emotion.Jealous -> "Comparison steals the sweetness of your own offering. Turn your gaze from another's harvest to the seed in your own palm. Cultivate what is yours to grow."
    Emotion.Peaceful -> "Rest here a moment. Peace is not the absence of waves, but a depth beneath them. Carry one quiet kindness into your next action."
}

private val GRID_EMOTIONS: List<Emotion> = Emotion.entries.filter { it != Emotion.Peaceful }

@Composable
fun FeelScreen(
    feelViewModel: FeelViewModel,
    onBack: () -> Unit,
    onContinue: (Emotion) -> Unit,
) {
    val selected by feelViewModel.selected.collectAsStateWithLifecycle()

    SacredBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = SacredGold)
                }
                OfflineBadge()
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                SplashCenterLogo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("When I feel…", style = MaterialTheme.typography.headlineMedium, color = SoftGold)
                Text("Choose what rests upon your heart", style = MaterialTheme.typography.titleMedium, color = SacredGold)
            }
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false,
                contentPadding = PaddingValues(0.dp),
            ) {
                items(GRID_EMOTIONS) { em ->
                    ChoiceChipCard(
                        title = em.label,
                        description = "",
                        selected = selected == em,
                        onClick = { feelViewModel.select(em) },
                        leading = {},
                    )
                }
                item(span = { GridItemSpan(3) }) {
                    ChoiceChipCard(
                        title = Emotion.Peaceful.label,
                        description = "",
                        selected = selected == Emotion.Peaceful,
                        onClick = { feelViewModel.select(Emotion.Peaceful) },
                        leading = {},
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val sel = selected
            if (sel != null) {
                SacredCard(variant = SacredCardVariant.Parchment) {
                    Text(
                        text = replyFor(sel),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "— Inspired by Bhagavad Gita themes —",
                        style = MaterialTheme.typography.bodySmall,
                        color = SacredGold,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(12.dp))
                SacredButton(
                    onClick = { onContinue(sel) },
                    style = SacredButtonStyle.GoldOutline,
                ) {
                    SacredButtonLabel("Continue with this feeling", inkOnParchment = false)
                }
            }
        }
    }
}
