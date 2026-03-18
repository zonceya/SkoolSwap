package com.example.skoolswap.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.skoolswap.R
import com.example.skoolswap.databinding.DialogPriceRangeBinding
import java.text.NumberFormat
import java.util.Locale

class PriceRangeDialogFragment : DialogFragment() {

    private var _binding: DialogPriceRangeBinding? = null
    private val binding get() = _binding!!

    private var minPrice = 0f
    private var maxPrice = 855f
    private var currentMin = 0f
    private var currentMax = 855f

    private var onPriceRangeApplied: ((Float, Float) -> Unit)? = null

    companion object {
        const val TAG = "PriceRangeDialog"

        fun newInstance(
            min: Float = 0f,
            max: Float = 855f,
            currentMin: Float = 0f,
            currentMax: Float = 855f
        ): PriceRangeDialogFragment {
            return PriceRangeDialogFragment().apply {
                arguments = Bundle().apply {
                    putFloat("min_price", min)
                    putFloat("max_price", max)
                    putFloat("current_min", currentMin)
                    putFloat("current_max", currentMax)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // setStyle(STYLE_NORMAL, R.style.ThemeOverlay_App_Dialog_Rounded) // Uncomment if you have this style

        arguments?.let {
            minPrice = it.getFloat("min_price", 0f)
            maxPrice = it.getFloat("max_price", 855f)
            currentMin = it.getFloat("current_min", 0f)
            currentMax = it.getFloat("current_max", 855f)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPriceRangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRangeSlider()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        // Set dialog width to 90% of screen (only need this once)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupRangeSlider() {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        binding.rangeSlider.setValues(currentMin, currentMax)
        binding.rangeSlider.valueFrom = minPrice
        binding.rangeSlider.valueTo = maxPrice

        binding.minPrice.text = currencyFormat.format(currentMin)
        binding.maxPrice.text = currencyFormat.format(currentMax)

        binding.rangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            if (values.size == 2) {
                currentMin = values[0]
                currentMax = values[1]
                binding.minPrice.text = currencyFormat.format(currentMin)
                binding.maxPrice.text = currencyFormat.format(currentMax)
            }
        }
    }

    private fun setupButtons() {
        binding.applyBtn.setOnClickListener {
            // Option 1: Using callback (if you prefer this approach)
            onPriceRangeApplied?.invoke(currentMin, currentMax)

            // Option 2: Using FragmentResult (if you prefer this approach)
            // val result = Bundle().apply {
            //     putFloat("min_price", currentMin)
            //     putFloat("max_price", currentMax)
            // }
            // parentFragmentManager.setFragmentResult("price_range_request", result)

            dismiss()
        }

        binding.clearBtn.setOnClickListener {
            binding.rangeSlider.setValues(minPrice, maxPrice)
            currentMin = minPrice
            currentMax = maxPrice

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            binding.minPrice.text = currencyFormat.format(currentMin)
            binding.maxPrice.text = currencyFormat.format(currentMax)
        }
    }

    fun setOnPriceRangeAppliedListener(listener: (Float, Float) -> Unit) {
        this.onPriceRangeApplied = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}