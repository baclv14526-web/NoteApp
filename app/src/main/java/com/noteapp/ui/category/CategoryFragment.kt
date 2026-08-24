package com.noteapp.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.noteapp.NoteApplication
import com.noteapp.data.db.entities.Category
import com.noteapp.data.db.entities.Tag
import com.noteapp.databinding.FragmentCategoryBinding
import com.noteapp.databinding.ItemCategoryBinding
import com.noteapp.databinding.ItemTagBinding

class CategoryFragment : Fragment() {

    private var _b: FragmentCategoryBinding? = null
    private val b get() = _b!!

    private val vm: CategoryViewModel by viewModels {
        CategoryViewModelFactory((requireActivity().application as NoteApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        try {
            _b = FragmentCategoryBinding.inflate(inflater, container, false)
            return b.root
        } catch (e: Exception) {
            android.util.Log.e("CategoryFragment", "Error inflating FragmentCategoryBinding", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryList()
        setupTagList()
        b.btnAddCategory.setOnClickListener { showCategoryDialog(null) }
        b.btnAddTag.setOnClickListener      { showTagDialog(null) }
    }

    // ── Categories ────────────────────────────────────────────────────────────

    private fun setupCategoryList() {
        val adapter = SimpleCategoryAdapter(
            onEdit   = { cat -> showCategoryDialog(cat) },
            onDelete = { cat ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xóa danh mục \"${cat.name}\"?")
                    .setMessage("Các ghi chú thuộc danh mục này sẽ không bị xóa.")
                    .setPositiveButton("Xóa") { _, _ -> vm.deleteCategory(cat) }
                    .setNegativeButton("Hủy", null).show()
            }
        )
        b.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        b.rvCategories.adapter = adapter
        vm.categories.observe(viewLifecycleOwner) { adapter.setItems(it) }
    }

    private fun showCategoryDialog(existing: Category?) {
        val input = EditText(requireContext()).apply {
            hint = "Tên danh mục"
            if (existing != null) setText(existing.name)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Thêm danh mục" else "Sửa danh mục")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val color = existing?.color ?: 0xFF2196F3.toInt()
                    if (existing == null) vm.insertCategory(name, color)
                    else vm.updateCategory(existing.copy(name = name))
                }
            }
            .setNegativeButton("Hủy", null).show()
    }

    // ── Tags ──────────────────────────────────────────────────────────────────

    private fun setupTagList() {
        val adapter = SimpleTagAdapter(
            onEdit   = { tag -> showTagDialog(tag) },
            onDelete = { tag ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xóa tag \"#${tag.name}\"?")
                    .setPositiveButton("Xóa") { _, _ -> vm.deleteTag(tag) }
                    .setNegativeButton("Hủy", null).show()
            }
        )
        b.rvTags.layoutManager = LinearLayoutManager(requireContext())
        b.rvTags.adapter = adapter
        vm.tags.observe(viewLifecycleOwner) { adapter.setItems(it) }
    }

    private fun showTagDialog(existing: Tag?) {
        val input = EditText(requireContext()).apply {
            hint = "Tên tag"
            if (existing != null) setText(existing.name)
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
            .setNegativeButton("Hủy", null).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

// ── Standalone adapters (outside fragment class) ──────────────────────────────

class SimpleCategoryAdapter(
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<SimpleCategoryAdapter.VH>() {

    private val items = mutableListOf<Category>()

    fun setItems(newItems: List<Category>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = items[position]
        holder.binding.tvName.text = cat.name
        holder.binding.vColor.setBackgroundColor(cat.color)
        holder.binding.btnEdit.setOnClickListener   { onEdit(cat) }
        holder.binding.btnDelete.setOnClickListener { onDelete(cat) }
    }
}

class SimpleTagAdapter(
    private val onEdit: (Tag) -> Unit,
    private val onDelete: (Tag) -> Unit
) : RecyclerView.Adapter<SimpleTagAdapter.VH>() {

    private val items = mutableListOf<Tag>()

    fun setItems(newItems: List<Tag>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemTagBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTagBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tag = items[position]
        holder.binding.tvName.text = "#${tag.name}"
        holder.binding.vColor.setBackgroundColor(tag.color)
        holder.binding.btnEdit.setOnClickListener   { onEdit(tag) }
        holder.binding.btnDelete.setOnClickListener { onDelete(tag) }
    }
}
