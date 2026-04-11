package com.unesell.comfysorter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unesell.comfysorter.ui.theme.ComfySorterTheme
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComfySorterTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { ServerRepository(context) }

    NavHost(navController = navController, startDestination = "server_list") {

        // Экран 1: Список серверов
        composable("server_list") {
            ServerListScreen(
                repository = repository,
                onNavigateToScanner = { navController.navigate("scanner") },
                onServerClick = { url ->
                    val encodedUrl = URLEncoder.encode(url, "UTF-8")
                    navController.navigate("gallery?url=$encodedUrl")
                }
            )
        }

        // Экран 2: Сканер QR
        composable("scanner") {
            ScannerScreen(
                onQrScanned = { url ->
                    repository.addServer(url)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Экран 3: Галерея
        composable("gallery?url={url}") { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val baseUrl = URLDecoder.decode(encodedUrl, "UTF-8")

            GalleryScreen(
                baseUrl = baseUrl,
                onBack = { navController.popBackStack() },
                onImageClick = { relPath ->
                    // Переход к просмотру изображения
                    val encodedPath = URLEncoder.encode(relPath, "UTF-8")
                    navController.navigate("viewer?baseUrl=$encodedUrl&relPath=$encodedPath")
                }
            )
        }

        // Экран 4: Полноэкранный просмотр
        composable("viewer?baseUrl={baseUrl}&relPath={relPath}") { backStackEntry ->
            val encodedBaseUrl = backStackEntry.arguments?.getString("baseUrl") ?: ""
            val encodedRelPath = backStackEntry.arguments?.getString("relPath") ?: ""

            val baseUrl = URLDecoder.decode(encodedBaseUrl, "UTF-8")
            val relPath = URLDecoder.decode(encodedRelPath, "UTF-8")

            ViewerScreen(
                baseUrl = baseUrl,
                relPath = relPath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}