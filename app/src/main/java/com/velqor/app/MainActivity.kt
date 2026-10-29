package com.velqor.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.velqor.core.designsystem.component.VelqorEmptyState
import com.velqor.core.designsystem.component.VelqorPrimaryButton
import com.velqor.core.designsystem.theme.LocalVelqorColors
import com.velqor.core.designsystem.theme.VelqorTheme
import com.velqor.core.filesystem.model.FileType
import com.velqor.core.tasks.ui.TasksScreen
import com.velqor.feature.archive.ArchiveViewerScreen
import com.velqor.feature.browser.BrowserScreen
import com.velqor.feature.browser.BrowserViewModel
import com.velqor.feature.editor.TextEditorScreen
import com.velqor.feature.search.SearchScreen
import com.velqor.feature.settings.SettingsScreen
import com.velqor.feature.vault.VaultScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as VelqorApplication
        val container = app.container

        setContent {
            VelqorTheme {
                val colors = LocalVelqorColors.current
                var hasStoragePermission by remember { mutableStateOf(checkStoragePermission()) }

                if (!hasStoragePermission) {
                    PermissionRequestScreen(
                        onGrantClicked = {
                            requestStoragePermission()
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val browserViewModel = remember {
                        BrowserViewModel(
                            fileManager = container.fileManager,
                            taskManager = container.taskManager,
                            archiveEngine = container.archiveEngine,
                            initialPath = Environment.getExternalStorageDirectory().absolutePath
                        )
                    }

                    Scaffold(
                        bottomBar = {
                            val showBottomBar = currentRoute in setOf("browser", "vault", "tasks", "settings")
                            if (showBottomBar) {
                                NavigationBar(
                                    containerColor = colors.surface.copy(alpha = 0.95f),
                                    contentColor = colors.textPrimary,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.navigationBarsPadding()
                                ) {
                                    NavigationBarItem(
                                        selected = currentRoute == "browser",
                                        onClick = { navController.navigate("browser") { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.Folder, null) },
                                        label = { Text("Tệp", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = colors.accent,
                                            selectedTextColor = colors.accent,
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "vault",
                                        onClick = { navController.navigate("vault") { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.Lock, null) },
                                        label = { Text("Kho bảo mật", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = colors.accent,
                                            selectedTextColor = colors.accent,
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "tasks",
                                        onClick = { navController.navigate("tasks") { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.PendingActions, null) },
                                        label = { Text("Tác vụ", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = colors.accent,
                                            selectedTextColor = colors.accent,
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "settings",
                                        onClick = { navController.navigate("settings") { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.Settings, null) },
                                        label = { Text("Cài đặt", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = colors.accent,
                                            selectedTextColor = colors.accent,
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "browser",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable("browser") {
                                BrowserScreen(
                                    viewModel = browserViewModel,
                                    onNavigateToSearch = { rootPath ->
                                        val encoded = URLEncoder.encode(rootPath, StandardCharsets.UTF_8.toString())
                                        navController.navigate("search/$encoded")
                                    },
                                    onOpenFile = { fileItem ->
                                        if (fileItem.type == FileType.CODE || fileItem.type == FileType.DOCUMENT) {
                                            val encoded = URLEncoder.encode(fileItem.path, StandardCharsets.UTF_8.toString())
                                            navController.navigate("editor?path=$encoded")
                                        }
                                    },
                                    onOpenArchive = { archivePath ->
                                        val encoded = URLEncoder.encode(archivePath, StandardCharsets.UTF_8.toString())
                                        navController.navigate("archive?path=$encoded")
                                    }
                                )
                            }

                            composable(
                                route = "search/{rootPath}",
                                arguments = listOf(navArgument("rootPath") { type = NavType.StringType })
                            ) { backStack ->
                                val encoded = backStack.arguments?.getString("rootPath") ?: ""
                                val rootPath = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                                SearchScreen(
                                    fileManager = container.fileManager,
                                    searchRootPath = rootPath,
                                    onNavigateBack = { navController.popBackStack() },
                                    onSelectFile = { item ->
                                        navController.popBackStack()
                                        if (item.isDirectory) {
                                            browserViewModel.loadDirectory(item.path)
                                        }
                                    }
                                )
                            }

                            composable(
                                route = "archive?path={path}",
                                arguments = listOf(navArgument("path") { type = NavType.StringType })
                            ) { backStack ->
                                val encoded = backStack.arguments?.getString("path") ?: ""
                                val path = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                                ArchiveViewerScreen(
                                    archivePath = path,
                                    archiveEngine = container.archiveEngine,
                                    taskManager = container.taskManager,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "editor?path={path}",
                                arguments = listOf(navArgument("path") { type = NavType.StringType })
                            ) { backStack ->
                                val encoded = backStack.arguments?.getString("path") ?: ""
                                val path = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                                TextEditorScreen(
                                    filePath = path,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable("vault") {
                                VaultScreen(
                                    vaultDirectory = container.vaultDirectory,
                                    vaultCipher = container.vaultCipher,
                                    taskManager = container.taskManager
                                )
                            }

                            composable("tasks") {
                                TasksScreen(taskManager = container.taskManager)
                            }

                            composable("settings") {
                                SettingsScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when coming back from system settings
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(
    onGrantClicked: () -> Unit
) {
    val colors = LocalVelqorColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        VelqorEmptyState(
            title = "Cấp quyền quản lý bộ nhớ",
            subtitle = "VELQOR cần quyền truy cập toàn bộ tệp tin để duyệt thư mục, nén, giải nén và bảo mật tệp của bạn với tốc độ cao nhất.",
            actionButton = {
                VelqorPrimaryButton(
                    text = "Cấp quyền ngay",
                    onClick = onGrantClicked
                )
            }
        )
    }
}
