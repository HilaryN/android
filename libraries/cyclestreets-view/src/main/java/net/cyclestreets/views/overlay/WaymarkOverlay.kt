package net.cyclestreets.views.overlay

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.graphics.*
import android.graphics.Bitmap.createScaledBitmap
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import net.cyclestreets.routing.Journey
import net.cyclestreets.routing.Route
import net.cyclestreets.routing.Waypoints
import net.cyclestreets.util.Brush
import net.cyclestreets.view.R
import net.cyclestreets.views.CycleMapView
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.OverlayItem

import java.util.ArrayList

class WaymarkOverlay(private val mapView: CycleMapView) : ItemizedOverlay<OverlayItem>(mapView.mapView(), ArrayList(), true),
                                                            PauseResumeListener,
                                                            Route.Listener {

    val INCREASE_WAYMARK_SIZE = 1.5
    val HORIZONTAL_TEXT_POSITION_ADJUSTMENT = 10
    val VERTICAL_TEXT_POSITION_ADJUSTMENT = 1.8
    val REDUCE_TEXT_SIZE = 0.8

    private val wispWpStart = makeWisp(R.drawable.wp_start_wisp)
    private val wispWpMid = makeWisp(R.drawable.wp_mid_wisp)
    private val wispWpFinish = makeWisp(R.drawable.wp_finish_wisp)
    private val screenPos = Point()
    private val bitmapTransform = Matrix()
    private val bitmapPaint = Paint()
    private val waymarkNumberTextBrush = Brush.createBoldTextBrush((offset(mapView.getContext())*REDUCE_TEXT_SIZE).toInt())

    private fun makeWisp(drawable: Int) : Drawable? {
        return ResourcesCompat.getDrawable(mapView.context.resources, drawable, null)
    }

    //////////////////////////////////////
    fun waymarkersCount(): Int {
        return items().size
    }

    fun waypoints(): Waypoints {
        return Waypoints(items().map { wp -> wp.point })
    }

    fun finish(): IGeoPoint {
        return items().last().point
    }

    fun addWaypoint(point: IGeoPoint?) {
        if (point == null)
            return
        when (waymarkersCount()) {
            0 -> pushMarker(point, "start", wispWpStart)
            1 -> pushMarker(point, "finish", wispWpFinish)
            else -> {
                val prevFinished = finish()
                popMarker()
                pushMarker(prevFinished, "waypoint", wispWpMid)
                pushMarker(point, "finish", wispWpFinish)
            }
        }
    }

    fun removeWaypoint() {
        when (waymarkersCount()) {
            0 -> { }
            1, 2 -> popMarker()
            else -> {
                popMarker()
                val prevFinished = finish()
                popMarker()
                pushMarker(prevFinished, "finish", wispWpFinish)
            }
        }
    }

    private fun pushMarker(point: IGeoPoint, label: String, icon: Drawable?) {
        items().add(makeMarker(point, label, icon))
    }

    private fun popMarker() {
        items().removeAt(items().lastIndex)
    }

    private fun makeMarker(point: IGeoPoint, label: String, icon: Drawable?): OverlayItem {
        return OverlayItem(label, label, GeoPoint(point.latitude, point.longitude)).apply {
            setMarker(icon)
            markerHotspot = OverlayItem.HotspotPlace.BOTTOM_CENTER
        }
    }

    ////////////////////////////////////
    private fun setWaypoints(waypoints: Waypoints) {
        resetWaypoints()

        waypoints.forEach { wp -> addWaypoint(wp) }
    }

    private fun resetWaypoints() {
        items().clear()
    }

    ////////////////////////////////////
    override fun onResume(prefs: SharedPreferences) {
        Route.registerListener(this)
    }

    override fun onPause(prefs: Editor) {
        Route.unregisterListener(this)
    }

    override fun onNewJourney(journey: Journey, waypoints: Waypoints) {
        setWaypoints(waypoints)
    }

    override fun onResetJourney() {
        resetWaypoints()
    }
}
