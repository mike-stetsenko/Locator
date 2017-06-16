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
import rx.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val REQUEST_FINE_LOCATION_PERMISSION = 1

        val isFineLocationPermitted get() = isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)

        fun isPermissionGranted(permission: String): Boolean {
            return ContextCompat.checkSelfPermission(LocatorApplication.instance, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    @Inject
    lateinit internal var locationKeeper: LocationKeeper
    private var mMap: GoogleMap? = null

    val markers: MutableList<Marker> = emptyList<Marker>().toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocatorApplication.instance.appComponent.inject(this)

        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_start -> {
            startService(Intent(this, LocationLoggerService::class.java))
            true
        }
        R.id.action_stop -> {
            stopService(Intent(this, LocationLoggerService::class.java))
            true
        }
        R.id.action_clear -> {
            markers.forEach(Marker::remove)
            markers.clear()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        locationKeeper.observeChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    loc -> markers.add(
                        googleMap.addMarker(MarkerOptions()
                                //.icon(iconMarker) TODO
                                .title(loc.accuracy.toString())
                                //.anchor(0.0f, 1.0f) // Anchors the marker on the bottom left
                                .position(LatLng(loc.latitude, loc.longitude))))
                }, {
                    err -> err.printStackTrace()
                })

        requestFineLocationPermission()

        if (isFineLocationPermitted) {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
            googleMap.uiSettings.isZoomControlsEnabled = true
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

    private fun startPermissionDialog(info: String?, runnable: Runnable) {
        AlertDialog.Builder(this)
                .setMessage(info)
                .setPositiveButton(android.R.string.yes) { dialog, whichButton -> runnable.run() }
                .setNegativeButton(android.R.string.no, null).show()
    }
}
