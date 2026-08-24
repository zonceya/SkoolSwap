package za.co.skoolswap.ui.component

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import za.co.skoolswap.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.*
import timber.log.Timber

abstract class ApiSearchablePickerBottomSheet<T>(
    private val title: String,
    private val placeholder: String,
    private val minSearchLength: Int,
    private val debounceDelay: Long
) : BottomSheetDialogFragment() {

    private var items: List<T> = emptyList()
    private lateinit var adapter: OptionsAdapter
    private var searchJob: Job? = null
    private var searchQuery: String = ""
    private var isCancelled = false

    // Abstract methods
    protected abstract suspend fun performSearch(query: String): List<T>
    protected abstract fun getDisplayName(item: T): String
    protected abstract fun onItemSelected(item: T)
    protected open fun getLogoUrl(item: T): String? = null
    protected open fun hasLogo(item: T): Boolean = false

    // ✅ NEW: Allow subclasses to override the layout
    protected open fun getItemLayoutResId(): Int = R.layout.item_picker_option

    // View references
    private lateinit var tvTitle: TextView
    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvInitialHint: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_api_searchable_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tvTitle)
        searchInput = view.findViewById(R.id.searchInput)
        recyclerView = view.findViewById(R.id.recyclerView)
        tvNoResults = view.findViewById(R.id.tvNoResults)
        progressBar = view.findViewById(R.id.searchProgressBar)
        tvInitialHint = view.findViewById(R.id.tvInitialHint)

        tvTitle.text = title
        searchInput.hint = placeholder

        // ✅ Pass the layout resource ID to the adapter
        adapter = OptionsAdapter(items, getItemLayoutResId()) { position ->
            val item = items[position]
            onItemSelected(item)
            dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchInput.text?.toString()?.trim() ?: ""
                if (query.length >= minSearchLength) {
                    performSearchWithCancellationHandling(query)
                }
                true
            } else {
                false
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                searchQuery = query
                searchJob?.cancel()

                when {
                    query.isEmpty() -> {
                        items = emptyList()
                        adapter.updateList(emptyList())
                        showInitialState()
                    }
                    query.length < minSearchLength -> {
                        showMinLengthHint()
                    }
                    else -> {
                        searchJob = viewLifecycleOwner.lifecycleScope.launch {
                            delay(debounceDelay)
                            if (searchQuery == query && isAdded) {
                                performSearchWithCancellationHandling(query)
                            }
                        }
                    }
                }
            }
        })

        showInitialState()

        searchInput.postDelayed({
            searchInput.requestFocus()
            showKeyboard()
        }, 300)
    }

    private fun performSearchWithCancellationHandling(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val results = withContext(NonCancellable + Dispatchers.IO) {
                    try {
                        performSearch(query)
                    } catch (e: CancellationException) {
                        Timber.tag("ApiSearchablePicker").d("Search was cancelled: ${e.message}")
                        isCancelled = true
                        emptyList()
                    } catch (e: Exception) {
                        Timber.tag("ApiSearchablePicker").e(e, "Search error")
                        throw e
                    }
                }

                if (!isAdded) return@launch
                if (searchQuery != query) {
                    Timber.tag("ApiSearchablePicker").d("Query changed, ignoring stale results")
                    return@launch
                }

                if (isCancelled) {
                    isCancelled = false
                    return@launch
                }

                items = results

                if (results.isEmpty()) {
                    tvNoResults.visibility = View.VISIBLE
                    tvNoResults.text = "No schools found for '$query'.\nTry a shorter or different search term."
                    recyclerView.visibility = View.GONE
                    progressBar.visibility = View.GONE
                    tvInitialHint.visibility = View.GONE
                } else {
                    tvNoResults.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    tvInitialHint.visibility = View.GONE
                    adapter.updateList(results)
                }

            } catch (e: Exception) {
                if (!isAdded) return@launch

                if (e is CancellationException) {
                    Timber.tag("ApiSearchablePicker").d("Search cancelled: ${e.message}")
                    return@launch
                }

                Timber.tag("ApiSearchablePicker").e(e, "Search failed")
                tvNoResults.visibility = View.VISIBLE
                tvNoResults.text = "Search failed: ${e.message}"
                recyclerView.visibility = View.GONE
                progressBar.visibility = View.GONE
                tvInitialHint.visibility = View.GONE
            }
        }
    }

    private fun showInitialState() {
        tvInitialHint.visibility = View.VISIBLE
        tvInitialHint.text = "Type $minSearchLength+ characters to search"
        tvNoResults.visibility = View.GONE
        recyclerView.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun showMinLengthHint() {
        tvInitialHint.visibility = View.VISIBLE
        tvInitialHint.text = "Type $minSearchLength+ characters to search"
        tvNoResults.visibility = View.GONE
        recyclerView.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(android.app.Activity.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
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
        private var items: List<T>,
        private val layoutResId: Int,  // ✅ Accept layout resource ID
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<OptionsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(layoutResId, parent, false)  // ✅ Use the passed layout ID
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
            holder.itemView.setOnClickListener {
                onItemClick(position)
            }
        }

        override fun getItemCount() = items.size

        fun updateList(newItems: List<T>) {
            items = newItems
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val optionText: TextView = itemView.findViewById(R.id.tvOption)
            private val logoImage: android.widget.ImageView? = itemView.findViewById(R.id.ivLogo)

            fun bind(item: T) {
                optionText.text = getDisplayName(item)

                if (logoImage != null && hasLogo(item)) {
                    val logoUrl = getLogoUrl(item)
                    if (!logoUrl.isNullOrEmpty()) {
                        logoImage.visibility = View.VISIBLE
                        com.bumptech.glide.Glide.with(itemView.context)
                            .load(logoUrl)
                            .placeholder(R.drawable.ic_school_placeholder)
                            .error(R.drawable.ic_school_placeholder)
                            .circleCrop()
                            .into(logoImage)
                    } else {
                        logoImage.visibility = View.GONE
                    }
                } else if (logoImage != null) {
                    logoImage.visibility = View.GONE
                }
            }
        }
    }
}