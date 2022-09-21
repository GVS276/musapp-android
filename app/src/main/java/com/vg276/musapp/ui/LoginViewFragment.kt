package com.vg276.musapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.vg276.musapp.*
import com.vg276.musapp.core.RequestResult
import com.vg276.musapp.core.VKRequests
import com.vg276.musapp.databinding.FragmentLoginBinding
import com.vg276.musapp.utils.SettingsPreferences
import com.vg276.musapp.utils.applyWindowInsets

class LoginViewFragment: Fragment()
{
    private val model = VKRequests.shared
    private var binding: FragmentLoginBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val top = view.paddingTop
        val bottom = view.paddingTop

        view.applyWindowInsets { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                top = top + bars.top,
                bottom = bottom + bars.bottom
            )

            insets
        }

        binding?.let { bind ->
            bind.startButton.setOnClickListener {
                val login = bind.login.text.toString()
                val password = bind.password.text.toString()

                if (login.isEmpty() || password.isEmpty())
                {
                    Toast.makeText(requireContext(), R.string.empty_login_or_password, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                startLogin(login, password)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun startLogin(login: String, password: String)
    {
        showLoadingView()
        model.doAuth(login, password) { info, result ->
            activity?.runOnUiThread {
                when(result)
                {
                    RequestResult.ErrorInternet ->
                    {
                        hideLoadingView()
                        Toast.makeText(requireContext(), R.string.error_block_internet, Toast.LENGTH_SHORT).show()
                    }
                    RequestResult.ErrorAvailable ->
                    {
                        hideLoadingView()
                        Toast.makeText(requireContext(), R.string.error_block_available, Toast.LENGTH_SHORT).show()
                    }
                    RequestResult.ErrorRequest ->
                    {
                        hideLoadingView()
                        Toast.makeText(requireContext(), R.string.error_block_request, Toast.LENGTH_SHORT).show()
                    }
                    RequestResult.ErrorAuth ->
                    {
                        hideLoadingView()
                        Toast.makeText(requireContext(), R.string.error_block_auth, Toast.LENGTH_SHORT).show()
                    }
                    RequestResult.Success ->
                    {
                        info?.let {
                            refreshToken(it.access_token, it.secret, it.user_id)
                        }
                    }
                }
            }
        }
    }

    private fun refreshToken(token: String, secret: String, userId: Int)
    {
        model.refreshToken(token, secret) { response, result ->
            activity?.runOnUiThread {
                hideLoadingView()

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
                        response?.let {
                            val settings = SettingsPreferences(requireContext())
                            settings.put(KEY_TOKEN, it.response.token)
                            settings.put(KEY_SECRET, it.response.secret)
                            settings.put(KEY_USER_ID, userId)
                            showMain()
                        }
                    }
                }
            }
        }
    }

    private fun showMain()
    {
        (activity as? MainActivity)?.setStartGraph(R.id.MainFragment)
    }

    private fun showLoadingView()
    {
        (activity as? MainActivity)?.showLoadingView(getString(R.string.title_authorization))
    }

    private fun hideLoadingView()
    {
        (activity as? MainActivity)?.hideLoadingView()
    }
}