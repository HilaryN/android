package net.cyclestreets

import net.cyclestreets.fragments.R
import net.cyclestreets.iconics.IconicsHelper
import net.cyclestreets.util.*
import net.cyclestreets.routing.Journey
import net.cyclestreets.routing.Route
import net.cyclestreets.routing.Waypoints

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

import net.cyclestreets.util.MenuHelper.enableMenuItem
import net.cyclestreets.util.MenuHelper.showMenuItem
import net.cyclestreets.views.overlay.*

private val TAG = Logging.getTag(RouteMapFragment::class.java)

class RouteMapFragment : CycleMapFragment(), Route.Listener, ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var routeSetter: TapToRouteOverlay
    private var hasGps: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View? {
        val v = super.onCreateView(inflater, container, saved)

        overlayPushBottom(RouteHighlightOverlay(requireContext(), mapView()))
        overlayPushBottom(POIOverlay(mapView()))
        overlayPushBottom(RouteOverlay())

        routeSetter = TapToRouteOverlay(mapView())
        overlayPushTop(routeSetter)

        hasGps = GPS.deviceHasGPS(requireContext())

        return v
    }

    override fun onPause() {
        Route.onPause(routeSetter.waypoints())
        Route.unregisterListener(this)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Route.registerListener(this)
        Route.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsHelper.inflate(inflater, R.menu.route_map, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        showMenuItem(menu, R.id.ic_menu_liveride, Route.routeAvailable() && hasGps)
        enableMenuItem(menu, R.id.ic_menu_directions, true)
        showMenuItem(menu, R.id.ic_menu_saved_routes, Route.storedCount() != 0)
        enableMenuItem(menu, R.id.ic_menu_route_number, true)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item))
            return true

        when (item.itemId) {
            R.id.ic_menu_liveride -> {
                startLiveRide()
                return true
            }
            R.id.ic_menu_directions -> {
                launchRouteDialog()
                return true
            }
            R.id.ic_menu_saved_routes -> {
                launchStoredRoutes()
                return true
            }
            R.id.ic_menu_route_number -> {
                launchFetchRouteDialog()
                return true
            }
            else -> return false
        }

    }

    private fun startLiveRide() {
        doOrRequestPermission2(this, Manifest.permission.ACCESS_FINE_LOCATION) {
            LiveRideActivity.launch(requireContext())
        }
    }

    private fun launchRouteDialog() {
        startNewRoute(DialogInterface.OnClickListener { _, _ -> doLaunchRouteDialog() })
    }

    private fun doLaunchRouteDialog() {
        RouteByAddress.launch(requireContext(),
                              mapView().boundingBox,
                              mapView().lastFix,
                              routeSetter.waypoints())
    }

    private fun launchFetchRouteDialog() {
        startNewRoute(DialogInterface.OnClickListener { _, _ -> doLaunchFetchRouteDialog() })
    }

    private fun doLaunchFetchRouteDialog() {
        RouteByNumber.launch(requireContext())
    }

    private fun launchStoredRoutes() {
        StoredRoutes.launch(requireContext())
    }

    private fun startNewRoute(listener: DialogInterface.OnClickListener) {
        if (Route.routeAvailable() && CycleStreetsPreferences.confirmNewRoute())
            MessageBox.YesNo(mapView(), R.string.confirm_new_route, listener)
        else
            listener.onClick(null, 0)
    }

    override fun onNewJourney(journey: Journey, waypoints: Waypoints) {
        if (!waypoints.isEmpty()) {
            Log.d(TAG, "Setting map centre to " + waypoints.first()!!)
            mapView().controller.setCenter(waypoints.first())
        }
        mapView().postInvalidate()
    }

    override fun onResetJourney() {
        mapView().invalidate()
    }

    fun doOrRequestPermission2(fragment: Fragment, permission: String, action: () -> Unit) {
        val context = fragment.requireContext()
        if (hasPermission(context, permission)) {
            CycleStreetsPreferences.clearSettingsLastTime(permission)
            // all good, carry on
            action()
        }
        else {
            val prev = CycleStreetsPreferences.permissionPreviouslyRequested(permission)
            val rati = fragment.shouldShowRequestPermissionRationale(permission)
            if (!prev || rati) {
                // Give details of why we're asking for permission, be it when we ask for the first time
                // or after a user clicked "deny" the first time
                MessageBox.OkHtml(context, justification(context, permission)) { _, _ ->
                    requestPermission(fragment, permission, 2)
                }
            } else {
                if (CycleStreetsPreferences.settingsLastTime(permission)) {
                    // If the user was taken to device settings last time, then Android 11 / API 30 gives them 3 options:
                    // Allow for this app / Ask every time / Deny
                    // To get to this point they would have taken "Ask every time" or "Deny" but unfortunately we don't know which
                    // as both appear as Permission not granted.
                    // However the Android framework will know which option was taken in the settings
                    // and if we request permission here it will take the appropriate action.
                    // If the "Deny" option was taken in the device settings then the permission box won't be displayed.
                    // (In that case it would be nice to display a toast message saying "X permission not granted.  Please go to device settings to allow this."
                    // but that can be a future enhancement :-) )
                    // If the "Ask every time" option was taken then the permission box will be displayed.
                    // No justification box is shown here as they have already had them both and it wouldn't be appropriate if the permissions
                    // box then wasn't displayed.
                    requestPermission(fragment, permission, 2)
                }
                else {
                    // User has previously denied, and said "don't ask me again".  Tell them they'll have to go into app settings now.
                    CycleStreetsPreferences.logSettingsLastTime(permission)
                    MessageBox.OkHtml(context, justificationAfterDenial(context, permission)) { _, _ ->
                        goToSettings(context)
                    }
                }
            }
        }
    }
    // Callback after user has selected a permission (todo in this case the one requested from DoOrRequest2)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 2) {
            // Request for location permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted.
                CycleStreetsPreferences.clearSettingsLastTime(permissions[0])
                CycleStreetsPreferences.clearPermissionRequested(permissions[0])    // todo rename as clearPermsDenied
                startLiveRide()
            } else {
                // Permission request was denied.
                CycleStreetsPreferences.logPermissionAsRequested(permissions[0])    // todo rename as log...Denied
            }
        }
    }

}
