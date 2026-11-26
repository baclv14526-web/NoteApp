package com.example.noteapp.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Ô nhập liệu dùng chung cho toàn app, thay cho OutlinedTextField mặc định:
 *  - Bo góc 16dp, nền dịu khi chưa chọn, nền sáng + viền màu chủ đạo khi đang nhập.
 *  - Nhãn nổi (label) + gợi ý (placeholder) + dòng chú thích/báo lỗi bên dưới.
 *  - Nút ✕ xoá nhanh nội dung ([showClear]).
 *
 * Dùng chung một chỗ để mọi ô nhập trong app (category, tag...) đồng bộ giao diện
 * và sửa kiểu dáng chỉ cần sửa tại đây.
 */
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showClear: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val scheme = MaterialTheme.colorScheme

    val clearButton: @Composable () -> Unit = {
        IconButton(onClick = { onValueChange("") }) {
            Icon(Icons.Default.Close, contentDescription = "Xoá nội dung")
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        leadingIcon = leading,
        trailingIcon = if (showClear && value.isNotEmpty()) clearButton else trailing,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surface,
            unfocusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.45f),
            errorContainerColor = scheme.errorContainer.copy(alpha = 0.25f),
            focusedBorderColor = scheme.primary,
            unfocusedBorderColor = scheme.outlineVariant,
            cursorColor = scheme.primary,
            focusedLabelColor = scheme.primary,
            focusedLeadingIconColor = scheme.primary,
            unfocusedLeadingIconColor = scheme.onSurfaceVariant,
            focusedPlaceholderColor = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = scheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    )
}
