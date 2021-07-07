package net.cyclestreets.views.overlay

import android.content.SharedPreferences
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

class CircularRoutePOIOverlay(mapView: CycleMapView): PauseResumeListener, Route.Listener, // todo may also need Undoable (once bubble functionality added?), MapListener
        LiveItemOverlay<POIOverlay.POIOverlayItem?>(mapView, false) {

    private val controller = mapView.controller

    override fun onResume(prefs: SharedPreferences) {
        Route.registerListener(this)
    }

    override fun onPause(prefs: SharedPreferences.Editor) {
        Route.unregisterListener(this)
    }

    override fun onNewJourney(journey: Journey, waypoints: Waypoints) {
        // TODO display POI's for circular route
        //override fun onPostExecute(pois: List<POI>) {
            val items: MutableList<POIOverlay.POIOverlayItem> = ArrayList()
            for (poi in journey.circularRoutePois) {
                items.add(POIOverlay.POIOverlayItem(poi))
            }
            setItems(items as List<POIOverlay.POIOverlayItem?>?)
    }

    override fun onResetJourney() {
        // TODO clear POI's
        // todo remove circ route pois without removing POIs requested for display
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
    // todo temp comment: Copied from POIOverlay:
    override fun onItemSingleTap(item: POIOverlay.POIOverlayItem?): Boolean {
        // todo comment out following line for now
        //Bubble.hideOrShowBubble(item, controller, this)
        redraw()
        return true
    }
}