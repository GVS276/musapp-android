package com.vg276.musapp.ui.adapters

import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vg276.musapp.R
import com.vg276.musapp.databinding.ItemAlbumBinding
import com.vg276.musapp.db.model.AlbumModel
import com.vg276.musapp.thumb.ThumbCache

class AlbumAdapter(
    private val clickable: (model: AlbumModel) -> Unit):
    RecyclerView.Adapter<AlbumAdapter.ViewHolder>()
{
    val list = ArrayList<AlbumModel>()

    inner class ViewHolder(private val bind: ItemAlbumBinding): RecyclerView.ViewHolder(bind.root)
    {
        fun bind(model: AlbumModel)
        {
            val tracks = if (model.countTracks > 1)
                "${model.countTracks} ${itemView.context.getString(R.string.title_tracks)}"
            else
                itemView.context.getString(R.string.title_single)

            val str = "${model.year} • ${tracks.lowercase()}"
            bind.title.text = model.title
            bind.info.text = str

            itemView.setOnClickListener {
                clickable(model)
            }

            // thumbs
            ThumbCache.load(model.thumb, model.albumId) { albumId, bitmap ->
                Handler(itemView.context.mainLooper).post {
                    if (albumId.isEmpty() && bitmap == null)
                    {
                        bind.thumb.setImageResource(R.drawable.thumb_album)
                        return@post
                    }

                    if (albumId.isNotEmpty() && bitmap == null)
                    {
                        bind.thumb.setImageResource(R.drawable.thumb_album)
                        return@post
                    }

                    bind.thumb.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        val binding = ItemAlbumBinding.inflate(inflate, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun offset(): Int
    {
        return list.size - 1
    }

    fun update(received: ArrayList<AlbumModel>)
    {
        val pos = list.size
        list.addAll(received)
        notifyItemInserted(pos)
    }

    fun exit()
    {
        list.clear()
    }
}