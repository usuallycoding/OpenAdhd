package com.usuallycoding.openadhd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.usuallycoding.openadhd.data.createAndroidContainer

class MainActivity : ComponentActivity() {
    private val container by lazy { createAndroidContainer(this, AndroidDataFiles(this), AndroidReminderScheduler(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App(container)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
