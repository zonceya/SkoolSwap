package za.co.skoolswap.ui.component

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import za.co.skoolswap.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SearchableOptionsPickerBottomSheet(
    private val title: String,
    private val options: List<String>,
    private val onOptionSelected: (String, Int) -> Unit
) : BottomSheetDialogFragment() {

    private var filteredOptions: List<String> = options
    private lateinit var adapter: OptionsAdapter
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_searchable_options_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val searchInput = view.findViewById<EditText>(R.id.searchInput)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val tvNoResults = view.findViewById<TextView>(R.id.tvNoResults)

        tvTitle.text = title

        adapter = OptionsAdapter(filteredOptions) { option, position ->
            val originalPosition = options.indexOf(option)
            onOptionSelected(option, originalPosition)
            dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                filterOptions()
            }
        })

        if (options.isEmpty()) {
            tvNoResults.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
    }

    private fun filterOptions() {
        filteredOptions = if (searchQuery.isEmpty()) {
            options
        } else {
            options.filter { it.lowercase().contains(searchQuery) }
        }

        val tvNoResults = view?.findViewById<TextView>(R.id.tvNoResults)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)

        if (filteredOptions.isEmpty()) {
            tvNoResults?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
        } else {
            tvNoResults?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        }

        adapter.updateList(filteredOptions)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                it.setBackgroundResource(R.drawable.bottom_sheet_rounded)
            }
        }

        return dialog
    }

    inner class OptionsAdapter(
        private var options: List<String>,
        private val onItemClick: (String, Int) -> Unit
    ) : RecyclerView.Adapter<OptionsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_picker_option, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.optionText.text = options[position]
            holder.itemView.setOnClickListener {
                onItemClick(options[position], position)
            }
        }

        override fun getItemCount() = options.size

        fun updateList(newOptions: List<String>) {
            options = newOptions
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val optionText: TextView = itemView.findViewById(R.id.tvOption)
        }
    }

    companion object {
        fun newInstance(
            title: String,
            options: List<String>,
            onOptionSelected: (String, Int) -> Unit
        ): SearchableOptionsPickerBottomSheet {
            return SearchableOptionsPickerBottomSheet(title, options, onOptionSelected)
        }
    }
}