package net.cyclestreets.content;

import net.cyclestreets.routing.Waypoints;

public class RouteData
{
  private final String name;
  private final String json;
  private final Waypoints points;
  private final String poiTypes;

  // todo may not need this constructor - replaced with one which has poiTypes?
  public RouteData(final String json,
                   final Waypoints points,
                   final String name) {
    this.json = json;
    this.points = points;
    this.name = name;
    this.poiTypes = null;
  }

  public RouteData(final String json,
                   final Waypoints points,
                   final String name,
                   final String poiTypes) {
    this.json = json;
    this.points = points;
    this.name = name;
    this.poiTypes = poiTypes;
  }

  public String name() { return name; }
  public String json() { return json; }
  public Waypoints points() { return points; }
  public String poiTypes() { return poiTypes; }
}
