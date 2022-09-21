package com.vg276.musapp.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.vg276.musapp.R
import com.vg276.musapp.databinding.DialogLoadingBinding

class LoadingView(private val title: String): DialogFragment()
{
    private var binding: DialogLoadingBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // style dialog
        setStyle(STYLE_NO_TITLE, R.style.LoadingDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogLoadingBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.title?.text = title
    }

    override fun onResume() {
        super.onResume()
        val param = dialog?.window?.attributes
        param?.width = ViewGroup.LayoutParams.MATCH_PARENT
        param?.height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog?.window?.attributes = param
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}