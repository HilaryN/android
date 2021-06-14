package net.cyclestreets.routing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

// todo remove: import net.cyclestreets.api.POI;

import org.osmdroid.api.IGeoPoint;

import java.util.ArrayList;
import java.util.List;

public class JourneyDomainObject {
  @JsonProperty
  public final List<IGeoPoint> waypoints = new ArrayList<>();
  @JsonProperty
  public final RouteDomainObject route = new RouteDomainObject();
  @JsonProperty
  public final List<SegmentDomainObject> segments = new ArrayList<>();
  @JsonProperty
  public final List<PoiDomainObject> pois = new ArrayList<>(); //todo remove domain obj?
  // todo remove if not needed: public final List<POI> pois = new ArrayList<>();
  @Override
  public String toString() {
    return "JourneyDomainObject{" + "waypoints=" + waypoints +
        ", route=" + route +
        ", segments=" + segments +
        '}';
  }
}
