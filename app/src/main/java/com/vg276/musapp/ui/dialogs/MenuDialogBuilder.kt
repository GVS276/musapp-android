package com.vg276.musapp.ui.dialogs

class MenuDialogBuilder(builder: Builder)
{
    val visibleItemGoToArtist: Boolean = builder.visibleItemGoToArtist
    val visibleItemGoToAlbum: Boolean = builder.visibleItemGoToAlbum

    class Builder
    {
        var visibleItemGoToArtist: Boolean = false
        var visibleItemGoToAlbum: Boolean = false

        fun setVisibleItemGoToArtist(value: Boolean): Builder
        {
            this.visibleItemGoToArtist = value
            return this
        }

        fun setVisibleItemGoToAlbum(value: Boolean): Builder
        {
            this.visibleItemGoToAlbum = value
            return this
        }

        fun build(): MenuDialogBuilder
        {
            return MenuDialogBuilder(this)
        }
    }
}