package net.cyclestreets.views.overlay

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.graphics.*
import android.graphics.Bitmap.createScaledBitmap
import android.graphics.drawable.Drawable
import android.widget.Toast
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
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace

import java.util.ArrayList

class WaymarkOverlay(private val mapView: CycleMapView) : ItemizedOverlay<OverlayItem>(mapView.mapView(), ArrayList()),
                                                            PauseResumeListener,
                                                            Route.Listener {

    private val wispWpStart = makeWisp(R.drawable.wp_green_wisp)
    private val wispWpMid = makeWisp(R.drawable.wp_orange_wisp)
    private val wispWpFinish = makeWisp(R.drawable.wp_red_wisp)
    private val screenPos = Point()
    private val bitmapTransform = Matrix()
    private val bitmapPaint = Paint()
    private val boldTextBrush = Brush.createBoldTextBrush((offset(mapView.getContext())*0.8).toInt())

    var rect_ = Rect()

//    private val waymarkers = ArrayList<OverlayItem>()

    private val CHANGE_WAYMARK_SIZE = 0.6

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
            //markerHotspot = OverlayItem.HotspotPlace.CENTER
        }
    }

    override fun onItemSingleTap(item: OverlayItem?): Boolean {
        //return super.onItemSingleTap(item)
        Toast.makeText(mapView.context, "Waymark tapped", Toast.LENGTH_LONG).show()
        return true
    }

    ////////////////////////////////////////////
    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        super.draw(canvas, mapView, shadow)
/*        val projection = mapView.projection

        //waymarkers.forEach { wp -> drawMarker(canvas, projection, wp, waymarkers.indexOf(wp), waymarkers.size) }
        waymarkers.forEach { wp -> run {projection.toPixels(wp.point, screenPos)
            onDrawItem(canvas, wp, screenPos, ((mapView.getContext().getResources().getDisplayMetrics().density * CHANGE_WAYMARK_SIZE).toFloat()),
                mapView.getMapOrientation())} } */
    }

//    private fun onDrawItem(
//        canvas: Canvas,
//        item: OverlayItem,
//        curScreenCoords: Point,
//        scale: Float,
//        mapOrientation: Float
//    ) {
//        val hotspot: HotspotPlace = item.getMarkerHotspot()
//        val marker: Drawable = item.getMarker(0)
//        boundToHotspot(marker, hotspot, scale)
//        val x = curScreenCoords.x
//        val y = curScreenCoords.y
//        val matrix: Matrix = mapView.getMatrix()
//        val matrixValues = FloatArray(9)
//        matrix.getValues(matrixValues)
//        val scaleX = Math.sqrt(
//            (matrixValues[Matrix.MSCALE_X]
//                    * matrixValues[Matrix.MSCALE_X] + matrixValues[Matrix.MSKEW_Y]
//                    * matrixValues[Matrix.MSKEW_Y]).toDouble()
//        ).toFloat()
//        val scaleY = Math.sqrt(
//            (matrixValues[Matrix.MSCALE_Y]
//                    * matrixValues[Matrix.MSCALE_Y] + matrixValues[Matrix.MSKEW_X]
//                    * matrixValues[Matrix.MSKEW_X]).toDouble()
//        ).toFloat()
//        canvas.save()
//        canvas.rotate(-mapOrientation, x.toFloat(), y.toFloat())
//        canvas.scale(1 / scaleX, 1 / scaleY, x.toFloat(), y.toFloat())
//
//        val halfWidth = (marker.intrinsicWidth).toFloat() / 2
//        val halfHeight = marker.intrinsicHeight / 2
//
//        marker.copyBounds(rect_)
//        marker.setBounds(rect_.left + x, rect_.top + y, rect_.right + x, rect_.bottom + y)
//        //marker.hei
//        marker.draw(canvas)
//        canvas.drawText("X", x - halfWidth/10, (y - halfHeight/1.8).toFloat(), boldTextBrush)
//        marker.bounds = rect_
//        canvas.restore()
//    }
//
//    private fun boundToHotspot(
//        marker: Drawable,
//        hotspot: HotspotPlace,
//        scale: Float
//    ): Drawable? {
//        var hotspot: HotspotPlace? = hotspot
//        val markerWidth = (marker.intrinsicWidth * scale).toInt()
//        val markerHeight = (marker.intrinsicHeight * scale).toInt()
//        rect_.set(0, 0, markerWidth, markerHeight)
//        if (hotspot == null) hotspot = HotspotPlace.BOTTOM_CENTER
//        when (hotspot) {
//            HotspotPlace.NONE -> {
//            }
//            HotspotPlace.CENTER -> rect_.offset(-markerWidth / 2, -markerHeight / 2)
//            //HotspotPlace.BOTTOM_CENTER -> rect_.offset(-markerWidth / 2, -markerHeight)
//            HotspotPlace.BOTTOM_CENTER -> rect_.offset(-markerWidth / 2, -markerHeight / 2)
//            HotspotPlace.TOP_CENTER -> rect_.offset(-markerWidth / 2, 0)
//            HotspotPlace.RIGHT_CENTER -> rect_.offset(-markerWidth, -markerHeight / 2)
//            HotspotPlace.LEFT_CENTER -> rect_.offset(0, -markerHeight / 2)
//            HotspotPlace.UPPER_RIGHT_CORNER -> rect_.offset(-markerWidth, 0)
//            HotspotPlace.LOWER_RIGHT_CORNER -> rect_.offset(-markerWidth, -markerHeight)
//            HotspotPlace.UPPER_LEFT_CORNER -> rect_.offset(0, 0)
//            HotspotPlace.LOWER_LEFT_CORNER -> rect_.offset(0, markerHeight)
//            else -> {
//            }
//        }
//        marker.bounds = rect_
//        return marker
//    }
//
//    private fun drawMarker(canvas: Canvas,
//                           projection: Projection,
//                           marker: OverlayItem,
//                           index: Int,
//                           size: Int) {
//
//        val waymarkPosition = when (index) {
//                                    0 -> "S"                    // Starting waymark
//                                    size - 1 -> "F"             // Finishing waymark
//                                    else -> {index.toString()}  // Numbered intermediate waymark
//        }
//        val INCREASE_WAYMARK_SIZE = 1.5
//
//        projection.toPixels(marker.point, screenPos)
//
//        val transform = mapView.matrix
//        val transformValues = FloatArray(9)
//        transform.getValues(transformValues)
//
//        val originalSizeBitmap = getBitmapFromDrawable(marker.drawable)
//        val bitmap = createScaledBitmap(originalSizeBitmap,
//                    (originalSizeBitmap.width * INCREASE_WAYMARK_SIZE).toInt(),
//                    (originalSizeBitmap.height * INCREASE_WAYMARK_SIZE).toInt(),
//                true)
//
//        val halfWidth = bitmap.width / 2
//        val halfHeight = bitmap.height / 2
//
//        bitmapTransform.apply {
//            setTranslate((-halfWidth).toFloat(), (-halfHeight).toFloat())
//            postScale(1 / transformValues[Matrix.MSCALE_X], 1 / transformValues[Matrix.MSCALE_Y])
//            postTranslate(screenPos.x.toFloat(), screenPos.y.toFloat())
//        }
//
//        val x = screenPos.x.toFloat()
//        val y = screenPos.y.toFloat()
//        canvas.apply {
//            save()
//            rotate(-projection.orientation, x, y)
//            drawBitmap(bitmap, bitmapTransform, bitmapPaint)
//            drawText(waymarkPosition, x - halfWidth/10, (y - halfHeight/1.8).toFloat(), boldTextBrush)
//            restore()
//        }
//    }

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
