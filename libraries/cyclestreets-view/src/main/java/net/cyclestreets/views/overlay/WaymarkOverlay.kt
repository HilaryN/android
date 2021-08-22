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
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayItem

import java.util.ArrayList

class WaymarkOverlay(private val mapView: CycleMapView) : Overlay(), PauseResumeListener, Route.Listener {

    private val wispWpStart = makeWisp(R.drawable.wp_green_wisp)
    private val wispWpMid = makeWisp(R.drawable.wp_orange_wisp)
    private val wispWpFinish = makeWisp(R.drawable.wp_red_wisp)
    private val screenPos = Point()
    private val bitmapTransform = Matrix()
    private val bitmapPaint = Paint()
    private val boldTextBrush = Brush.createBoldTextBrush(offset(mapView.getContext()))

    private val waymarkers = ArrayList<OverlayItem>()

    private fun makeWisp(drawable: Int) : Drawable? {
        return ResourcesCompat.getDrawable(mapView.context.resources, drawable, null)
    }

    //////////////////////////////////////
    fun waymarkersCount(): Int {
        return waymarkers.size
    }

    fun waypoints(): Waypoints {
        return Waypoints(waymarkers.map { wp -> wp.point })
    }

    fun finish(): IGeoPoint {
        return waymarkers.last().point
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
        waymarkers.add(makeMarker(point, label, icon))
    }

    private fun popMarker() {
        waymarkers.removeAt(waymarkers.lastIndex)
    }

    private fun makeMarker(point: IGeoPoint, label: String, icon: Drawable?): OverlayItem {
        return OverlayItem(label, label, GeoPoint(point.latitude, point.longitude)).apply {
            setMarker(icon)
            markerHotspot = OverlayItem.HotspotPlace.BOTTOM_CENTER
        }
    }

    ////////////////////////////////////////////
    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        val projection = mapView.projection

        waymarkers.forEach { wp -> drawMarker(canvas, projection, wp, waymarkers.indexOf(wp), waymarkers.size) }
    }

    private fun drawMarker(canvas: Canvas,
                           projection: Projection,
                           marker: OverlayItem,
                           index: Int,
                           size: Int) {

        val waymarkPosition = when (index) {
                                    0 -> "S"                    // Starting waymark
                                    size - 1 -> "F"             // Finishing waymark
                                    else -> {index.toString()}  // Numbered intermediate waymark
        }
        projection.toPixels(marker.point, screenPos)

        val transform = mapView.matrix
        val transformValues = FloatArray(9)
        transform.getValues(transformValues)

        val originalSizeBitmap = getBitmapFromDrawable(marker.drawable)
        val increaseSize = 2.3
        val bitmap = createScaledBitmap(originalSizeBitmap,
                    (originalSizeBitmap.width * increaseSize).toInt(),
                    (originalSizeBitmap.height * increaseSize).toInt(),
                true)

        val halfWidth = bitmap.width / 2
        val halfHeight = bitmap.height / 2

        bitmapTransform.apply {
            setTranslate((-halfWidth).toFloat(), (-halfHeight).toFloat())
            postScale(1 / transformValues[Matrix.MSCALE_X], 1 / transformValues[Matrix.MSCALE_Y])
            postTranslate(screenPos.x.toFloat(), screenPos.y.toFloat())
        }
// todo temp this is just to draw a provisional rectangle so I can see the boundaries of the bitmap:
//        val bounds = Rect()
//        bounds.left = screenPos.x
//        bounds.right = screenPos.x + originalSizeBitmap.width
//        bounds.top = screenPos.y - originalSizeBitmap.height
//        bounds.bottom = screenPos.y
//        canvas.drawRect(screenPos.x.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), Brush.BlackOutline)
        /////////////////////////////////////////////////////////////////////////////

        val x = screenPos.x.toFloat()
        val y = screenPos.y.toFloat()
        canvas.apply {
            save()
            rotate(-projection.orientation, screenPos.x.toFloat(), screenPos.y.toFloat())
            drawBitmap(bitmap, bitmapTransform, bitmapPaint)
            drawText(waymarkPosition, x - halfWidth/10, (y - halfHeight/2), boldTextBrush)
            restore()
        }
    }

    ////////////////////////////////////
    private fun setWaypoints(waypoints: Waypoints) {
        resetWaypoints()

        waypoints.forEach { wp -> addWaypoint(wp) }
    }

    private fun resetWaypoints() {
        waymarkers.clear()
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
