package com.noteapp.ui.home

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.noteapp.NoteApplication
import com.noteapp.R
import com.noteapp.data.db.entities.Category
import com.noteapp.data.db.entities.Note
import com.noteapp.data.db.entities.Tag
import com.noteapp.databinding.FragmentHomeBinding
import com.noteapp.utils.ExportImportUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    private val vm: HomeViewModel by viewModels {
        HomeViewModelFactory((requireActivity().application as NoteApplication).repository)
    }

    private lateinit var adapter: NoteAdapter
    private var importFormat = "json"
    private var isGridLayout = true

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
                    Snackbar.make(b.root, "Đã nhập $count ghi chú", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupFab()
        setupMenu()
        observeData()
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onClick = { note ->
                if (note.isSecure) {
                    findNavController().navigate(
                        R.id.action_homeFragment_to_pinFragment,
                        bundleOf("noteId" to note.id, "isVerifying" to true)
                    )
                } else {
                    openEditor(note.id)
                }
            },
            onLongClick = { note -> showContextMenu(note) }
        )
        b.recyclerView.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        b.recyclerView.adapter = adapter

        adapter.addLoadStateListener { states ->
            b.progressBar.visibility =
                if (states.refresh is LoadState.Loading) View.VISIBLE else View.GONE
            b.tvEmpty.visibility =
                if (states.refresh is LoadState.NotLoading && adapter.itemCount == 0)
                    View.VISIBLE else View.GONE
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────

    private fun setupSearch() {
        b.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                vm.setSearch(newText.orEmpty())
                return true
            }
        })
    }

    // ── FAB ──────────────────────────────────────────────────────────────────

    private fun setupFab() {
        b.fabAdd.setOnClickListener { openEditor(-1L) }
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu)
            }
            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_export      -> { showExportDialog(); true }
                    R.id.action_import      -> { showImportDialog(); true }
                    R.id.action_toggle_grid -> { toggleLayout(); true }
                    else                    -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun toggleLayout() {
        isGridLayout = !isGridLayout
        val cols = if (isGridLayout) 2 else 1
        b.recyclerView.layoutManager =
            StaggeredGridLayoutManager(cols, StaggeredGridLayoutManager.VERTICAL)
    }

    // ── Observe data ─────────────────────────────────────────────────────────

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.notes.collectLatest { adapter.submitData(it) }
            }
        }
        vm.categories.observe(viewLifecycleOwner) { buildCategoryChips(it) }
        vm.tags.observe(viewLifecycleOwner)       { buildTagChips(it) }
        vm.noteCount.observe(viewLifecycleOwner)  { b.tvNoteCount.text = "$it ghi chú" }
    }

    // ── Chip helpers ──────────────────────────────────────────────────────────
    //
    // ONE signature, ONE callback: (isChecked: Boolean) -> Unit
    // This avoids the type-mismatch between (Boolean)->Unit and ()->Unit.

    private fun addChip(
        parent: com.google.android.material.chip.ChipGroup,
        label: String,
        color: Int,
        initialChecked: Boolean,
        onChecked: (Boolean) -> Unit
    ) {
        val chip = Chip(requireContext()).apply {
            text = label
            isCheckable = true
            isChecked = initialChecked
            chipBackgroundColor = ColorStateList.valueOf(color)
            setTextColor(Color.WHITE)
        }
        chip.setOnCheckedChangeListener { _, checked -> onChecked(checked) }
        parent.addView(chip)
    }

    private fun buildCategoryChips(categories: List<Category>) {
        b.chipGroupCategories.removeAllViews()
        // "All" chip
        addChip(b.chipGroupCategories, "Tất cả", 0xFF607D8B.toInt(), true) { checked ->
            if (checked) vm.setCategory(null)
        }
        categories.forEach { cat ->
            addChip(b.chipGroupCategories, cat.name, cat.color, false) { checked ->
                if (checked) vm.setCategory(cat.id)
            }
        }
    }

    private fun buildTagChips(tags: List<Tag>) {
        b.chipGroupTags.removeAllViews()
        if (tags.isEmpty()) { b.chipGroupTags.visibility = View.GONE; return }
        b.chipGroupTags.visibility = View.VISIBLE
        tags.forEach { tag ->
            addChip(b.chipGroupTags, "#${tag.name}", tag.color, false) { checked ->
                vm.setTag(if (checked) tag.id else null)
            }
        }
    }

    // ── Context menu ─────────────────────────────────────────────────────────

    private fun showContextMenu(note: Note) {
        val opts = arrayOf(
            if (note.isPinned) "Bỏ ghim" else "Ghim lên đầu",
            "Chia sẻ",
            "Xóa"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(note.title.ifEmpty { "Ghi chú" })
            .setItems(opts) { _, i ->
                when (i) {
                    0 -> vm.togglePin(note)
                    1 -> shareNote(note)
                    2 -> confirmDelete(note)
                }
            }.show()
    }

    private fun confirmDelete(note: Note) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa ghi chú")
            .setMessage("Bạn có chắc muốn xóa \"${note.title.ifEmpty { "ghi chú này" }}\"?")
            .setPositiveButton("Xóa") { _, _ ->
                vm.deleteNote(note)
                Snackbar.make(b.root, "Đã xóa", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null).show()
    }

    private fun shareNote(note: Note) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
        }
        startActivity(Intent.createChooser(intent, "Chia sẻ ghi chú"))
    }

    // ── Export / Import ──────────────────────────────────────────────────────

    private fun showExportDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xuất dữ liệu")
            .setItems(arrayOf("Xuất file .txt", "Xuất file .json")) { _, i ->
                val fmt = if (i == 0) "txt" else "json"
                lifecycleScope.launch {
                    ExportImportUtil(
                        requireContext(),
                        (requireActivity().application as NoteApplication).repository
                    ).exportNotes(fmt)
                }
            }.show()
    }

    private fun showImportDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nhập dữ liệu")
            .setItems(arrayOf("Nhập file .txt", "Nhập file .json")) { _, i ->
                importFormat = if (i == 0) "txt" else "json"
                val mime  = if (i == 0) "text/plain" else "application/json"
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = mime
                }
                importLauncher.launch(intent)
            }.show()
    }

    private fun openEditor(noteId: Long) {
        findNavController().navigate(
            R.id.action_homeFragment_to_editorFragment,
            bundleOf("noteId" to noteId)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
