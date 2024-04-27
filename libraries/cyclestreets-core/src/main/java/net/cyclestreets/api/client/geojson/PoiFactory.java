package net.cyclestreets.api.client.geojson;

import android.text.TextUtils;
import android.util.Log;

import net.cyclestreets.api.POI;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;
import org.geojson.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoiFactory {

  private PoiFactory() {}

  private static POI toPoi(Feature feature) {
    Log.d("toPOI method", "start");
    LngLatAlt coordinates = ((Point)feature.getGeometry()).getCoordinates();
    Log.d("toPOI method", "next");
    Map<String, String> osmTags = feature.getProperty("osmTags");
    Log.d("toPOI method", "osmTags");
    if (osmTags == null)
      osmTags = new HashMap<>();
    Log.d("toPOI method", "opening hours");
    String openingHours = osmTags.get("opening_hours");
    if (openingHours != null)
      openingHours = openingHours.replaceAll("; *", "\n");
    Log.d("toPOI method", "website");
    String website = feature.getProperty("website");
    if (TextUtils.isEmpty(website))
      website = osmTags.get("url");
    Log.d("toPOI method", "last");
    Log.d("POI feature id", feature.getProperty("id"));
    Log.d("POI parseInt", String.valueOf(Integer.parseInt("1025934598")));
    //Log.d("POI parseInt", String.valueOf(Integer.parseInt("10259345989")));
    final long longId = Long.parseLong("10259345989");
    Log.d("POI parseLong", String.valueOf(Long.parseLong("10259345989")));
    Log.d("POI parseLong", String.valueOf((int) Long.parseLong("10259345989")));
    Log.d("POI Long rt op", String.valueOf((longId>>>32)));
    Log.d("POI Long bit rt op", String.valueOf((longId^(longId>>>32))));
    Log.d("POI Long to int", String.valueOf((int)(longId^(longId>>>32))));
    /*Log.d("POI parseInt", String.valueOf(Integer.parseInt("10259345989")));
    //Log.d("POI parseInt", String.valueOf(Integer.parseInt(feature.getProperty("id"))));
    Log.d("toPOI method return value", String.valueOf(new POI(Integer.parseInt(feature.getProperty("id")),
    //Log.d("toPOI method return value", String.valueOf(new POI(feature.getProperty("id"),
            feature.getProperty("name"),
            feature.getProperty("notes"),
            website,
            osmTags.get("phone"),
            openingHours,
            coordinates.getLatitude(),
            coordinates.getLongitude()))); */
    return new POI(feature.getProperty("id"),
                   feature.getProperty("name"),
                   feature.getProperty("notes"),
                   website,
                   osmTags.get("phone"),
                   openingHours,
                   coordinates.getLatitude(),
                   coordinates.getLongitude());
  }

  public static List<POI> toPoiList(FeatureCollection featureCollection) {
    List<POI> pois = new ArrayList<>();
    Log.d("Response toPOIList", featureCollection.getFeatures().toString());  //.toString())
    for (Feature feature : featureCollection.getFeatures()) {
      Log.d("Response toPOI", toPoi(feature).toString());
      pois.add(toPoi(feature));
      Log.d("Response pois", pois.toString());
    }
    return pois;
  }
}
