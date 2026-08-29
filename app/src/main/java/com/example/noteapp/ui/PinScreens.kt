package com.example.noteapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.noteapp.util.PinManager

/**
 * Màn hình đặt PIN 6 số lần đầu tiên, kèm chọn 1 câu hỏi bảo mật + câu trả
 * lời để dùng cho việc reset PIN sau này nếu quên.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupPinScreen(onBack: () -> Unit, onPinSet: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var selectedQuestion by remember { mutableStateOf(PinManager.securityQuestions.first()) }
    var answer by remember { mutableStateOf("") }
    var questionMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đặt mật khẩu 6 số") },
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
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Đặt mật khẩu để bảo vệ các ghi chú bí mật. Mật khẩu này dùng chung cho tất cả ghi chú đã khoá.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                label = { Text("Nhập mã PIN 6 số") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                label = { Text("Nhập lại mã PIN") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Text("Câu hỏi bảo mật (dùng để lấy lại PIN nếu quên)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    onClick = { questionMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedQuestion, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = questionMenuExpanded,
                    onDismissRequest = { questionMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PinManager.securityQuestions.forEach { q ->
                        DropdownMenuItem(
                            text = { Text(q) },
                            onClick = {
                                selectedQuestion = q
                                questionMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("Câu trả lời của bạn") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    when {
                        pin.length != 6 -> Toast.makeText(context, "Mã PIN phải đủ 6 số", Toast.LENGTH_SHORT).show()
                        pin != confirmPin -> Toast.makeText(context, "Mã PIN nhập lại không khớp", Toast.LENGTH_SHORT).show()
                        answer.isBlank() -> Toast.makeText(context, "Vui lòng nhập câu trả lời bảo mật", Toast.LENGTH_SHORT).show()
                        else -> {
                            PinManager.setPin(context, pin, selectedQuestion, answer)
                            Toast.makeText(context, "Đã đặt mật khẩu thành công", Toast.LENGTH_SHORT).show()
                            onPinSet()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Xác nhận")
            }
        }
    }
}

/**
 * Màn hình nhập PIN để mở 1 ghi chú bí mật. Có nút "Quên PIN?" dẫn sang
 * ForgotPinScreen. onSuccess được gọi khi nhập đúng PIN.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyPinScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onForgotPin: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhập mật khẩu") },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text("Ghi chú này đã được khoá", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        pin = it
                        errorText = null
                    }
                },
                label = { Text("Mã PIN 6 số") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                isError = errorText != null,
                supportingText = { errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (PinManager.verifyPin(context, pin)) {
                        onSuccess()
                    } else {
                        errorText = "Mã PIN không đúng"
                        pin = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.length == 6
            ) {
                Text("Mở khoá")
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onForgotPin) {
                Text("Quên mã PIN?")
            }
        }
    }
}

/**
 * Màn hình reset PIN quên: trả lời đúng câu hỏi bảo mật đã đặt trước đó,
 * sau đó cho phép đặt PIN mới ngay trong cùng màn hình.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPinScreen(onBack: () -> Unit, onPinReset: () -> Unit) {
    val context = LocalContext.current
    val question = remember { PinManager.getSecurityQuestion(context) }
    var answer by remember { mutableStateOf("") }
    var answerVerified by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quên mật khẩu") },
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
            if (question == null) {
                Text(
                    "Không tìm thấy câu hỏi bảo mật. Vui lòng liên hệ hỗ trợ hoặc xoá dữ liệu ứng dụng để đặt lại từ đầu.",
                    color = MaterialTheme.colorScheme.error
                )
                return@Column
            }

            if (!answerVerified) {
                Text("Câu hỏi bảo mật của bạn:", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text(question, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it; errorText = null },
                    label = { Text("Câu trả lời") },
                    isError = errorText != null,
                    supportingText = { errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (PinManager.verifyAnswer(context, answer)) {
                            answerVerified = true
                        } else {
                            errorText = "Câu trả lời không đúng"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xác nhận")
                }
            } else {
                Text("Đặt mã PIN mới", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("Mã PIN mới (6 số)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmNewPin = it },
                    label = { Text("Nhập lại mã PIN mới") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        when {
                            newPin.length != 6 -> Toast.makeText(context, "Mã PIN phải đủ 6 số", Toast.LENGTH_SHORT).show()
                            newPin != confirmNewPin -> Toast.makeText(context, "Mã PIN nhập lại không khớp", Toast.LENGTH_SHORT).show()
                            else -> {
                                // Giữ nguyên câu hỏi/câu trả lời bảo mật cũ, chỉ đổi PIN.
                                PinManager.setPin(context, newPin, question, answer)
                                Toast.makeText(context, "Đã đặt lại mật khẩu thành công", Toast.LENGTH_SHORT).show()
                                onPinReset()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đặt lại mật khẩu")
                }
            }
        }
    }
}
