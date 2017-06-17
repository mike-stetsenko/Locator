package stetsenko.locationtracker.repository.prefs

import android.content.Context
import android.preference.PreferenceManager

class PrefsImpl(context: Context) : Prefs {

    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)!!

    override fun getValue(key: String): Boolean = sharedPref.getBoolean(key, false)

    override fun putValue(key: String, value: Boolean) {
        sharedPref.edit().putBoolean(key, value).apply()
    }
}
