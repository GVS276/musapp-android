package com.vg276.musapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.vg276.musapp.AudioAdapterType
import com.vg276.musapp.AudioItemClick
import com.vg276.musapp.base.FragmentSettings
import com.vg276.musapp.MainActivity
import com.vg276.musapp.R
import com.vg276.musapp.base.BaseActivity
import com.vg276.musapp.base.BaseFragment
import com.vg276.musapp.databinding.FragmentMainBinding
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.ui.adapters.AudioAdapter
import com.vg276.musapp.ui.dialogs.MenuDialogBuilder
import com.vg276.musapp.utils.thenNull

class MainViewFragment: BaseFragment()
{
    private var binding: FragmentMainBinding? = null
    private lateinit var audioAdapter: AudioAdapter

    private var requestReceiveId: Long? = null

    override fun fragmentSettings(): FragmentSettings {
        return FragmentSettings(getString(R.string.title_library), false, R.layout.menu_items_main)
    }

    override fun onMenuItemClick(id: Int) {
        when(id)
        {
            R.id.menu_item_search ->
            {
                (activity as? MainActivity)?.navigateFragment(
                    R.id.SearchViewFragment, null, false
                )
            }

            R.id.menu_item_my ->
            {
                (activity as? MainActivity)?.navigateFragment(
                    R.id.MyMusicFragment, null, false
                )
            }

            R.id.menu_item_settings ->
            {

            }
            else -> {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // create adapter
        audioAdapter = AudioAdapter(AudioAdapterType.OtherAudio) { type, model ->
            if (type == AudioItemClick.Menu)
            {
                val builder = MenuDialogBuilder.Builder()
                builder.setVisibleItemGoToArtist(true)
                builder.setVisibleItemGoToAlbum(true)

                (activity as? MainActivity)?.showMenuDialog(model, builder.build())
            } else {
                playOrPause(model)
            }
        }

        // receive audio from db
        requestReceiveId = db.receiveAudioList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.let { bind ->

            // setup list
            bind.audioList.layoutManager = LinearLayoutManager(view.context)
            bind.audioList.adapter = audioAdapter

            // hint
            if (audioAdapter.list.isEmpty())
            {
                bind.emptyList.visibility = View.VISIBLE
            } else {
                bind.emptyList.visibility = View.GONE
            }
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

    override fun onDestroy()
    {
        // remove adapter
        audioAdapter.exit()

        super.onDestroy()
    }

    override fun onLibraryList(requestIdentifier: Long, list: ArrayList<AudioModel>?)
    {
        if (requestReceiveId == requestIdentifier)
        {
            list?.let {
                if (it.isNotEmpty())
                {
                    if (this::audioAdapter.isInitialized)
                    {
                        audioAdapter.addAll(it)
                    }

                    binding?.emptyList?.visibility = View.GONE
                }
            }
        }
    }

    override fun onAddedToLibrary(requestIdentifier: Long, model: AudioModel?)
    {
        model?.let {
            if (this::audioAdapter.isInitialized)
            {
                audioAdapter.add(it)
            }

            binding?.emptyList?.visibility = View.GONE

            Toast.makeText(requireContext(),
                R.string.title_audio_added, Toast.LENGTH_SHORT).show()
        }.thenNull {
            Toast.makeText(requireContext(),
                R.string.title_audio_not_added, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeletedFromLibrary(requestIdentifier: Long, audioId: String)
    {
        if (this::audioAdapter.isInitialized)
        {
            audioAdapter.delete(audioId)
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
}