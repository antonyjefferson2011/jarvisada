package com.jarvis.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jarvis.mobile.navigation.JarvisApp
import com.jarvis.mobile.ui.ChatViewModel
import com.jarvis.mobile.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisTheme {
                JarvisApp(viewModel = chatViewModel)
            }
        }
    }
}
