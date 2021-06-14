package net.cyclestreets.views.overlay

import android.content.SharedPreferences
import net.cyclestreets.api.POI
import net.cyclestreets.routing.Journey
import net.cyclestreets.routing.Route
import net.cyclestreets.routing.Waypoints
import net.cyclestreets.util.Logging
import net.cyclestreets.views.CycleMapView
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.BoundingBox


private val TAG = Logging.getTag(CircularRoutePOIOverlay::class.java)

class CircularRoutePOIOverlay(mapView: CycleMapView): POIOverlay(mapView), PauseResumeListener, Route.Listener {

    override fun onResume(prefs: SharedPreferences) {
        Route.registerListener(this)
    }

    override fun onPause(prefs: SharedPreferences.Editor) {
        Route.unregisterListener(this)
    }

    override fun onNewJourney(journey: Journey, waypoints: Waypoints) {
        // TODO display POI's for circular route
        //override fun onPostExecute(pois: List<POI>) {
            val items: MutableList<POIOverlayItem> = ArrayList()
            for (poi in journey.circularRoutePois) {
                items.add(POIOverlayItem(poi))
            }
            setItems(items as List<POIOverlayItem?>?)
    }

    override fun onResetJourney() {
        // TODO clear POI's'
    }

/*    override fun fetchItemsInBackground(mapCentre: IGeoPoint,
                                        zoom: Int,
                                        boundingBox: BoundingBox): Boolean {
        //todo - don't need to do anything here.
        // Return false so that "Loading" message doesn't appear.
        return false
    }
*/
}