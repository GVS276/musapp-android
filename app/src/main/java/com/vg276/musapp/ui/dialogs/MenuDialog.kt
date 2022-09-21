package com.vg276.musapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vg276.musapp.MainActivity
import com.vg276.musapp.R
import com.vg276.musapp.databinding.DialogMenuBinding
import com.vg276.musapp.db.DBController
import com.vg276.musapp.db.model.ArtistModel
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.ui.adapters.ArtistAdapter
import com.vg276.musapp.ui.screens.artist.AlbumViewFragment
import com.vg276.musapp.ui.screens.artist.ArtistViewFragment
import com.vg276.musapp.utils.toTime

class MenuDialog(
    private val model: AudioModel,
    private val currentId: Int,
    private val showItemGoToArtist: Boolean,
    private val showItemGoToAlbum: Boolean): DialogFragment(),
    View.OnClickListener
{
    private var binding: DialogMenuBinding? = null

    private val db = DBController.shared
    private var isAudioAdded = false

    private lateinit var artists: ConstraintLayout
    private lateinit var content: LinearLayout

    private var firstY: Float = 0f
    private var currentPos: Float = 0f
    private var currentHeight: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // style dialog
        setStyle(STYLE_NO_TITLE, R.style.DialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogMenuBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ui
        artists = view.findViewById(R.id.artists)
        artists.visibility = View.GONE

        content = view.findViewById(R.id.content)
        content.visibility = View.VISIBLE

        val back = artists.findViewById<AppCompatImageButton>(R.id.back)
        val item0 = content.findViewById<AppCompatButton>(R.id.item0)
        val item1 = content.findViewById<AppCompatButton>(R.id.item1)
        val item2 = content.findViewById<AppCompatButton>(R.id.item2)

        val peekArtist = content.findViewById<AppCompatTextView>(R.id.peekArtist)
        val peekTitle = content.findViewById<AppCompatTextView>(R.id.peekTitle)
        val peekClose = content.findViewById<AppCompatImageButton>(R.id.peekClose)

        val str = "${model.title} • ${model.duration.toTime()}"
        peekArtist.text = model.artist
        peekTitle.text = str

        if (model.isExplicit)
            peekTitle.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.action_explicit,
                0,
                0,
                0)
        else
            peekTitle.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                0,
                0)

        back.setOnClickListener(this)  // back
        item0.setOnClickListener(this) // add / delete
        item1.setOnClickListener(this) // go to artist
        item2.setOnClickListener(this) // go to album
        peekClose.setOnClickListener(this)

        // swipe to close dialog
        addSwipeToClose(view, content)

        // artist list
        if (!model.artists.isNullOrEmpty())
        {
            val artistList = artists.findViewById<RecyclerView>(R.id.artistList)
            artistList.layoutManager = LinearLayoutManager(view.context)
            artistList.adapter = ArtistAdapter(model.artists) { model ->
                dismiss()
                goToArtist(model)
            }
        }

        // visible items (go to artist / go to album)
        if (showItemGoToArtist)
        {
            item1.visibility = when(model.artists.isNullOrEmpty()) {
                true -> View.GONE
                false -> View.VISIBLE
            }
        } else {
            item1.visibility = View.GONE
        }

        if (showItemGoToAlbum)
        {
            item2.visibility = when(model.albumId.isNotEmpty()) {
                true -> View.VISIBLE
                false -> View.GONE
            }
        } else {
            item2.visibility = View.GONE
        }


        // check model in library
        db.getAudioById(model.audioId) {
            activity?.runOnUiThread {
                isAudioAdded = it != null
                if (isAudioAdded)
                {
                    item0.setText(R.string.title_delete_library)
                    item0.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.action_delete,
                        0)
                } else {
                    item0.setText(R.string.title_add_library)
                    item0.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.action_add,
                        0)
                }
            }
        }
    }

    override fun onClick(p0: View?) {
        when(p0?.id)
        {
            R.id.back ->
            {
                content.visibility = View.VISIBLE
                artists.visibility = View.GONE
            }

            R.id.item0 ->
            {
                dismiss()
                if (isAudioAdded)
                    db.deleteAudio(model.audioId)
                else
                    db.addAudio(model)
            }

            R.id.item1 ->
            {
                if (model.artists.size > 1)
                {
                    artists.visibility = View.VISIBLE
                    content.visibility = View.GONE
                } else {
                    dismiss()
                    goToArtist(model.artists[0])
                }
            }

            R.id.item2 ->
            {
                dismiss()
                gotToAlbum()
            }

            R.id.peekClose ->
            {
                dismiss()
            }

            else -> {}
        }
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

    private fun addSwipeToClose(root: View, content: View?)
    {
        root.setOnTouchListener { v, event ->
            when(event.action)
            {
                MotionEvent.ACTION_DOWN -> {
                    firstY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    firstY = 0f
                    val end = kotlin.math.abs(currentHeight / 2)
                    if (currentPos >= end)
                    {
                        dismiss()
                    } else {
                        content?.animate()?.translationY(0f)
                        v.performClick()
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    content?.let {
                        currentHeight = it.height.toFloat()
                        currentPos = java.lang.Float.min(
                            currentHeight,
                            java.lang.Float.max(0f, event.y - firstY)
                        )
                        it.translationY = currentPos
                    }
                }
                else -> {}
            }
            return@setOnTouchListener true
        }
    }

    private fun goToArtist(model: ArtistModel)
    {
        val bundle = bundleOf(ArtistViewFragment.ARG_ARTIST to model)
        (activity as? MainActivity)?.navigateFragment(
            R.id.ArtistViewFragment, bundle, currentId, false
        )
    }

    private fun gotToAlbum()
    {
        val bundle = bundleOf(
            AlbumViewFragment.ARG_ALBUM_ID to model.albumId,
            AlbumViewFragment.ARG_ALBUM_NAME to model.albumTitle,
            AlbumViewFragment.ARG_ALBUM_ARTIST_NAME to model.artist,
            AlbumViewFragment.ARG_OWNER_ID to model.albumOwnerId,
            AlbumViewFragment.ARG_ACCESS_KEY to model.albumAccessKey
        )
        (activity as? MainActivity)?.navigateFragment(
            R.id.AlbumViewFragment, bundle, currentId, false
        )
    }
}