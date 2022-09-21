package com.vg276.musapp.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vg276.musapp.*
import com.vg276.musapp.base.BaseActivity
import com.vg276.musapp.base.BaseFragment
import com.vg276.musapp.base.FragmentSettings
import com.vg276.musapp.core.RequestResult
import com.vg276.musapp.databinding.FragmentSearchBinding
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.ui.adapters.AudioAdapter

class SearchViewFragment: BaseFragment()
{
    private var binding: FragmentSearchBinding? = null
    private lateinit var audioAdapter: AudioAdapter

    private var query = ""
    private var isAllowLoading = true
    private val maxCount = 50

    override fun fragmentSettings(): FragmentSettings {
        return FragmentSettings("", true, R.layout.menu_items_search)
    }

    override fun onMenuItemClick(id: Int) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // create adapter
        audioAdapter = AudioAdapter(AudioAdapterType.OtherAudio) { type, model ->
            if (type == AudioItemClick.Menu)
            {
                (activity as? MainActivity)?.showMenuDialog(
                    model,
                    R.id.SearchViewFragment,
                    showItemGoToArtist = true,
                    showItemGoToAlbum = true
                )
            } else {
                playOrPause(model)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addSearch()

        binding?.let { bind ->

            // setup list
            bind.audioList.layoutManager = LinearLayoutManager(view.context)
            bind.audioList.adapter = audioAdapter
            bind.audioList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // начнем подгружать новые данные когда сколл достигнет конца списка
                    if (!recyclerView.canScrollVertically(1) && isAllowLoading)
                    {
                        receiveAudio(audioAdapter.offset())
                    }
                }
            })
        }

        // playing
        audioPlayer?.playing?.observe(viewLifecycleOwner) {
            audioAdapter.updatePlaying(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        // remove adapter
        audioAdapter.exit()

        super.onDestroy()
    }

    override fun onLibraryList(requestIdentifier: Long, list: ArrayList<AudioModel>?) {

    }

    override fun onAddedToLibrary(requestIdentifier: Long, model: AudioModel?) {
        model?.let {
            audioAdapter.updateIsAdded(it.audioId, true)
        }
    }

    override fun onDeletedFromLibrary(requestIdentifier: Long, audioId: String) {
        audioAdapter.updateIsAdded(audioId, false)
    }

    private fun addSearch()
    {
        menuContainer?.let { menu ->
            val searchEditText = (menu.getChildAt(0) as? EditText)
            val closeButton = (menu.getChildAt(1) as? AppCompatImageButton)

            closeButton?.setOnClickListener {
                searchEditText?.text?.clear()
            }

            searchEditText?.addTextChangedListener {
                query = it.toString()

                if (query.isEmpty())
                    closeButton?.visibility = View.GONE
                else
                    closeButton?.visibility = View.VISIBLE
            }

            searchEditText?.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if (query.isEmpty())
                    {
                        Toast.makeText(requireContext(),
                            R.string.empty_request, Toast.LENGTH_SHORT).show()
                        return@setOnEditorActionListener true
                    }

                    startLoad()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
        }
    }

    private fun playOrPause(model: AudioModel)
    {
        if (audioPlayer?.playedId == model.audioId)
        {
            (activity as? BaseActivity)?.playOrPause()
        } else {
            (activity as? BaseActivity)?.startStream(model, audioAdapter.list as ArrayList<AudioModel>)
        }
    }

    private fun startLoad()
    {
        // hide keyboard
        (menuContainer?.getChildAt(0) as? EditText)?.let {
            hideKeyBoard(it.windowToken)
            it.clearFocus()
        }

        // allow receive audio
        isAllowLoading = true

        // clear list from adapter
        audioAdapter.clear()

        // hint
        binding?.emptyList?.visibility = View.VISIBLE
        binding?.emptyList?.text = getString(R.string.title_loading)

        // start
        receiveAudio(0)
    }

    private fun receiveAudio(offset: Int)
    {
        if (token.isEmpty() || secret.isEmpty() || query.isEmpty())
            return

        if (!isAllowLoading)
            return

        request.searchAudio(token, secret, query, maxCount, offset) { list, result ->
            activity?.runOnUiThread {
                when(result)
                {
                    RequestResult.ErrorInternet ->
                    {
                        Toast.makeText(requireContext(), R.string.error_block_internet, Toast.LENGTH_SHORT).show()
                    }
                    RequestResult.ErrorAvailable ->
                    {
                        Toast.makeText(requireContext(), R.string.error_block_available, Toast.LENGTH_SHORT).show()
                    }
                    RequestResult.ErrorRequest ->
                    {
                        Toast.makeText(requireContext(), R.string.error_block_request, Toast.LENGTH_SHORT).show()
                    }
                    RequestResult.ErrorAuth ->
                    {
                        Toast.makeText(requireContext(), R.string.error_block_request_auth, Toast.LENGTH_SHORT).show()
                    }
                    RequestResult.Success ->
                    {
                        list?.let {
                            if (!it.isNullOrEmpty())
                            {
                                // hide hint
                                binding?.emptyList?.visibility = View.GONE

                                // update list
                                audioAdapter.addAll(it)

                                // Если полученный список будет maxCount, то разрешаем следующую подгрузку
                                isAllowLoading = it.size == maxCount
                            } else {
                                if (audioAdapter.list.isNullOrEmpty())
                                {
                                    binding?.emptyList?.visibility = View.VISIBLE
                                    binding?.emptyList?.text = getString(R.string.empty_list)
                                }
                                isAllowLoading = false
                            }
                        }
                    }
                }
            }
        }

        isAllowLoading = false
    }
}