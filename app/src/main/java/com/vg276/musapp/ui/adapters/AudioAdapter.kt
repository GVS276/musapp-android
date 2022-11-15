package com.vg276.musapp.ui.adapters

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vg276.musapp.AudioAdapterType
import com.vg276.musapp.AudioItemClick
import com.vg276.musapp.R
import com.vg276.musapp.databinding.ItemAudioBinding
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.models.AudioPlaying
import com.vg276.musapp.thumb.ThumbCache
import com.vg276.musapp.utils.toTime

class AudioAdapter(
    private val type: AudioAdapterType,
    private val clickable: (type: AudioItemClick, model: AudioModel) -> Unit):
    RecyclerView.Adapter<AudioAdapter.ViewHolder>()
{
    // original list
    val list: MutableList<AudioModel> = ArrayList()

    // map holders <audioId, holder>
    private val listMap: MutableMap<String, ViewHolder> = HashMap()

    // old playing model
    private var oldPlaying = AudioPlaying("", false)

    inner class ViewHolder(private val bind: ItemAudioBinding): RecyclerView.ViewHolder(bind.root)
    {
        fun bind(model: AudioModel)
        {
            when(type)
            {
                AudioAdapterType.AudioFromAlbum ->
                {
                    bind.thumb.visibility = View.GONE

                    bind.artist.text = model.title
                    bind.title.text = model.duration.toTime()
                }

                AudioAdapterType.OtherAudio ->
                {
                    bind.thumb.visibility = View.VISIBLE

                    bind.artist.text = model.artist
                    bind.title.text = model.title
                }
            }

            bind.menu.setOnClickListener {
                clickable(AudioItemClick.Menu, model)
            }

            itemView.setOnClickListener {
                clickable(AudioItemClick.Item, model)
            }

            if (model.isExplicit)
                bind.title.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.action_explicit,
                    0,
                    0,
                    0)
            else
                bind.title.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    0)

            if (oldPlaying.audioId == model.audioId)
                setPlaying(oldPlaying.isPlaying)
            else
                setPlaying(false)

            // thumbs
            ThumbCache.load(model.thumb, model.albumId) { albumId, bitmap ->
                Handler(itemView.context.mainLooper).post {
                    if (albumId.isEmpty() && bitmap == null)
                    {
                        bind.thumb.setImageResource(R.drawable.thumb)
                        return@post
                    }

                    if (albumId.isNotEmpty() && bitmap == null)
                    {
                        bind.thumb.setImageResource(R.drawable.thumb)
                        return@post
                    }

                    bind.thumb.setImageBitmap(bitmap)
                }
            }
        }

        fun setPlaying(value: Boolean)
        {
            itemView.setBackgroundResource(when(value) {
                true -> R.drawable.background_audio_playing
                false -> R.drawable.background_audio_not_playing
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        val binding = ItemAudioBinding.inflate(inflate, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        val model = list[position]

        holder.bind(model)

        listMap[model.audioId] = holder
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun offset(): Int
    {
        return list.size - 1
    }

    fun addAll(received: ArrayList<AudioModel>)
    {
        // get start pos for insert new values to list
        val pos = list.size

        // add new values to list
        list.addAll(received)

        // update recycler view
        notifyItemInserted(pos)
    }

    fun add(model: AudioModel)
    {
        // insert new model to 0 pos
        list.add(0, model)

        // update recycler view
        notifyItemInserted(0)
    }

    fun delete(audioId: String)
    {
        val index = getIndex(audioId)
        if (index != -1)
        {
            // remove holder from map
            listMap.remove(list[index].audioId)

            // remove model from list by index
            list.removeAt(index)

            // update recycler view
            notifyItemRemoved(index)
        }
    }

    fun clear()
    {
        // get last pos
        val pos = list.size

        // full clear
        list.clear()

        // update recycler view
        notifyItemRangeRemoved(0, pos)
    }

    fun updatePlaying(playing: AudioPlaying)
    {
        if (playing.audioId.isNotEmpty())
        {
            // old
            if (oldPlaying.audioId.isNotEmpty())
                listMap[oldPlaying.audioId]?.setPlaying(false)

            // current
            listMap[playing.audioId]?.setPlaying(playing.isPlaying)

            // update old
            oldPlaying = playing
        }
    }

    fun updateIsAdded(audioId: String, added: Boolean)
    {
        val index = getIndex(audioId)
        if (index != -1)
        {
            list[index].isAddedToLibrary = added
        }
    }

    fun exit()
    {
        list.clear()
        listMap.clear()
    }

    private fun getIndex(audioId: String): Int
    {
        return list.indexOfFirst { it.audioId == audioId }
    }
}