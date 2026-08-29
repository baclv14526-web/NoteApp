package com.example.noteapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.noteapp.util.PinManager

/**
 * Màn hình quản lý mật khẩu bảo vệ ghi chú bí mật: đặt PIN lần đầu, đổi PIN
 * (yêu cầu xác thực PIN cũ trước), hoặc tắt hẳn tính năng khoá.
 *
 * Truy cập từ danh sách ghi chú chính. Không cần xác thực PIN cũ nếu người
 * dùng vào đây để ĐẶT PIN LẦN ĐẦU (chưa có gì để xác thực).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var pinIsSet by remember { mutableStateOf(PinManager.isPinSet(context)) }
    var showSetupScreen by remember { mutableStateOf(false) }
    var showVerifyBeforeChange by remember { mutableStateOf(false) }
    var showForgotPinScreen by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    if (showSetupScreen) {
        SetupPinScreen(
            onBack = { showSetupScreen = false },
            onPinSet = {
                showSetupScreen = false
                pinIsSet = true
            }
        )
        return
    }

    if (showVerifyBeforeChange) {
        VerifyPinScreen(
            onBack = { showVerifyBeforeChange = false },
            onSuccess = {
                showVerifyBeforeChange = false
                showSetupScreen = true
            },
            onForgotPin = {
                showVerifyBeforeChange = false
                showForgotPinScreen = true
            }
        )
        return
    }

    if (showForgotPinScreen) {
        ForgotPinScreen(
            onBack = { showForgotPinScreen = false },
            onPinReset = {
                showForgotPinScreen = false
                pinIsSet = true
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bảo mật ghi chú") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                if (pinIsSet) "Đã đặt mật khẩu bảo vệ ghi chú bí mật." else "Chưa đặt mật khẩu. Đặt mật khẩu để có thể khoá các ghi chú riêng tư.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            if (!pinIsSet) {
                Button(
                    onClick = { showSetupScreen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đặt mật khẩu")
                }
            } else {
                Button(
                    onClick = { showVerifyBeforeChange = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đổi mật khẩu")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showRemoveConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tắt bảo vệ bằng mật khẩu")
                }
            }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Tắt bảo vệ bằng mật khẩu?") },
            text = {
                Text(
                    "Toàn bộ ghi chú bí mật hiện tại sẽ vẫn được đánh dấu là khoá, " +
                        "nhưng sẽ không còn mật khẩu nào để mở khoá lại cho đến khi bạn đặt mật khẩu mới. " +
                        "Bạn nên mở khoá các ghi chú quan trọng trước khi tắt."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    PinManager.clearPin(context)
                    pinIsSet = false
                    showRemoveConfirm = false
                    Toast.makeText(context, "Đã tắt bảo vệ bằng mật khẩu", Toast.LENGTH_SHORT).show()
                }) { Text("Tắt", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Huỷ") }
            }
        )
    }
}
