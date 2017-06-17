package stetsenko.locationtracker.repository.prefs

interface Prefs {

    companion object {
        const val LOCATOR_ENABLED: String = "LOCATOR_ENABLED"
    }

    fun putValue(key: String, value: Boolean)
    fun getValue(key: String): Boolean
}
