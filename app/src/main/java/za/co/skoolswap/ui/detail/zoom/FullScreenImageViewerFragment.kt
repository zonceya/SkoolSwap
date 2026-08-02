package za.co.skoolswap.ui.detail.zoom

import android.app.Dialog
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import za.co.skoolswap.R
import za.co.skoolswap.databinding.DialogFullScreenImageViewerBinding
import za.co.skoolswap.ui.detail.adapter.FullScreenImagePagerAdapter

class FullScreenImageViewerDialogFragment : DialogFragment() {

    private var _binding: DialogFullScreenImageViewerBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(
            imageUrls: List<String>,
            startingPosition: Int = 0,
            transitionName: String = ""
        ): FullScreenImageViewerDialogFragment {
            return FullScreenImageViewerDialogFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList("image_urls", ArrayList(imageUrls))
                    putInt("starting_position", startingPosition)
                    putString("transition_name", transitionName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)

        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFullScreenImageViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUrls = arguments?.getStringArrayList("image_urls") ?: arrayListOf()
        val startingPosition = arguments?.getInt("starting_position", 0) ?: 0
        val transitionName = arguments?.getString("transition_name", "") ?: ""

        if (imageUrls.isEmpty()) {
            dismiss()
            return
        }

        val adapter = FullScreenImagePagerAdapter(imageUrls) {
            dismiss()   // single tap anywhere on image dismisses the viewer
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(startingPosition, false)

        updatePositionText(startingPosition + 1, imageUrls.size)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePositionText(position + 1, imageUrls.size)
            }
        })
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnBackContainer) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBar + 8
            }
            insets
        }
        // Single click listener on the container — ImageView has clickable=false so it won't intercept
        binding.btnBackContainer.setOnClickListener {
            dismiss()
        }

        if (transitionName.isNotEmpty()) {
            ViewCompat.setTransitionName(binding.viewPager, transitionName)
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                dismiss()
                return@setOnKeyListener true
            }
            false
        }
        return dialog
    }
    private fun updatePositionText(current: Int, total: Int) {
        binding.tvPosition.text = "$current/$total"
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.black)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}