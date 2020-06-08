package net.cyclestreets.routing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import net.cyclestreets.TestUtils
import net.cyclestreets.content.RouteData
import net.cyclestreets.routing.Route.journey
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE, sdk = [28])
@RunWith(RobolectricTestRunner::class)
class JourneyTest {
// Journey #62,909,947
    private lateinit var journey: Journey
    private val roboContext = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        Route.initialise(roboContext)
        loadJourneyFrom("journey-domain.json")
        journey.setActiveSegmentIndex(3)
    }

    @Test
    fun remainingDistance() {
        // Get distance to end of active segment

        //activeSeg!!.distanceFromEnd(52.2055f, 0.1183f)
        assert(journey.remainingDistance(27) == 6202)
    }

    @Test
    fun remainingTime() {
        assert((journey.remainingTime(27) == 1539))
    }

    @Test
    fun previousSegment() {
    }

    private fun loadJourneyFrom(domainJsonFile: String) {
        val rawJson = TestUtils.fromResourceFile(domainJsonFile)
        val routeData = RouteData(rawJson, null, "test route")
        Route.onNewJourney(routeData)
        journey = journey()
    }
}