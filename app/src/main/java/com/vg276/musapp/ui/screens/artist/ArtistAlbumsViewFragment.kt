package com.vg276.musapp.ui.screens.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vg276.musapp.MainActivity
import com.vg276.musapp.R
import com.vg276.musapp.base.BaseFragment
import com.vg276.musapp.base.FragmentSettings
import com.vg276.musapp.core.RequestResult
import com.vg276.musapp.databinding.FragmentArtistAlbumsBinding
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.ui.adapters.AlbumAdapter
import com.vg276.musapp.utils.thenNull

class ArtistAlbumsViewFragment: BaseFragment()
{
    companion object
    {
        const val ARG_ARTIST_ID = "artistId"
        const val ARG_ARTIST_NAME = "artistName"
    }

    private var binding: FragmentArtistAlbumsBinding? = null

    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var artistId: String
    private lateinit var artistName: String

    private var isAllowLoading = true
    private val maxCount = 50

    override fun fragmentSettings(): FragmentSettings {
        return FragmentSettings(getString(R.string.title_albums), true, null)
    }

    override fun onMenuItemClick(id: Int) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // apply bundle
        arguments?.let {
            artistId = it.getString(ARG_ARTIST_ID, "")
            artistName = it.getString(ARG_ARTIST_NAME, "")
        }.thenNull {
            activity?.onBackPressed()
        }

        // create albums adapter
        albumAdapter = AlbumAdapter { model ->
            val bundle = bundleOf(
                AlbumViewFragment.ARG_ALBUM_ID to model.albumId,
                AlbumViewFragment.ARG_ALBUM_NAME to model.title,
                AlbumViewFragment.ARG_ALBUM_ARTIST_NAME to artistName,
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
        binding = FragmentArtistAlbumsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.let { bind ->

            // setup list
            bind.albumList.layoutManager = LinearLayoutManager(view.context)
            bind.albumList.adapter = albumAdapter
            bind.albumList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // начнем подгружать новые данные когда сколл достигнет конца списка
                    if (!recyclerView.canScrollVertically(1) && isAllowLoading)
                    {
                        receiveAlbum(albumAdapter.offset())
                    }
                }
            })
        }

        // load if empty
        if (albumAdapter.list.isEmpty())
        {
            binding?.emptyList?.visibility = View.VISIBLE
            binding?.emptyList?.text = getString(R.string.title_loading)

            startLoad()
        } else {
            binding?.emptyList?.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        // remove adapter
        albumAdapter.exit()

        super.onDestroy()
    }

    override fun onLibraryList(requestIdentifier: Long, list: ArrayList<AudioModel>?) {

    }

    override fun onAddedToLibrary(requestIdentifier: Long, model: AudioModel?) {

    }

    override fun onDeletedFromLibrary(requestIdentifier: Long, audioId: String) {

    }

    private fun startLoad()
    {
        isAllowLoading = true
        receiveAlbum(0)
    }

    private fun receiveAlbum(offset: Int)
    {
        if (!isAllowLoading)
            return

        request.receiveAlbumArtist(token, secret, artistId, maxCount, offset) { list, result ->
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
                            if (it.isNotEmpty())
                            {
                                // hide hint
                                binding?.emptyList?.visibility = View.GONE

                                // update list
                                albumAdapter.update(it)

                                // Если полученный список будет maxCount, то разрешаем следующую подгрузку
                                isAllowLoading = it.size == maxCount
                            } else {
                                if (albumAdapter.list.isEmpty())
                                {
                                    binding?.emptyList?.visibility = View.VISIBLE
                                    binding?.emptyList?.text = getString(R.string.empty_list_album)
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