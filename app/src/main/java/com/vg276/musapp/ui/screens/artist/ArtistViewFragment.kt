package com.vg276.musapp.ui.screens.artist

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.vg276.musapp.*
import com.vg276.musapp.base.BaseActivity
import com.vg276.musapp.base.BaseFragment
import com.vg276.musapp.base.FragmentSettings
import com.vg276.musapp.core.RequestResult
import com.vg276.musapp.databinding.FragmentArtistBinding
import com.vg276.musapp.db.model.ArtistModel
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.ui.adapters.AlbumAdapter
import com.vg276.musapp.ui.adapters.AudioAdapter
import com.vg276.musapp.ui.dialogs.MenuDialogBuilder
import com.vg276.musapp.utils.thenNull
import java.lang.Float.min

class ArtistViewFragment: BaseFragment()
{
    companion object
    {
        const val ARG_ARTIST = "artist"
    }

    private var binding: FragmentArtistBinding? = null

    private lateinit var audioAdapter: AudioAdapter
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var artist: ArtistModel

    override fun fragmentSettings(): FragmentSettings {
        return FragmentSettings(artist.name, true, null)
    }

    override fun onMenuItemClick(id: Int) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // apply bundle
        arguments?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                it.getSerializable(ARG_ARTIST, ArtistModel::class.java)?.let { model ->
                    artist = model
                }.thenNull {
                    activity?.onBackPressed()
                }
            } else {
                (it.getSerializable(ARG_ARTIST) as? ArtistModel)?.let { model ->
                    artist = model
                }.thenNull {
                    activity?.onBackPressed()
                }
            }
        }.thenNull {
            activity?.onBackPressed()
        }

        // create tracks adapter
        audioAdapter = AudioAdapter(AudioAdapterType.OtherAudio) { type, model ->
            if (type == AudioItemClick.Menu)
            {
                val builder = MenuDialogBuilder.Builder()
                builder.setVisibleItemGoToArtist(false)
                builder.setVisibleItemGoToAlbum(false)

                (activity as? MainActivity)?.showMenuDialog(model, builder.build())
            } else {
                playOrPause(model)
            }
        }

        // create albums adapter
        albumAdapter = AlbumAdapter { model ->
            val bundle = bundleOf(
                AlbumViewFragment.ARG_ALBUM_ID to model.albumId,
                AlbumViewFragment.ARG_ALBUM_NAME to model.title,
                AlbumViewFragment.ARG_ALBUM_ARTIST_NAME to artist.name,
                AlbumViewFragment.ARG_OWNER_ID to model.ownerId.toString(),
                AlbumViewFragment.ARG_ACCESS_KEY to model.accessKey
            )
            (activity as? MainActivity)?.navigateFragment(
                R.id.AlbumViewFragment, bundle, false
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentArtistBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.let {

            // setup header
            it.headerTitle.text = fragmentSettings().title

            val toolbar = it.root.findViewById<LinearLayout>(R.id.toolbar)
            it.placeholder.alpha = 1f

            // settings header
            it.nestedScroll.isSmoothScrollingEnabled = true
            it.nestedScroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                it.header.translationY = scrollY.toFloat()

                val headerH = it.header.height
                val end = headerH - (toolbar.height)
                val y =  scrollY.toFloat() - end
                val opacity = min(1f, 1 + min(1f, y / end))

                it.placeholder.alpha = 1 - opacity
            }

            // setup track list
            it.recyclerListTracks.layoutManager = LinearLayoutManager(view.context)
            it.recyclerListTracks.adapter = audioAdapter

            // setup album list
            it.recyclerListAlbums.layoutManager = LinearLayoutManager(view.context)
            it.recyclerListAlbums.adapter = albumAdapter

            // click
            it.tracks.setOnClickListener {
                val bundle = bundleOf(ArtistTracksViewFragment.ARG_ARTIST_ID to artist.id)
                (activity as? MainActivity)?.navigateFragment(
                    R.id.ArtistTracksViewFragment, bundle, false
                )
            }

            it.albums.setOnClickListener {
                val bundle = bundleOf(
                    ArtistAlbumsViewFragment.ARG_ARTIST_ID to artist.id,
                    ArtistAlbumsViewFragment.ARG_ARTIST_NAME to artist.name
                )
                (activity as? MainActivity)?.navigateFragment(
                    R.id.ArtistAlbumsViewFragment, bundle, false
                )
            }
        }

        // load if empty
        if (audioAdapter.list.isEmpty())
        {
            binding?.content?.visibility = View.GONE
            binding?.emptyList?.visibility = View.VISIBLE
            binding?.emptyList?.text = getString(R.string.title_loading)

            receiveTracks()
        } else {
            binding?.content?.visibility = View.VISIBLE
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

    override fun onDestroy()
    {
        // remove audio adapter
        audioAdapter.exit()

        // remove album adapter
        albumAdapter.exit()

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

    private fun receiveTracks()
    {
        request.receiveAudioArtist(token, secret, artist.id, 5, 0) { list, result ->
            activity?.runOnUiThread {
                when(result) {
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
                    RequestResult.Success -> {
                        list?.let {
                            if (it.isNotEmpty())
                            {
                                // hide hint and show content
                                binding?.content?.visibility = View.VISIBLE
                                binding?.emptyList?.visibility = View.GONE

                                // update list
                                audioAdapter.addAll(it)

                                // load albums
                                receiveAlbums()
                            }
                            else if (audioAdapter.list.isEmpty()) {
                                binding?.content?.visibility = View.GONE
                                binding?.emptyList?.visibility = View.VISIBLE
                                binding?.emptyList?.text = getString(R.string.empty_list)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun receiveAlbums()
    {
        request.receiveAlbumArtist(token, secret, artist.id, 5, 0) { list, result ->
            activity?.runOnUiThread {
                if (result == RequestResult.Success) {
                    list?.let {
                        if (it.isNotEmpty())
                        {
                            albumAdapter.update(it)
                        }
                    }
                }
            }
        }
    }
}