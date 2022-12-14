package com.vg276.musapp.ui.screens.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vg276.musapp.*
import com.vg276.musapp.base.BaseActivity
import com.vg276.musapp.base.BaseFragment
import com.vg276.musapp.base.FragmentSettings
import com.vg276.musapp.core.RequestResult
import com.vg276.musapp.databinding.FragmentArtistTracksBinding
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.ui.adapters.AudioAdapter
import com.vg276.musapp.utils.thenNull

class ArtistTracksViewFragment: BaseFragment()
{
    companion object
    {
        const val ARG_ARTIST_ID = "artistId"
    }

    private var binding: FragmentArtistTracksBinding? = null

    private lateinit var audioAdapter: AudioAdapter
    private lateinit var artistId: String

    private var isAllowLoading = true
    private val maxCount = 50

    override fun fragmentSettings(): FragmentSettings {
        return FragmentSettings(getString(R.string.title_tracks), true, null)
    }

    override fun onMenuItemClick(id: Int) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // apply bundle
        arguments?.let {
            artistId = it.getString(ARG_ARTIST_ID, "")
        }.thenNull {
            activity?.onBackPressed()
        }

        // create adapter
        audioAdapter = AudioAdapter(AudioAdapterType.OtherAudio) { type, model ->
            if (type == AudioItemClick.Menu)
            {
                (activity as? MainActivity)?.showMenuDialog(
                    model,
                    R.id.ArtistTracksViewFragment,
                    showItemGoToArtist = false,
                    showItemGoToAlbum = false
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
        binding = FragmentArtistTracksBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

                    // ???????????? ???????????????????? ?????????? ???????????? ?????????? ?????????? ?????????????????? ?????????? ????????????
                    if (!recyclerView.canScrollVertically(1) && isAllowLoading)
                    {
                        receiveAudio(audioAdapter.offset())
                    }
                }
            })
        }

        // load if empty
        if (audioAdapter.list.isNullOrEmpty())
        {
            binding?.emptyList?.visibility = View.VISIBLE
            binding?.emptyList?.text = getString(R.string.title_loading)

            startLoad()
        } else {
            binding?.emptyList?.visibility = View.GONE
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
        isAllowLoading = true
        receiveAudio(0)
    }

    private fun receiveAudio(offset: Int)
    {
        if (!isAllowLoading)
            return

        request.receiveAudioArtist(token, secret, artistId, maxCount, offset) { list, result ->
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

                                // ???????? ???????????????????? ???????????? ?????????? maxCount, ???? ?????????????????? ?????????????????? ??????????????????
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