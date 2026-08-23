package com.noteapp.ui.category

import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.noteapp.NoteApplication
import com.noteapp.R
import com.noteapp.data.db.entities.Category
import com.noteapp.data.db.entities.Tag
import com.noteapp.databinding.FragmentCategoryBinding
import com.noteapp.databinding.ItemCategoryBinding
import android.view.LayoutInflater

class CategoryFragment : Fragment() {

    private var _b: FragmentCategoryBinding? = null
    private val b get() = _b!!

    private val vm: CategoryViewModel by viewModels {
        CategoryViewModelFactory((requireActivity().application as NoteApplication).repository)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentCategoryBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        setupCategoryList()
        setupTagList()
        setupAddButtons()
    }

    // ── Categories ───────────────────────────────────────────────────────────

    private fun setupCategoryList() {
        val adapter = CategoryListAdapter(
            onEdit   = { cat -> showEditCategoryDialog(cat) },
            onDelete = { cat ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xóa danh mục")
                    .setMessage("Xóa \"${cat.name}\"? Các ghi chú sẽ không bị xóa.")
                    .setPositiveButton("Xóa") { _, _ -> vm.deleteCategory(cat) }
                    .setNegativeButton("Hủy", null).show()
            }
        )
        b.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        b.rvCategories.adapter = adapter
        vm.categories.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    private fun setupAddButtons() {
        b.btnAddCategory.setOnClickListener { showAddCategoryDialog() }
        b.btnAddTag.setOnClickListener { showAddTagDialog() }
    }

    private fun showAddCategoryDialog() {
        showCategoryDialog(null)
    }

    private fun showEditCategoryDialog(cat: Category) {
        showCategoryDialog(cat)
    }

    private fun showCategoryDialog(existing: Category?) {
        val input = EditText(requireContext()).apply {
            hint = "Tên danh mục"
            existing?.let { setText(it.name) }
        }
        val colors = listOf(
            0xFF2196F3.toInt(), 0xFF4CAF50.toInt(), 0xFFFF5722.toInt(),
            0xFF9C27B0.toInt(), 0xFFFF9800.toInt(), 0xFF607D8B.toInt(),
            0xFFE91E63.toInt(), 0xFF00BCD4.toInt()
        )
        var selectedColor = existing?.color ?: colors[0]

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Thêm danh mục" else "Sửa danh mục")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (existing == null) vm.insertCategory(name, selectedColor)
                    else vm.updateCategory(existing.copy(name = name, color = selectedColor))
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ── Tags ─────────────────────────────────────────────────────────────────

    private fun setupTagList() {
        val adapter = TagListAdapter(
            onEdit   = { tag -> showEditTagDialog(tag) },
            onDelete = { tag ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xóa tag")
                    .setMessage("Xóa \"#${tag.name}\"?")
                    .setPositiveButton("Xóa") { _, _ -> vm.deleteTag(tag) }
                    .setNegativeButton("Hủy", null).show()
            }
        )
        b.rvTags.layoutManager = LinearLayoutManager(requireContext())
        b.rvTags.adapter = adapter
        vm.tags.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    private fun showAddTagDialog() { showTagDialog(null) }
    private fun showEditTagDialog(tag: Tag) { showTagDialog(tag) }

    private fun showTagDialog(existing: Tag?) {
        val input = EditText(requireContext()).apply {
            hint = "Tên tag"
            existing?.let { setText(it.name) }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Thêm tag" else "Sửa tag")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val color = existing?.color ?: 0xFF4CAF50.toInt()
                    if (existing == null) vm.insertTag(name, color)
                    else vm.updateTag(existing.copy(name = name))
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

// ── Inline adapters ──────────────────────────────────────────────────────────

class CategoryListAdapter(
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoryListAdapter.VH>() {

    private var list = emptyList<Category>()

    fun submitList(newList: List<Category>) {
        list = newList
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = list[position]
        holder.b.tvName.text = cat.name
        holder.b.vColor.setBackgroundColor(cat.color)
        holder.b.btnEdit.setOnClickListener   { onEdit(cat) }
        holder.b.btnDelete.setOnClickListener { onDelete(cat) }
    }
}

class TagListAdapter(
    private val onEdit: (Tag) -> Unit,
    private val onDelete: (Tag) -> Unit
) : RecyclerView.Adapter<TagListAdapter.VH>() {

    private var list = emptyList<Tag>()

    fun submitList(newList: List<Tag>) {
        list = newList
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tag = list[position]
        holder.b.tvName.text = "#${tag.name}"
        holder.b.vColor.setBackgroundColor(tag.color)
        holder.b.btnEdit.setOnClickListener   { onEdit(tag) }
        holder.b.btnDelete.setOnClickListener { onDelete(tag) }
    }
}
