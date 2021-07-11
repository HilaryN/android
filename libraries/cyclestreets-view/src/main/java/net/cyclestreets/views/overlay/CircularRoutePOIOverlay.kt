package net.cyclestreets.views.overlay

import android.content.SharedPreferences
import net.cyclestreets.Undoable
import net.cyclestreets.api.POI
import net.cyclestreets.routing.Journey
import net.cyclestreets.routing.Route
import net.cyclestreets.routing.Waypoints
import net.cyclestreets.util.Logging
import net.cyclestreets.views.CycleMapView
import org.osmdroid.api.IGeoPoint
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox


private val TAG = Logging.getTag(CircularRoutePOIOverlay::class.java)

class CircularRoutePOIOverlay(mapView: CycleMapView): PauseResumeListener, Route.Listener,
        LiveItemOverlay<POIOverlay.POIOverlayItem?>(mapView, false), Undoable {

    private var journey: Journey? = null

    override fun onResume(prefs: SharedPreferences) {
        Route.registerListener(this)
    }

    override fun onPause(prefs: SharedPreferences.Editor) {
        Route.unregisterListener(this)
    }

    override fun onNewJourney(newJourney: Journey, waypoints: Waypoints) {
            removePois()
            journey = newJourney
            val items: MutableList<POIOverlay.POIOverlayItem> = ArrayList()
            for (poi in journey!!.circularRoutePois) {
                items.add(POIOverlay.POIOverlayItem(poi))
            }
            setItems(items as List<POIOverlay.POIOverlayItem?>?)
    }

    override fun onResetJourney() {
        removePois()
    }

    private fun removePois() {
        Bubble.hideBubble(this)
        // Remove Circular Route POI's from display
        if (journey != null) {
            // In reverse order, otherwise it errors on last one:
            for (i in items().indices.reversed()) {
                if (items()[i]!!.poi in journey!!.circularRoutePois) {
                    items().removeAt(i)
                }
            }
            journey = null
        }
    }

    override fun fetchItemsInBackground(mapCentre: IGeoPoint,
                                        zoom: Int,
                                        boundingBox: BoundingBox): Boolean {
        //todo - don't need to do anything here.  Need to consider naming of this function in super class as inaccurate name in this case
        // Return false so that "Loading" message doesn't appear?
        return false
    }

    override fun onZoom(event: ZoomEvent): Boolean {
        // Don't want any of the functionality in the superclass so override and do nothing / return true
        return true
    }

    override fun onScroll(event: ScrollEvent): Boolean {
        // Don't want any of the functionality in the superclass, so override and do nothing / return true
        return true
    }

    override fun onItemSingleTap(item: POIOverlay.POIOverlayItem?): Boolean {
        Bubble.hideOrShowBubble(item, this)
        redraw()
        return true
    }

    override fun onBackPressed(): Boolean {
        Bubble.hideBubble(this)
        redraw()
        return true
    }
}