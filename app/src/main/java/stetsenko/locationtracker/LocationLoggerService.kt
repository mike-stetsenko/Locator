package stetsenko.locationtracker

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import android.support.v4.content.res.ResourcesCompat
import timber.log.Timber

import javax.inject.Inject

class LocationLoggerService : Service() {

    @Inject
    lateinit internal var locationKeeper: LocationKeeper

    init {
        LocatorApplication.instance.appComponent.inject(this)
    }

    override fun onCreate() {
        super.onCreate()
        startForeground()
        Timber.d("onCreate")
    }

    private fun startForeground() {

        val notificationIntent = Intent(applicationContext, MapsActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        val builder = Notification.Builder(this)
                .setContentTitle(baseContext.getString(R.string.app_name))
                .setContentText("tracking location")
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_place))
                .setContentIntent(PendingIntent.getActivity(applicationContext, 0,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT))

        builder.setSmallIcon(R.drawable.ic_stat_name)
        builder.setColor(ResourcesCompat.getColor(resources, R.color.colorPrimary, null))

        startForeground(799, builder.build())
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
