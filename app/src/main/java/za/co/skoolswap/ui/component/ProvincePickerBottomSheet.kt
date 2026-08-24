package za.co.skoolswap.ui.component

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import za.co.skoolswap.R
import za.co.skoolswap.domain.model.Province
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProvincePickerBottomSheet(
    private val provinces: List<Province>,
    private val selectedProvinceId: Int? = null,
    private val onProvinceSelected: (Province) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_province_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val tvCancel = view.findViewById<TextView>(R.id.pvCancel)

        tvTitle.text = "Select Province"

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ProvinceAdapter(provinces, selectedProvinceId) { province ->
            onProvinceSelected(province)
            dismiss()
        }

        tvCancel.setOnClickListener {
            dismiss()
        }
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

    inner class ProvinceAdapter(
        private val provinces: List<Province>,
        private val selectedProvinceId: Int?,
        private val onItemClick: (Province) -> Unit
    ) : RecyclerView.Adapter<ProvinceAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_province_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val province = provinces[position]
            holder.tvProvinceName.text = province.name

            // Show checkmark if selected
            val isSelected = province.id == selectedProvinceId
            holder.ivCheckmark.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.itemView.setBackgroundColor(
                if (isSelected) {
                    holder.itemView.context.getColor(R.color.white)
                } else {
                    android.graphics.Color.TRANSPARENT
                }
            )

            holder.itemView.setOnClickListener {
                onItemClick(province)
            }
        }

        override fun getItemCount() = provinces.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvProvinceName: TextView = itemView.findViewById(R.id.tvProvinceName)
            val ivCheckmark: View = itemView.findViewById(R.id.ivCheckmark)
        }
    }
}