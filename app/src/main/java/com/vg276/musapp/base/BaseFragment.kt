package com.vg276.musapp.base

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.vg276.musapp.KEY_SECRET
import com.vg276.musapp.KEY_TOKEN
import com.vg276.musapp.KEY_USER_ID
import com.vg276.musapp.R
import com.vg276.musapp.core.VKRequests
import com.vg276.musapp.db.DBController
import com.vg276.musapp.db.IDBDelegate
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.models.AudioPlayerModel
import com.vg276.musapp.utils.SettingsPreferences
import com.vg276.musapp.utils.applyWindowInsets

data class FragmentSettings(
    val title: String,
    val visibleBack: Boolean,
    val menuResId: Int?
)

abstract class BaseFragment: Fragment()
{
    private lateinit var settings: SettingsPreferences

    protected abstract fun fragmentSettings(): FragmentSettings
    protected abstract fun onMenuItemClick(id: Int)

    protected val db = DBController.shared
    protected val request = VKRequests.shared

    protected val token: String get() {
        return settings.getString(KEY_TOKEN, "")
    }

    protected val secret: String get() {
        return settings.getString(KEY_SECRET, "")
    }

    protected val userId: Int get() {
        return settings.getInt(KEY_USER_ID, -1)
    }

    protected var audioPlayer: AudioPlayerModel? = null
    protected var menuContainer: LinearLayout? = null

    protected fun hideKeyBoard(token: IBinder)
    {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(token, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // preferences
        settings = SettingsPreferences(requireContext())

        // viewModel from store
        audioPlayer = ViewModelProvider(requireActivity())[AudioPlayerModel::class.java]

        // add delegate from db
        db.addDelegate(libraryDelegates)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<LinearLayout>(R.id.toolbar)
        toolbar?.applyWindowInsets { v, insets ->
            val value = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.updatePadding(top = value)
            insets
        }

        val back = toolbar.findViewById<AppCompatImageButton>(R.id.back)
        back.setOnClickListener {
            activity?.onBackPressed()
        }

        val title = toolbar.findViewById<AppCompatTextView>(R.id.title)
        title.text = fragmentSettings().title

        if (title.text.isEmpty())
        {
            title.visibility = View.GONE
        } else {
            title.visibility = View.VISIBLE
        }

        if (fragmentSettings().visibleBack)
        {
            back.visibility = View.VISIBLE
            title.updatePadding(left = 0)
        } else {
            back.visibility = View.GONE
            title.updatePadding(left = resources.getDimensionPixelOffset(R.dimen.toolbar_title_padding))
        }

        fragmentSettings().menuResId?.let {
            inflateMenuItems(it, toolbar)?.let { items ->
                // add menu layout
                val content = toolbar.findViewById<LinearLayout>(R.id.content)
                content.addView(items)

                // add menu items click
                (items as? LinearLayout)?.let { menu ->
                    // container
                    menuContainer = menu

                    // items click
                    for(i in 0 until menu.childCount)
                    {
                        (menu.getChildAt(i) as? AppCompatImageButton)?.setOnClickListener { item ->
                            onMenuItemClick(item.id)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy()
    {
        // remove delegate
        db.removeDelegate(libraryDelegates)
        super.onDestroy()
    }

    private fun inflateMenuItems(resLayout: Int, root: ViewGroup): View?
    {
        context?.let {
            val inflate = LayoutInflater.from(it)
            return inflate.inflate(resLayout, root, false)
        }
        return null
    }

    /*
     * Delegates from DB
     */

    protected abstract fun onLibraryList(requestIdentifier: Long, list: ArrayList<AudioModel>?)
    protected abstract fun onAddedToLibrary(requestIdentifier: Long, model: AudioModel?)
    protected abstract fun onDeletedFromLibrary(requestIdentifier: Long, audioId: String)

    private val libraryDelegates = object : IDBDelegate
    {
        override fun onAudioList(requestIdentifier: Long, list: ArrayList<AudioModel>?) {
            activity?.runOnUiThread {
                onLibraryList(requestIdentifier, list)
            }
        }

        override fun onAudioAdded(requestIdentifier: Long, model: AudioModel?) {
            activity?.runOnUiThread {
                onAddedToLibrary(requestIdentifier, model)
            }
        }

        override fun onAudioDeleted(requestIdentifier: Long, audioId: String) {
            activity?.runOnUiThread {
                onDeletedFromLibrary(requestIdentifier, audioId)
            }
        }
    }
}