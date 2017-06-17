package stetsenko.locationtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.Subscriptions
import stetsenko.locationtracker.LocationKeeper.Companion.isFineLocationPermitted
import stetsenko.locationtracker.repository.db.LocationDatabase
import stetsenko.locationtracker.repository.prefs.Prefs
import stetsenko.locationtracker.repository.prefs.Prefs.Companion.LOCATOR_ENABLED
import javax.inject.Inject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val REQUEST_FINE_LOCATION_PERMISSION = 1
    }

    @Inject
    lateinit internal var locationKeeper: LocationKeeper
    @Inject
    lateinit var db: LocationDatabase // TODO move to ViewModel or Presenter
    @Inject
    lateinit var prefs: Prefs

    private var map: GoogleMap? = null
    private var locationsChanges: Subscription = Subscriptions.unsubscribed()

    val markers: MutableList<Marker> = emptyList<Marker>().toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocatorApplication.instance.appComponent.inject(this)

        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        updateTitle()
    }

    private fun updateTitle() {
        title = if (prefs.getValue(LOCATOR_ENABLED)) "Locator enabled" else "Locator disabled"
    }

    override fun onDestroy() {
        unsubscribeChanges()
        super.onDestroy()
    }

    private fun unsubscribeChanges() {
        if (!locationsChanges.isUnsubscribed) {
            locationsChanges.unsubscribe()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_start -> {
            prefs.putValue(LOCATOR_ENABLED, true)
            startService(Intent(this, LocationLoggerService::class.java))
            subscribeChanges()
            updateTitle()
            true
        }
        R.id.action_stop -> {
            prefs.putValue(LOCATOR_ENABLED, false)
            stopService(Intent(this, LocationLoggerService::class.java))
            unsubscribeChanges()
            updateTitle()
            true
        }
        R.id.action_clear -> {
            markers.forEach(Marker::remove)
            markers.clear()
            db.locationDao().deleteAll()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        requestFineLocationPermission()

        if (isFineLocationPermitted) {
            addMarkers()
        }
    }

    // TODO move to ViewModel or Presenter
    private fun addMarkers(){
        if (map != null){

            map?.isMyLocationEnabled = true
            map?.uiSettings?.isMyLocationButtonEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true

            db.locationDao().all.forEach {
                markers.add(
                        map!!.addMarker(MarkerOptions()
                                .title(it.accuracy.toString())
                                .position(LatLng(it.latitude, it.longitude))))
            }

            if (prefs.getValue(LOCATOR_ENABLED)) {
                subscribeChanges()
            }
        }
    }

    private fun subscribeChanges() {
        if (locationsChanges.isUnsubscribed) {
            locationsChanges = locationKeeper.observeChanges()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        loc ->
                        markers.add(
                                map!!.addMarker(MarkerOptions()
                                        //.icon(iconMarker) TODO
                                        .title(loc.accuracy.toString())
                                        //.anchor(0.0f, 1.0f)  Anchors the marker on the bottom left
                                        .position(LatLng(loc.latitude, loc.longitude))))
                    }, {
                        err ->
                        err.printStackTrace()
                    })
        }
    }

    fun requestFineLocationPermission() {
        if (!isFineLocationPermitted) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                startPermissionDialog(getString(R.string.request_fine_location_permission),
                        Runnable { ActivityCompat.requestPermissions(this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                REQUEST_FINE_LOCATION_PERMISSION) })
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION_PERMISSION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_FINE_LOCATION_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationKeeper.startLocationUpdates()
                    addMarkers()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
        }
    }

    private fun startPermissionDialog(info: String?, runnable: Runnable) {
        AlertDialog.Builder(this)
                .setMessage(info)
                .setPositiveButton(android.R.string.yes) { dialog, whichButton -> runnable.run() }
                .setNegativeButton(android.R.string.no, null).show()
    }
}
