package stetsenko.locationtracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import stetsenko.locationtracker.repository.db.Location as DbLocation
import rx.Observable
import rx.subjects.PublishSubject
import stetsenko.locationtracker.repository.db.LocationDatabase
import stetsenko.locationtracker.repository.prefs.Prefs
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.inject.Inject

class LocationKeeper : GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    companion object {
        private val LOCATION_UPDATE_INTERVAL = 10000L
        private val MIN_LOCATION_UPDATE_INTERVAL = 5000L
        private val MAX_LOCATION_UPDATE_INTERVAL = 15000L

        val isFineLocationPermitted get() = isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)

        fun isPermissionGranted(permission: String): Boolean {
            return ContextCompat.checkSelfPermission(LocatorApplication.instance, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private val locationManager: LocationManager
    private var scheduledService: ScheduledExecutorService? = null

    private val locations = PublishSubject.create<Location>()

    @Inject
    lateinit var context: Context
    @Inject
    lateinit var db: LocationDatabase
    @Inject
    lateinit var prefs: Prefs

    fun observeChanges(): Observable<Location> = locations

    val googleApiClient: GoogleApiClient by lazy {
        Timber.d("LocationKeeper", "googleApiClient lazy init")
        GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }

    init {
        LocatorApplication.instance.appComponent.inject(this)

        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        googleApiClient.connect()
        Timber.d("googleApiClient->connect()")
    }

    fun closeConnections() {
        if (googleApiClient.isConnected) {
            googleApiClient.disconnect()
            stopLocationUpdates()
        }
    }

    override fun onConnected(connectionHint: Bundle?) {
        Timber.d("googleApiClient->onConnected !!")

        startLocationUpdates()

        scheduledService?.shutdown()
    }

    override fun onConnectionSuspended(i: Int) {
        Timber.d("googleApiClient->onConnectionSuspended")

        googleApiClient.connect()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Timber.e(Exception("Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.errorCode))

        startScheduleSaveLocationRequest()
    }

    private fun startScheduleSaveLocationRequest() {
        scheduledService = Executors.newSingleThreadScheduledExecutor()

        /*scheduledService?.scheduleWithFixedDelay(
                { startSaveLocationRequest(lastKnownLocation) }, 0, 1, TimeUnit.MINUTES) // location request every minute TODO*/
    }

    private fun locationProvidersEnabled(): Boolean =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    // cause there isn't activity context here, we should ask 4 permissions in another place
    // can never be accessed after permissionChecker evaluation
    val lastKnownLocation: Location?
        get() {
            return if (googleApiClient.isConnected) {

                if (!isFineLocationPermitted) return null

                try {
                    LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
                } catch (e: SecurityException) {
                    null
                }

            } else {
                locationFromManager
            }
        }

    private // can never be accessed after permissionChecker evaluation
            // Found best last known location
    val locationFromManager: Location?
        get() {
            if (!isFineLocationPermitted) {
                return null
            }
            return locationManager.getProviders(true)
                    .map { locationManager.getLastKnownLocation(it) }
                    .filterNotNull()
                    .minBy { it.accuracy }
        }

    override fun onLocationChanged(location: Location) {
        if (prefs.getValue(Prefs.LOCATOR_ENABLED)) {
            db.locationDao().insertLocation(DbLocation().apply {
                time = currentTimeSec()
                latitude = location.latitude
                longitude = location.longitude
                accuracy = location.accuracy
            })
            Timber.d("googleApiClient->onLocationChanged ${location.latitude} ${location.longitude} ${location.accuracy}")
            locations.onNext(location)
        }
    }

    fun startLocationUpdates() {
        if (!isFineLocationPermitted) {
            return
        }

        val locationRequest = LocationRequest().apply {
            interval = LOCATION_UPDATE_INTERVAL
            maxWaitTime = MAX_LOCATION_UPDATE_INTERVAL
            fastestInterval = MIN_LOCATION_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this).setResultCallback {
                res -> Timber.d("startLocationUpdates ${res.isSuccess}")
            }

        } catch (e: SecurityException) {
            // can never be accessed after permissionChecker evaluation
        }
    }

    fun stopLocationUpdates() {
        if (googleApiClient.isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
        }
    }
}
