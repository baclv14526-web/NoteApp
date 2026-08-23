package com.noteapp.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.noteapp.NoteApplication
import com.noteapp.R
import com.noteapp.databinding.FragmentSettingsBinding
import com.noteapp.utils.ExportImportUtil
import com.noteapp.utils.SecurityManager
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    private lateinit var security: SecurityManager
    private var importFormat = "json"

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                lifecycleScope.launch {
                    val util = ExportImportUtil(
                        requireContext(),
                        (requireActivity().application as NoteApplication).repository
                    )
                    val count = util.importFromUri(uri, importFormat)
                    Toast.makeText(context, "Đã nhập $count ghi chú", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSettingsBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        security = SecurityManager(requireContext())
        setupPinSection()
        setupExportImport()
        updatePinStatus()
    }

    private fun setupPinSection() {
        b.btnSetPin.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_pinFragment,
                bundleOf("isSettingPin" to true)
            )
        }
        b.btnClearPin.setOnClickListener {
            if (!security.hasPin()) {
                Toast.makeText(context, "Chưa có mã PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa mã PIN")
                .setMessage("Tất cả ghi chú bảo mật sẽ không còn được bảo vệ.")
                .setPositiveButton("Xóa PIN") { _, _ ->
                    security.clearPin()
                    updatePinStatus()
                    Toast.makeText(context, "Đã xóa mã PIN", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null).show()
        }
    }

    private fun updatePinStatus() {
        b.tvPinStatus.text = if (security.hasPin()) "✅ Đã cài mã PIN" else "❌ Chưa có mã PIN"
        b.tvBiometricStatus.text =
            if (security.isBiometricAvailable(requireContext())) "✅ Vân tay khả dụng"
            else "❌ Thiết bị không hỗ trợ"
    }

    private fun setupExportImport() {
        b.btnExportTxt.setOnClickListener  { doExport("txt") }
        b.btnExportJson.setOnClickListener { doExport("json") }
        b.btnImportTxt.setOnClickListener  { pickFile("txt") }
        b.btnImportJson.setOnClickListener { pickFile("json") }
    }

    private fun doExport(fmt: String) {
        lifecycleScope.launch {
            ExportImportUtil(
                requireContext(),
                (requireActivity().application as NoteApplication).repository
            ).exportNotes(fmt)
        }
    }

    private fun pickFile(fmt: String) {
        importFormat = fmt
        val mime = if (fmt == "txt") "text/plain" else "application/json"
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
        }
        importLauncher.launch(intent)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
