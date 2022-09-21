package com.vg276.musapp.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vg276.musapp.databinding.ItemArtistBinding
import com.vg276.musapp.db.model.ArtistModel

class ArtistAdapter(
    private val list: ArrayList<ArtistModel>,
    private val clickable: (model: ArtistModel) -> Unit):
    RecyclerView.Adapter<ArtistAdapter.ViewHolder>()
{
    inner class ViewHolder(private val bind: ItemArtistBinding): RecyclerView.ViewHolder(bind.root)
    {
        fun bind(model: ArtistModel)
        {
            bind.artist.text = model.name
            itemView.setOnClickListener {
                clickable(model)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        val binding = ItemArtistBinding.inflate(inflate, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}