package com.vg276.musapp.ui.screens.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.vg276.musapp.*
import com.vg276.musapp.base.BaseActivity
import com.vg276.musapp.base.BaseFragment
import com.vg276.musapp.base.FragmentSettings
import com.vg276.musapp.core.RequestResult
import com.vg276.musapp.databinding.FragmentAlbumBinding
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.thumb.ThumbCache
import com.vg276.musapp.ui.adapters.AudioAdapter
import com.vg276.musapp.ui.dialogs.MenuDialogBuilder
import com.vg276.musapp.utils.thenNull
import java.lang.Float.min

class AlbumViewFragment: BaseFragment()
{
    companion object
    {
        const val ARG_ALBUM_ID = "albumId"
        const val ARG_ALBUM_NAME = "albumName"
        const val ARG_ALBUM_ARTIST_NAME = "albumArtistName"
        const val ARG_OWNER_ID = "ownerId"
        const val ARG_ACCESS_KEY = "accessKey"
    }

    private var binding: FragmentAlbumBinding? = null

    private lateinit var albumId: String
    private lateinit var albumName: String
    private lateinit var albumArtistName: String
    private lateinit var ownerId: String
    private lateinit var accessKey: String
    private lateinit var audioAdapter: AudioAdapter

    override fun fragmentSettings(): FragmentSettings {
        return FragmentSettings(albumName, true, null)
    }

    override fun onMenuItemClick(id: Int) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // apply bundle
        arguments?.let {
            albumId = it.getString(ARG_ALBUM_ID, "")
            albumName = it.getString(ARG_ALBUM_NAME, "")
            albumArtistName = it.getString(ARG_ALBUM_ARTIST_NAME, "")
            ownerId = it.getString(ARG_OWNER_ID, "")
            accessKey = it.getString(ARG_ACCESS_KEY, "")
        }.thenNull {
            activity?.onBackPressed()
        }

        // create adapter
        audioAdapter = AudioAdapter(AudioAdapterType.AudioFromAlbum) { type, model ->
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
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAlbumBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.let {

            // setup header
            it.headerTitle.text = fragmentSettings().title
            it.headerSubTitle.text = albumArtistName

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

            // setup audio list
            it.audioList.layoutManager = LinearLayoutManager(view.context)
            it.audioList.adapter = audioAdapter

            // thumbs
            ThumbCache.getImage(requireContext(), albumId)?.let { bitmap ->
                it.placeholder.setImageBitmap(bitmap)
            }.thenNull {
                it.placeholder.setImageResource(R.drawable.album)
            }
        }

        // load if empty
        if (audioAdapter.list.isEmpty())
        {
            binding?.emptyList?.visibility = View.VISIBLE
            binding?.emptyList?.text = getString(R.string.title_loading)

            receiveAudio()
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

    private fun receiveAudio()
    {
        request.getAudioFromAlbum(token, secret, ownerId.toInt(), accessKey, albumId) { list, result ->
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
                                // hide hint
                                binding?.emptyList?.visibility = View.GONE

                                // update list
                                audioAdapter.addAll(it)
                            } else {
                                binding?.emptyList?.visibility = View.VISIBLE
                                binding?.emptyList?.text = getString(R.string.empty_list)
                            }
                        }
                    }
                }
            }
        }
    }
}