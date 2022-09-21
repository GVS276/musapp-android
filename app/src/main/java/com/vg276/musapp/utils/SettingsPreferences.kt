package com.vg276.musapp.utils

import android.content.Context
import android.content.SharedPreferences

class SettingsPreferences(private val context: Context)
{
    private fun getPreferences() : SharedPreferences? {
        return context.getSharedPreferences("SettingsPreferences", Context.MODE_PRIVATE)
    }

    fun put(key: String, value: String)
    {
        val editor = getPreferences()?.edit()
        editor?.putString(key, value)
        editor?.apply()
    }

    fun getString(key: String, defVal: String): String {
        return getPreferences()?.getString(key, defVal) ?: defVal
    }

    fun put(key: String, value: Int)
    {
        val editor = getPreferences()?.edit()
        editor?.putInt(key, value)
        editor?.apply()
    }

    fun getInt(key: String, defVal: Int): Int {
        return getPreferences()?.getInt(key, defVal) ?: defVal
    }

    fun put(key: String, value: Boolean)
    {
        val editor = getPreferences()?.edit()
        editor?.putBoolean(key, value)
        editor?.apply()
    }

    fun getBoolean(key: String, defVal: Boolean): Boolean {
        return getPreferences()?.getBoolean(key, defVal) ?: defVal
    }

    fun has(key: String): Boolean {
        return getPreferences()?.contains(key) ?: false
    }
}