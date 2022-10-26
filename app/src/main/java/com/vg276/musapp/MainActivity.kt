package com.vg276.musapp

import android.content.*
import android.os.Bundle
import androidx.activity.addCallback
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.vg276.musapp.base.BaseActivity
import com.vg276.musapp.db.model.AudioModel
import com.vg276.musapp.ui.dialogs.MenuDialog
import com.vg276.musapp.utils.SettingsPreferences
import com.vg276.musapp.utils.systemBarsTransparent
import com.vg276.musapp.view.LoadingView

class MainActivity: BaseActivity()
{
    private lateinit var settings: SettingsPreferences
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.systemBarsTransparent()

        initPlayer()

        navController = findNavController(R.id.nav_host_fragment)

        initGraph()
        initBackPressed()

        if (intent != null && intent.hasExtra(INTENT_TYPE))
            handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            when(it.getIntExtra(INTENT_TYPE, 0))
            {
                INTENT_TYPE_PLAYER -> showPlayerSheet()
                else -> {}
            }
            it.removeExtra(INTENT_TYPE)
        }
    }

    private fun initBackPressed()
    {
        onBackPressedDispatcher.addCallback(this, true) {
            if (isPlayerSheet())
            {
                hidePlayerSheet()
            }
            else if (!navController.popBackStack()) {
                finish()
            }
        }
    }

    private fun initGraph()
    {
        settings = SettingsPreferences(baseContext)
        if (settings.has("token") && settings.has("secret") && settings.has("userId")) {
            setStartGraph(R.id.MainFragment)
        } else {
            setStartGraph(R.id.LoginFragment)
        }
    }

    fun setStartGraph(id: Int)
    {
        val inf = navController.navInflater
        val graph = inf.inflate(R.navigation.main_navigation)

        graph.setStartDestination(id)
        navController.setGraph(graph, null)
    }

    fun navigateFragment(toId: Int,
                         bundle: Bundle?,
                         currentId: Int,
                         inclusive: Boolean)
    {
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .setPopUpTo(currentId, inclusive).build()

        navController.navigate(toId, bundle, navOptions, null)
    }

    fun showMenuDialog(model: AudioModel, currentId: Int,
                       showItemGoToArtist: Boolean, showItemGoToAlbum: Boolean)
    {
        val transaction = supportFragmentManager.beginTransaction()
        val dialog = MenuDialog(model, currentId, showItemGoToArtist, showItemGoToAlbum)
        dialog.show(transaction, null)
    }

    fun showLoadingView(title: String)
    {
        val transaction = supportFragmentManager.beginTransaction()
        val dialog = LoadingView(title)
        dialog.show(transaction, "LoadingView")
    }

    fun hideLoadingView()
    {
        supportFragmentManager.findFragmentByTag("LoadingView")?.let {
            (it as? LoadingView)?.dismiss()
        }
    }
}