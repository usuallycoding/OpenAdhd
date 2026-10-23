package com.usuallycoding.openadhd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.usuallycoding.openadhd.ui.components.ThemeChooser
import com.usuallycoding.openadhd.ui.theme.AppTheme
import com.usuallycoding.openadhd.ui.theme.ThemeMode
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.resources.painterResource

import com.usuallycoding.openadhd.resources.Res
import com.usuallycoding.openadhd.resources.compose_multiplatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
@DevelopmentEntryPoint
fun App() {
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }

    AppTheme(themeMode) {
        var showContent by remember { mutableStateOf(false) }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("OpenAdhd") },
                    actions = {
                        ThemeChooser(
                            selected = themeMode,
                            onSelected = { themeMode = it },
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(onClick = { showContent = !showContent }) {
                    Text("Click me!")
                }
                AnimatedVisibility(showContent) {
                    val greeting = remember { Greeting().greet() }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(painterResource(Res.drawable.compose_multiplatform), null)
                        Text("Compose: $greeting")
                    }
                }
            }
        }
    }
}
