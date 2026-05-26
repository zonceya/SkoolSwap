package com.example.skoolswap.ui.component

import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.R
import com.example.skoolswap.utils.ColorUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.skoolswap.domain.model.reference.Color
class ColorPickerBottomSheet(
    private val colors: List<Color>,
    private val onColorSelected: (Color, Int) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_color_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        tvTitle.text = "Select Color"

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ColorAdapter(colors) { color, position ->
            onColorSelected(color, position)
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

    inner class ColorAdapter(
        private val colors: List<Color>,
        private val onItemClick: (Color, Int) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_color_picker, parent, false)
            return ColorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            holder.bind(colors[position], position)
        }

        override fun getItemCount() = colors.size

        inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val colorPreview = itemView.findViewById<View>(R.id.colorPreview)
            private val colorName = itemView.findViewById<TextView>(R.id.colorName)

            fun bind(color: Color, position: Int) {
                colorName.text = color.name
                val colorInt = ColorUtils.getColorInt(color.name)

                // Get the drawable and tint it — preserves the oval shape
                val drawable = ContextCompat.getDrawable(itemView.context, R.drawable.color_circle)?.mutate()
                (drawable as? GradientDrawable)?.setColor(colorInt)
                colorPreview.background = drawable

                itemView.setOnClickListener {
                    onItemClick(color, position)
                }
            }
        }
    }
}