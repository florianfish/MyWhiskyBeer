package fr.florianfish.mywhiskybeer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import fr.florianfish.mywhiskybeer.ui.MyWhiskyBeerAppUi
import fr.florianfish.mywhiskybeer.ui.theme.MyWhiskyBeerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyWhiskyBeerTheme {
                MyWhiskyBeerAppUi()
            }
        }
    }
}
