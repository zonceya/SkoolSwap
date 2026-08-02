// ui/products/PriceRangeDialogFragment.kt
package za.co.skoolswap.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import za.co.skoolswap.databinding.DialogPriceRangeBinding

class PriceRangeDialogFragment : DialogFragment() {

    private var _binding: DialogPriceRangeBinding? = null
    private val binding get() = _binding!!

    private var minPrice = 0f
    private var maxPrice = 1000f
    private var currentMin = 0f
    private var currentMax = 1000f
    private var onPriceRangeApplied: ((Float, Float) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setStyle(STYLE_NORMAL, R.style.Theme_MaterialComponents_Light_Dialog)
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

        // Get arguments
        arguments?.let {
            minPrice = it.getFloat("min_price", 0f)
            maxPrice = it.getFloat("max_price", 1000f)
            currentMin = it.getFloat("current_min", minPrice)
            currentMax = it.getFloat("current_max", maxPrice)
        }

        setupPriceSlider()
        setupButtons()
        updatePriceDisplay()
    }

    private fun setupPriceSlider() {
        binding.rangeSlider.apply {
            valueFrom = minPrice
            valueTo = maxPrice
            setValues(currentMin, currentMax)

            addOnChangeListener { slider, _, _ ->
                // Get the current values from the slider
                val values = slider.values
                if (values.size >= 2) {
                    currentMin = values[0]
                    currentMax = values[1]
                    updatePriceDisplay()
                }
            }
        }
    }

    private fun updatePriceDisplay() {
        binding.minPrice.text = currentMin.toInt().toString()
        binding.maxPrice.text = currentMax.toInt().toString()
    }

    private fun setupButtons() {
        binding.clearBtn.setOnClickListener {
            currentMin = minPrice
            currentMax = maxPrice
            binding.rangeSlider.setValues(minPrice, maxPrice)
            updatePriceDisplay()
        }

        binding.applyBtn.setOnClickListener {
            onPriceRangeApplied?.invoke(currentMin, currentMax)
            dismiss()
        }
    }

    fun setOnPriceRangeAppliedListener(listener: (Float, Float) -> Unit) {
        onPriceRangeApplied = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PriceRangeDialogFragment"

        fun newInstance(
            min: Float = 0f,
            max: Float = 1000f,
            currentMin: Float = 0f,
            currentMax: Float = 1000f
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
}