package com.example.noteapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.noteapp.ui.CategoryScreen
import com.example.noteapp.ui.ExportImportScreen
import com.example.noteapp.ui.NoteEditScreen
import com.example.noteapp.ui.NoteListScreen
import com.example.noteapp.ui.NoteViewModel
import com.example.noteapp.ui.NoteViewModelFactory
import com.example.noteapp.ui.SecuritySettingsScreen
import com.example.noteapp.ui.TrashScreen
import com.example.noteapp.ui.theme.NoteAppTheme

class MainActivity : ComponentActivity() {

    companion object {
        /** Key Intent extra dùng khi mở app từ notification nhắc nhở (xem ReminderReceiver). */
        const val EXTRA_OPEN_NOTE_ID = "extra_open_note_id"
        /** Key Intent extra dùng khi mở app từ Widget (khớp với WidgetActionKeys.noteIdKey). */
        const val EXTRA_WIDGET_NOTE_ID = "note_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as NoteApplication).repository

        // Ghi chú được mở trực tiếp nếu app khởi động từ việc tap vào
        // notification nhắc nhở, hoặc từ tap vào 1 dòng trong App Widget —
        // hai nguồn dùng 2 key Intent extra khác nhau nên kiểm tra cả hai.
        val openNoteId = intent?.getLongExtra(EXTRA_OPEN_NOTE_ID, -1L)?.takeIf { it != -1L }
            ?: intent?.getLongExtra(EXTRA_WIDGET_NOTE_ID, -1L)?.takeIf { it != -1L }

        setContent {
            NoteAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: NoteViewModel = viewModel(factory = NoteViewModelFactory(repository))
                    RequestNotificationPermission()
                    AppNavHost(viewModel, startNoteId = openNoteId)
                }
            }
        }
    }
}

/**
 * Xin quyền POST_NOTIFICATIONS — bắt buộc từ Android 13 (API 33) trở lên để
 * hiển thị bất kỳ notification nào, bao gồm nhắc nhở ghi chú. Trên các bản
 * Android cũ hơn, quyền này được cấp mặc định nên không cần xin.
 */
@Composable
private fun RequestNotificationPermission() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Người dùng đồng ý hoặc từ chối — nếu từ chối, nhắc nhở sẽ không hiển thị được,
           NoteEditScreen sẽ tự kiểm tra lại quyền khi người dùng đặt nhắc nhở. */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun AppNavHost(viewModel: NoteViewModel, startNoteId: Long? = null) {
    val navController = rememberNavController()

    // Nếu app được mở từ notification, điều hướng thẳng tới ghi chú đó ngay
    // sau khi NavHost sẵn sàng.
    LaunchedEffect(startNoteId) {
        if (startNoteId != null) {
            navController.navigate("edit/$startNoteId")
        }
    }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            NoteListScreen(
                viewModel = viewModel,
                onAddNote = { navController.navigate("edit/new") },
                onOpenNote = { id -> navController.navigate("edit/$id") },
                onManageCategories = { navController.navigate("categories") },
                onExportImport = { navController.navigate("export_import") },
                onOpenTrash = { navController.navigate("trash") },
                onOpenSecuritySettings = { navController.navigate("security_settings") }
            )
        }
        composable(
            route = "edit/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val idArg = backStackEntry.arguments?.getString("noteId")
            val noteId = idArg?.toLongOrNull()
            NoteEditScreen(
                viewModel = viewModel,
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("categories") {
            CategoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("export_import") {
            ExportImportScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("trash") {
            TrashScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("security_settings") {
            SecuritySettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
