package com.sarathi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sarathi.app.ui.navigation.SarathiNavGraph
import com.sarathi.app.viewmodel.ChatViewModel
import com.sarathi.app.ui.theme.MidnightIndigo
import com.sarathi.app.ui.theme.SarathiTheme

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels {
        (application as SarathiApp).viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SarathiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MidnightIndigo,
                ) {
                    SarathiNavGraph(chatViewModel = chatViewModel)
                }
            }
        }
    }
}
