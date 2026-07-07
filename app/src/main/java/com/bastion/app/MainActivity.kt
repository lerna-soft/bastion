package com.bastion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.bastion.app.ui.BastionNavGraph
import com.bastion.app.ui.theme.BastionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as BastionApp

        setContent {
            BastionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    BastionNavGraph(
                        navController = navController,
                        repository = app.repository
                    )
                }
            }
        }
    }
}
