package net.cyclestreets.routing

import android.content.Context
import net.cyclestreets.routing.Journey.Companion.NULL_JOURNEY
import net.cyclestreets.routing.NULL_WAYPOINTS
import net.cyclestreets.routing.Journey.Companion.loadFromJson
import net.cyclestreets.routing.Route.Listeners
import net.cyclestreets.routing.Waypoints
import net.cyclestreets.routing.CycleStreetsRoutingTask
import net.cyclestreets.routing.LiveRideReplanRoutingTask
import net.cyclestreets.routing.FetchCycleStreetsRouteTask
import net.cyclestreets.routing.ReplanRoutingTask
import net.cyclestreets.routing.StoredRoutingTask
import net.cyclestreets.routing.Journey
import net.cyclestreets.CycleStreetsPreferences
import net.cyclestreets.content.RouteSummary
import net.cyclestreets.content.RouteData
import android.widget.Toast
import android.content.SharedPreferences
import android.util.Log
import net.cyclestreets.content.RouteDatabase
import net.cyclestreets.util.Logging
import net.cyclestreets.view.R
import java.lang.Exception
import java.util.ArrayList

object Route {
    private val TAG = Logging.getTag(Route::class.java)
    private val listeners_ = Listeners()
    @JvmStatic
    fun registerListener(l: Listener) {
        listeners_.register(l)
    }

    fun softRegisterListener(l: Listener) {
        listeners_.softRegister(l)
    }

    @JvmStatic
    fun unregisterListener(l: Listener?) {
        listeners_.unregister(l)
    }

    @JvmStatic
    fun PlotRoute(
        plan: String?,
        speed: Int,
        context: Context?,
        waypoints: Waypoints?
    ) {
        val query = CycleStreetsRoutingTask(plan!!, speed, context!!)
        query.execute(waypoints)
    }

    fun LiveReplanRoute(
        plan: String?,
        speed: Int,
        context: Context?,
        waypoints: Waypoints?
    ) {
        val query = LiveRideReplanRoutingTask(plan!!, speed, context!!)
        query.execute(waypoints)
    }

    @JvmStatic
    fun FetchRoute(
        plan: String?,
        itinerary: Long,
        speed: Int,
        context: Context?
    ) {
        val query = FetchCycleStreetsRouteTask(plan!!, speed, context!!)
        query.execute(itinerary)
    }

    fun RePlotRoute(
        plan: String?,
        context: Context?
    ) {
        val query = ReplanRoutingTask(plan!!, db_!!, context!!)
        query.execute(plannedRoute_)
    }

    @JvmStatic
    fun PlotStoredRoute(
        localId: Int,
        context: Context?
    ) {
        val query = StoredRoutingTask(db_!!, context!!)
        query.execute(localId)
    }

    @JvmStatic
    fun RenameRoute(localId: Int, newName: String?) {
        db_!!.renameRoute(localId, newName)
    }

    @JvmStatic
    fun DeleteRoute(localId: Int) {
        db_!!.deleteRoute(localId)
    }

    /////////////////////////////////////////
    private var plannedRoute_ = NULL_JOURNEY
    private var waypoints_ = plannedRoute_.waypoints
    private var db_: RouteDatabase? = null
    private var context_: Context? = null
    @JvmStatic
    fun initialise(context: Context?) {
        context_ = context
        db_ = RouteDatabase(context)
        if (isLoaded) loadLastJourney()
    }

    fun setWaypoints(waypoints: Waypoints) {
        waypoints_ = waypoints
    }

    fun resetJourney() {
        onNewJourney(null)
    }

    fun onResume() {
        Segment.formatter = DistanceFormatter.formatter(CycleStreetsPreferences.units())
    }

    /////////////////////////////////////
    fun storedCount(): Int {
        return db_!!.routeCount()
    }

    @JvmStatic
    fun storedRoutes(): List<RouteSummary> {
        return db_!!.savedRoutes()
    }

    /////////////////////////////////////
    fun onNewJourney(route: RouteData?): Boolean {
        try {
            doOnNewJourney(route)
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Route finding failed", e)
            Toast.makeText(context_, R.string.route_finding_failed, Toast.LENGTH_LONG).show()
        }
        return false
    }

    private fun doOnNewJourney(route: RouteData?) {
        if (route == null) {
            plannedRoute_ = NULL_JOURNEY
            waypoints_ = NULL_WAYPOINTS
            listeners_.onReset()
            clearRoutePref()
            return
        }
        plannedRoute_ = loadFromJson(route.json(), route.points(), route.name())
        db_!!.saveRoute(plannedRoute_, route.json())
        waypoints_ = plannedRoute_.waypoints
        listeners_.onNewJourney(plannedRoute_, waypoints_)
        setRoutePref()
    }

    fun waypoints(): Waypoints {
        return waypoints_
    }

    @JvmStatic
    fun available(): Boolean {
        return plannedRoute_ != NULL_JOURNEY
    }

    @JvmStatic
    fun journey(): Journey {
        return plannedRoute_
    }

    private fun loadLastJourney() {
        val routeSummaries = storedRoutes()
        if (!storedRoutes().isEmpty()) {
            val lastRoute = routeSummaries[0]
            val route = db_!!.route(lastRoute.localId())
            onNewJourney(route)
        }
    }

    private fun clearRoutePref() {
        prefs().edit().remove(routePref).commit()
    }

    private fun setRoutePref() {
        prefs().edit().putBoolean(routePref, true).commit()
    }

    private val isLoaded: Boolean
        private get() = prefs().getBoolean(routePref, false)
    private const val routePref = "route"
    private fun prefs(): SharedPreferences {
        return context_!!.getSharedPreferences(
            "net.cyclestreets.CycleStreets",
            Context.MODE_PRIVATE
        )
    }

    interface Listener {
        fun onNewJourney(journey: Journey, waypoints: Waypoints)
        fun onResetJourney()
    }

    private class Listeners {
        private val listeners_: MutableList<Listener> = ArrayList()
        fun register(listener: Listener) {
            if (!doRegister(listener)) return
            if (journey() != NULL_JOURNEY || waypoints() != NULL_WAYPOINTS) listener.onNewJourney(
                journey(), waypoints()
            ) else listener.onResetJourney()
        }

        fun softRegister(listener: Listener) {
            doRegister(listener)
        }

        private fun doRegister(listener: Listener): Boolean {
            if (listeners_.contains(listener)) return false
            listeners_.add(listener)
            return true
        }

        fun unregister(listener: Listener?) {
            listeners_.remove(listener)
        }

        fun onNewJourney(journey: Journey, waypoints: Waypoints) {
            for (l in listeners_) l.onNewJourney(journey, waypoints)
        }

        fun onReset() {
            for (l in listeners_) l.onResetJourney()
        }
    }
}