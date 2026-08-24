package com.noteapp.ui.editor

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.noteapp.NoteApplication
import com.noteapp.R
import com.noteapp.data.db.entities.Tag
import com.noteapp.databinding.FragmentEditorBinding
import kotlinx.coroutines.launch

class EditorFragment : Fragment() {

    private var _b: FragmentEditorBinding? = null
    private val b get() = _b!!

    private val vm: EditorViewModel by viewModels {
        EditorViewModelFactory((requireActivity().application as NoteApplication).repository)
    }

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // takePersistableUriPermission chỉ hoạt động với ACTION_OPEN_DOCUMENT
                // Trên Android 9 (Realme/Oppo ColorOS) đôi khi ném SecurityException
                // → bọc try-catch, nếu lỗi vẫn dùng được URI trong session hiện tại
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    android.util.Log.w("EditorFragment", "takePersistableUriPermission failed (OK on Android 9): ${e.message}")
                } catch (e: Exception) {
                    android.util.Log.w("EditorFragment", "takePersistableUriPermission unexpected error: ${e.message}")
                }
                vm.updateBgImage(uri.toString())
                loadBgImage(uri.toString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentEditorBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val noteId = arguments?.getLong("noteId", -1L) ?: -1L
        vm.loadNote(noteId)
        setupMenu()
        observeViewModel()
        setupColorButtons()
        setupImagePicker()
        setupTagSection()
        setupCategoryButton()
        b.btnLock.setOnClickListener { vm.toggleSecure() }
    }

    private fun setupMenu() {
        try {
            b.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
            b.toolbar.inflateMenu(R.menu.menu_editor)
            b.toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_save   -> { saveNote(); true }
                    R.id.action_delete -> { confirmDelete(); true }
                    R.id.action_pin    -> { vm.togglePin(); true }
                    else               -> false
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("EditorFragment", "setupMenu error", t)
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        vm.note.observe(viewLifecycleOwner) { note ->
            if (b.etTitle.text.isNullOrEmpty() && note.title.isNotEmpty()) {
                b.etTitle.setText(note.title)
            }
            if (b.etContent.text.isNullOrEmpty() && note.content.isNotEmpty()) {
                b.etContent.setText(note.content)
            }
            if (!note.backgroundImageUri.isNullOrEmpty()) {
                loadBgImage(note.backgroundImageUri)
            } else {
                b.ivBackground.visibility = View.GONE
                b.editorRoot.setBackgroundColor(note.backgroundColor)
            }
            b.etTitle.setTextColor(note.textColor)
            b.etContent.setTextColor(note.textColor)
            b.btnLock.setImageResource(
                if (note.isSecure) R.drawable.ic_lock_closed else R.drawable.ic_lock_open
            )
        }

        vm.tags.observe(viewLifecycleOwner) { renderTagChips(it) }
    }

    private fun loadBgImage(uriStr: String) {
        b.ivBackground.visibility = View.VISIBLE
        Glide.with(this).load(Uri.parse(uriStr)).centerCrop().into(b.ivBackground)
    }

    // ── Colour pickers ────────────────────────────────────────────────────────

    private val bgColors = listOf(
        0xFFFFFFFF.toInt() to "⬜ Trắng",     0xFFFFF9C4.toInt() to "🟡 Vàng nhạt",
        0xFFFFCDD2.toInt() to "🔴 Hồng nhạt", 0xFFE1F5FE.toInt() to "🔵 Xanh nhạt",
        0xFFE8F5E9.toInt() to "🟢 Xanh lá",   0xFFF3E5F5.toInt() to "🟣 Tím nhạt",
        0xFFFFE0B2.toInt() to "🟠 Cam nhạt",  0xFF212121.toInt() to "⬛ Đen",
        0xFF1A237E.toInt() to "🔵 Xanh đậm",  0xFF1B5E20.toInt() to "🟢 Xanh lá đậm"
    )

    private val textColors = listOf(
        0xFF212121.toInt() to "⬛ Đen",    0xFFFFFFFF.toInt() to "⬜ Trắng",
        0xFF1976D2.toInt() to "🔵 Xanh",   0xFF388E3C.toInt() to "🟢 Xanh lá",
        0xFFD32F2F.toInt() to "🔴 Đỏ",     0xFF7B1FA2.toInt() to "🟣 Tím",
        0xFFF57C00.toInt() to "🟠 Cam",    0xFF795548.toInt() to "🟤 Nâu"
    )

    private fun setupColorButtons() {
        b.btnBgColor.setOnClickListener {
            showColorPicker("Màu nền", bgColors) { vm.updateBackground(it) }
        }
        b.btnTextColor.setOnClickListener {
            showColorPicker("Màu chữ", textColors) { vm.updateTextColor(it) }
        }
        b.btnRemoveBg.setOnClickListener {
            vm.updateBgImage(null)
            b.ivBackground.visibility = View.GONE
        }
    }

    private fun showColorPicker(
        title: String,
        options: List<Pair<Int, String>>,
        onPick: (Int) -> Unit
    ) {
        val labels = options.map { it.second }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(labels) { _, i -> onPick(options[i].first) }
            .show()
    }

    // ── Image picker ──────────────────────────────────────────────────────────

    private fun setupImagePicker() {
        b.btnPickImage.setOnClickListener {
            // ACTION_OPEN_DOCUMENT yêu cầu DocumentsProvider – một số thiết bị Android 9
            // (Realme, Oppo ColorOS cũ) không có hoặc crash khi launch.
            // Dùng ACTION_GET_CONTENT làm primary (rộng hơn, ổn định hơn trên Android 9)
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpeg", "image/webp"))
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            try {
                imagePicker.launch(intent)
            } catch (e: Exception) {
                android.util.Log.e("EditorFragment", "Cannot open image picker", e)
                com.google.android.material.snackbar.Snackbar.make(
                    b.root, "Không thể mở thư viện ảnh", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ── Tags ──────────────────────────────────────────────────────────────────

    private fun setupTagSection() {
        b.btnAddTag.setOnClickListener { showTagPicker() }
    }

    private fun showTagPicker() {
        val allTags  = vm.allTags.value  ?: emptyList()
        val selected = vm.tags.value     ?: emptyList()
        if (allTags.isEmpty()) { showCreateTagDialog(); return }

        val checked  = allTags.map { t -> selected.any { it.id == t.id } }.toBooleanArray()
        val names    = allTags.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chọn tags")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Xong") { _, _ ->
                vm.setSelectedTags(allTags.filterIndexed { i, _ -> checked[i] })
            }
            .setNeutralButton("Tạo tag mới") { _, _ -> showCreateTagDialog() }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showCreateTagDialog() {
        val input = EditText(requireContext()).apply { hint = "Tên tag" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tag mới")
            .setView(input)
            .setPositiveButton("Tạo") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        (requireActivity().application as NoteApplication)
                            .repository.insertTag(Tag(name = name))
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun renderTagChips(tags: List<Tag>) {
        b.chipGroupTags.removeAllViews()
        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text                = "#${tag.name}"
                isCloseIconVisible  = true
                chipBackgroundColor = ColorStateList.valueOf(tag.color)
                setTextColor(Color.WHITE)
            }
            chip.setOnCloseIconClickListener {
                val current = (vm.tags.value ?: emptyList()).toMutableList()
                current.removeAll { it.id == tag.id }
                vm.setSelectedTags(current)
            }
            b.chipGroupTags.addView(chip)
        }
    }

    // ── Category ──────────────────────────────────────────────────────────────

    private fun setupCategoryButton() {
        b.btnCategory.setOnClickListener { showCategoryPicker() }
    }

    private fun showCategoryPicker() {
        val cats      = vm.allCategories.value ?: emptyList()
        val currentId = vm.note.value?.categoryId
        val names     = (listOf("Không có") + cats.map { it.name }).toTypedArray()
        var idx       = if (currentId == null) 0
                        else (cats.indexOfFirst { it.id == currentId } + 1).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chọn danh mục")
            .setSingleChoiceItems(names, idx) { _, i -> idx = i }
            .setPositiveButton("Chọn") { _, _ ->
                val catId = if (idx == 0) null else cats[idx - 1].id
                vm.updateCategory(catId)
                b.btnCategory.text = if (idx == 0) "Danh mục" else cats[idx - 1].name
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ── Save / Delete ─────────────────────────────────────────────────────────

    private fun saveNote() {
        val title   = b.etTitle.text.toString().trim()
        val content = b.etContent.text.toString().trim()
        if (title.isEmpty() && content.isEmpty()) {
            Snackbar.make(b.root, "Ghi chú trống, không lưu", Snackbar.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }
        vm.saveNote(title, content) {
            Snackbar.make(b.root, "Đã lưu", Snackbar.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa ghi chú")
            .setMessage("Ghi chú này sẽ bị xóa vĩnh viễn.")
            .setPositiveButton("Xóa") { _, _ ->
                vm.deleteNote { findNavController().popBackStack() }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        val title   = b.etTitle.text.toString().trim()
        val content = b.etContent.text.toString().trim()
        if (title.isNotEmpty() || content.isNotEmpty()) {
            vm.saveNote(title, content) {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
