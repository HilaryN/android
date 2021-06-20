package net.cyclestreets.api.client;

import android.content.Context;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.cyclestreets.api.Blog;
import net.cyclestreets.api.GeoPlaces;
import net.cyclestreets.api.POI;
import net.cyclestreets.api.POICategories;
import net.cyclestreets.api.PhotomapCategories;
import net.cyclestreets.api.Photos;
import net.cyclestreets.api.Result;
import net.cyclestreets.api.Signin;
import net.cyclestreets.api.Upload;
import net.cyclestreets.api.UserJourneys;
import net.cyclestreets.api.client.dto.BlogFeedDto;
import net.cyclestreets.api.client.dto.PhotomapCategoriesDto;
import net.cyclestreets.api.client.dto.PoiTypesDto;
import net.cyclestreets.api.client.dto.SendFeedbackResponseDto;
import net.cyclestreets.api.client.dto.UploadPhotoResponseDto;
import net.cyclestreets.api.client.dto.UserAuthenticateResponseDto;
import net.cyclestreets.api.client.dto.UserCreateResponseDto;
import net.cyclestreets.api.client.dto.UserJourneysDto;
import net.cyclestreets.api.client.geojson.GeoPlacesFactory;
import net.cyclestreets.api.client.geojson.PhotosFactory;
import net.cyclestreets.api.client.geojson.PoiFactory;

import org.geojson.FeatureCollection;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.Cache;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static net.cyclestreets.RoutePlans.PLAN_LEISURE;

public class RetrofitApiClient {

  private final V1Api v1Api;
  private final V2Api v2Api;
  private final BlogApi blogApi;
  private final Context context;

  private static final String REPORT_ERRORS = "1";

  // ~30KB covers /news/feed/, /v2/pois.types and /v2/photomap.categories - allow 200KB for headroom
  private static final int CACHE_MAX_SIZE_BYTES = 200 * 1024;
  private static final String CACHE_DIR_NAME = "RetrofitApiClientCache";

  // As per https://stackoverflow.com/a/49835538, SimpleXML is deprecated in favour of JAXB, but
  // the latter doesn't work on Android.
  @SuppressWarnings("deprecation")
  public RetrofitApiClient(Builder builder) {

    context = builder.context;
    Cache cache = new Cache(new File(context.getCacheDir(), CACHE_DIR_NAME), CACHE_MAX_SIZE_BYTES);
    OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(new ApiKeyInterceptor(builder.apiKey))
            .addInterceptor(new HttpLoggingInterceptor())
            .addNetworkInterceptor(new RewriteCacheControlInterceptor())
            .cache(cache)
            .build();

    // Configure our ObjectMapper to globally ignore unknown properties
    // Required for e.g. getPhotos API which returns `properties` on a `FeatureCollection`, which is
    // not part of standard GeoJSON
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Retrofit retrofitV1 = new Retrofit.Builder()
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(retrofit2.converter.simplexml.SimpleXmlConverterFactory.createNonStrict())
        .baseUrl(builder.v1Host)
        .build();
    v1Api = retrofitV1.create(V1Api.class);

    Retrofit retrofitV2 = new Retrofit.Builder()
        .client(client)
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .baseUrl(builder.v2Host)
        .build();
    v2Api = retrofitV2.create(V2Api.class);

    Retrofit retrofitBlog = new Retrofit.Builder()
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(retrofit2.converter.simplexml.SimpleXmlConverterFactory.createNonStrict())
        .baseUrl(builder.blogHost)
        .build();
    blogApi = retrofitBlog.create(BlogApi.class);
  }

  public static class Builder {
    private Context context;
    private String apiKey;
    private String v1Host;
    private String v2Host;
    private String blogHost;

    public Builder withContext(Context context) {
      this.context = context;
      return this;
    }

    public Builder withApiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }
    public Builder withV1Host(String v1Host) {
      this.v1Host = v1Host;
      return this;
    }

    public Builder withV2Host(String v2Host) {
      this.v2Host = v2Host;
      return this;
    }

    public Builder withBlogHost(String blogHost) {
      this.blogHost = blogHost;
      return this;
    }

    public RetrofitApiClient build() {
      return new RetrofitApiClient(this);
    }
  }

  private static String toBboxString(double lonW, double latS, double lonE, double latN) {
    return lonW + "," + latS + "," + lonE + "," + latN;
  }

  // --------------------------------------------------------------------------------
  // V1 APIs
  // --------------------------------------------------------------------------------
  public String getJourneyJson(final String plan,
                               final String itineraryPoints,
                               final String leaving,
                               final String arriving,
                               final int speed) throws IOException {
    Response<String> response = v1Api.getJourneyJson(plan, itineraryPoints, leaving, arriving, speed, REPORT_ERRORS).execute();
    return JourneyStringTransformerKt.fromV1ApiJson(response.body());
  }

  public String getCircularJourneyJson(final String itineraryPoints,
                                       final Integer distance,
                                       final Integer duration,
                                       final String poiTypes) throws IOException {
    Response<String> response = v1Api.getCircularJourneyJson(PLAN_LEISURE, itineraryPoints, distance, duration, poiTypes, REPORT_ERRORS).execute();
    // todo remove following hard-coded line
    //return JourneyStringTransformerKt.fromV1ApiJson("{\"marker\":[{\"@attributes\":{\"start\":\"Short un-named link\",\"finish\":\"Short un-named link\",\"startBearing\":\"0\",\"startSpeed\":\"0\",\"start_longitude\":\"0.17117\",\"start_latitude\":\"51.30253\",\"finish_longitude\":\"0.17117\",\"finish_latitude\":\"51.30253\",\"crow_fly_distance\":\"9608\",\"event\":\"depart\",\"whence\":\"1624211725\",\"speed\":\"16\",\"itinerary\":\"74980646\",\"clientRouteId\":\"0\",\"plan\":\"leisure1\",\"note\":\"\",\"length\":\"12890\",\"time\":\"5027\",\"busynance\":\"49887\",\"quietness\":\"26\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"west\":\"0.16312\",\"south\":\"51.30253\",\"east\":\"0.21724\",\"north\":\"51.32834\",\"name\":\"Short un-named link to Short un-named link\",\"walk\":\"1\",\"leaving\":\"2021-06-20 18:55:25\",\"arriving\":\"2021-06-20 20:19:12\",\"coordinates\":\"0.17117,51.30253 0.17114,51.30265 0.17073,51.30381 0.1706,51.30424 0.17004,51.30539 0.17012,51.30541 0.17059,51.30554 0.17115,51.30573 0.17123,51.30576 0.17188,51.30604 0.17231,51.30623 0.17246,51.30631 0.17259,51.30644 0.1728,51.30669 0.17304,51.30703 0.17315,51.30709 0.17326,51.30711 0.17347,51.30709 0.17355,51.30709 0.1736,51.30709 0.17365,51.30712 0.17408,51.3077 0.17432,51.30792 0.17461,51.3083 0.17479,51.30857 0.17501,51.30887 0.17533,51.30912 0.17554,51.30929 0.17566,51.30939 0.1757,51.30942 0.17575,51.3095 0.17612,51.31009 0.17614,51.31013 0.17635,51.31055 0.17652,51.31086 0.17661,51.31103 0.17681,51.31158 0.17615,51.31164 0.17567,51.3118 0.1746,51.31208 0.17361,51.31219 0.17353,51.31219 0.17261,51.31225 0.17232,51.31227 0.17164,51.3123 0.17138,51.3123 0.17068,51.31227 0.17065,51.31293 0.17061,51.31301 0.17051,51.31308 0.17032,51.31315 0.17003,51.31323 0.16976,51.31323 0.16925,51.31315 0.16892,51.31317 0.16807,51.31338 0.16774,51.31349 0.16719,51.31379 0.167,51.31389 0.16601,51.31445 0.16571,51.31457 0.16502,51.31483 0.164,51.31517 0.16312,51.31548 0.16365,51.31619 0.16422,51.31684 0.16436,51.3172 0.1643,51.31767 0.16398,51.31818 0.16401,51.3184 0.16423,51.31902 0.16427,51.31914 0.1643,51.31923 0.16452,51.31983 0.16472,51.32043 0.16498,51.32072 0.165,51.32073 0.16534,51.32097 0.16561,51.32124 0.16586,51.32156 0.16605,51.32212 0.16623,51.32244 0.16644,51.32279 0.16680,51.32312 0.16743,51.32346 0.16777,51.32365 0.16825,51.32391 0.16862,51.32414 0.16897,51.32453 0.16927,51.32503 0.16946,51.32535 0.16979,51.32579 0.17043,51.3263 0.17057,51.32635 0.17064,51.32639 0.17075,51.32641 0.17078,51.32642 0.17119,51.32664 0.17163,51.32696 0.17181,51.32713 0.17186,51.32718 0.17195,51.32726 0.17209,51.32739 0.17226,51.3276 0.17282,51.32804 0.17327,51.32834 0.17474,51.32779 0.17519,51.32766 0.17558,51.32755 0.17573,51.32751 0.17596,51.3275 0.17626,51.3276 0.17658,51.32768 0.17722,51.32781 0.17787,51.32802 0.17837,51.32815 0.17853,51.3282 0.17856,51.32821 0.17867,51.32823 0.17872,51.32823 0.17885,51.32816 0.17911,51.32814 0.17934,51.32812 0.17961,51.32809 0.17982,51.32807 0.18013,51.328 0.1805,51.32791 0.18075,51.32786 0.18105,51.3278 0.1812,51.32774 0.18132,51.3277 0.18156,51.32759 0.18185,51.32747 0.18221,51.32731 0.18256,51.32716 0.18354,51.32694 0.18561,51.32657 0.18673,51.32601 0.18762,51.32559 0.18797,51.32543 0.18834,51.32529 0.1887,51.32514 0.18919,51.325 0.18956,51.32495 0.18971,51.32498 0.19,51.32464 0.1902,51.32437 0.19028,51.32421 0.19034,51.32404 0.19037,51.32376 0.19023,51.32296 0.1903,51.32257 0.19031,51.32245 0.19038,51.3221 0.19052,51.32164 0.19058,51.3214 0.1907,51.32118 0.19093,51.32087 0.19118,51.32064 0.19165,51.32034 0.19207,51.32005 0.19254,51.31955 0.19268,51.31936 0.19288,51.31907 0.19314,51.3187 0.19325,51.31854 0.19329,51.3185 0.19374,51.31797 0.19392,51.31771 0.19407,51.31746 0.19426,51.31724 0.19462,51.31689 0.1954,51.31616 0.19573,51.31584 0.19634,51.31524 0.19658,51.31505 0.19664,51.31502 0.19687,51.31491 0.19721,51.31471 0.19742,51.31457 0.19751,51.3145 0.19766,51.31439 0.19773,51.31429 0.19776,51.31422 0.19777,51.31417 0.19776,51.31411 0.19773,51.31405 0.19755,51.31382 0.19735,51.31365 0.19717,51.31353 0.19709,51.31349 0.19696,51.31343 0.19737,51.31287 0.19756,51.3126 0.1977,51.3126 0.19784,51.31253 0.19809,51.31247 0.19816,51.31244 0.19825,51.31241 0.19828,51.31235 0.1986,51.31183 0.19884,51.31155 0.19917,51.31153 0.19924,51.31153 0.19922,51.31122 0.19975,51.31125 0.20019,51.31118 0.20031,51.31114 0.2007,51.31108 0.20107,51.31097 0.20158,51.31075 0.20239,51.31035 0.20265,51.31021 0.20635,51.30848 0.2066,51.30843 0.20691,51.30838 0.20772,51.30822 0.20812,51.30815 0.20839,51.30806 0.20862,51.30802 0.21052,51.30768 0.21138,51.3076 0.21236,51.3075 0.21249,51.30748 0.21318,51.3074 0.21346,51.3073 0.21427,51.30693 0.2161,51.30649 0.21707,51.30641 0.21724,51.30639 0.21707,51.30641 0.21610,51.30649 0.21427,51.30693 0.21346,51.3073 0.21318,51.3074 0.21249,51.30748 0.21236,51.3075 0.21138,51.3076 0.21052,51.30768 0.20862,51.30802 0.20839,51.30806 0.20812,51.30815 0.20772,51.30822 0.20691,51.30838 0.2066,51.30843 0.20635,51.30848 0.20265,51.31021 0.20239,51.31035 0.20158,51.31075 0.20107,51.31097 0.2007,51.31108 0.20031,51.31114 0.20019,51.31118 0.19975,51.31125 0.19922,51.31122 0.19924,51.31153 0.19917,51.31153 0.19884,51.31155 0.1986,51.31183 0.19828,51.31235 0.19825,51.31241 0.19816,51.31244 0.19809,51.31247 0.19784,51.31253 0.1977,51.3126 0.19756,51.3126 0.19737,51.31287 0.19696,51.31343 0.19668,51.31334 0.19636,51.31327 0.19608,51.31325 0.19591,51.31323 0.19571,51.31322 0.19564,51.31321 0.19542,51.31321 0.19509,51.31318 0.19482,51.31315 0.19473,51.31314 0.19458,51.31312 0.19401,51.31303 0.19365,51.31297 0.19328,51.31293 0.19298,51.31291 0.19281,51.31292 0.19239,51.31293 0.19213,51.31293 0.19193,51.31294 0.19174,51.31294 0.19157,51.31293 0.19133,51.3129 0.19123,51.31289 0.19114,51.31288 0.191,51.31279 0.19085,51.31261 0.1908,51.31258 0.19076,51.31256 0.19072,51.31255 0.19068,51.31255 0.19056,51.31255 0.19049,51.31256 0.19044,51.31258 0.19041,51.3126 0.19035,51.31266 0.19031,51.31271 0.1903,51.31274 0.19024,51.31277 0.19021,51.3128 0.19016,51.31283 0.19007,51.31286 0.19,51.31288 0.18994,51.3129 0.18937,51.31298 0.1886,51.31298 0.18844,51.31298 0.18838,51.31298 0.18831,51.31298 0.1882,51.31298 0.18752,51.31296 0.18646,51.31292 0.18631,51.31291 0.18613,51.3129 0.18569,51.31288 0.18561,51.31287 0.18492,51.31274 0.18466,51.31266 0.18402,51.31254 0.18378,51.31249 0.18308,51.31233 0.18292,51.31228 0.1828,51.31224 0.18213,51.312 0.18158,51.31184 0.18149,51.31181 0.18146,51.31171 0.18139,51.3116 0.1813,51.3115 0.18114,51.31136 0.18079,51.3111 0.18074,51.31106 0.18074,51.31095 0.18062,51.31095 0.18056,51.31095 0.18048,51.31095 0.18045,51.31093 0.18026,51.31083 0.17994,51.31066 0.17979,51.31058 0.17964,51.31053 0.17948,51.31049 0.17939,51.31049 0.17931,51.31049 0.17916,51.31048 0.17895,51.31046 0.17879,51.31043 0.1786,51.31038 0.17842,51.31032 0.1782,51.3102 0.17799,51.31005 0.17787,51.30992 0.17779,51.30977 0.17778,51.30972 0.17775,51.30961 0.17775,51.30954 0.17774,51.30947 0.17772,51.30942 0.17766,51.30933 0.17755,51.30915 0.17708,51.3087 0.1766,51.30856 0.17607,51.3087 0.17533,51.30912 0.17501,51.30887 0.17479,51.30857 0.17461,51.3083 0.17432,51.30792 0.17408,51.3077 0.17365,51.30712 0.1736,51.30709 0.17355,51.30709 0.17347,51.30709 0.17326,51.30711 0.17315,51.30709 0.17304,51.30703 0.1728,51.30669 0.17259,51.30644 0.17246,51.30631 0.17231,51.30623 0.17188,51.30604 0.17123,51.30576 0.17115,51.30573 0.17059,51.30554 0.17012,51.30541 0.17004,51.30539 0.1706,51.30424 0.17073,51.30381 0.17114,51.30265 0.17117,51.30253\",\"elevations\":\"94,95,97,96,97,96,93,91,90,87,84,83,82,80,77,76,76,75,75,75,75,76,76,75,74,73,72,72,72,72,72,72,72,72,71,71,69,70,71,71,72,72,73,73,73,72,72,69,69,69,69,69,69,71,72,73,74,77,78,82,83,85,90,94,90,87,86,86,87,85,81,81,80,81,83,87,87,88,88,88,88,88,89,92,88,85,80,76,74,74,73,72,69,69,68,68,68,68,67,68,68,68,68,67,66,66,58,56,55,55,55,55,55,55,55,54,54,54,54,54,54,54,54,54,54,54,55,55,56,58,59,61,64,69,73,75,79,83,86,87,90,88,93,95,97,96,96,97,98,100,105,107,107,106,108,108,109,108,103,102,97,93,93,92,91,91,91,88,86,85,85,84,83,84,86,88,88,89,89,89,89,89,89,89,89,89,89,87,86,86,86,86,83,83,83,82,82,82,82,82,81,81,81,82,81,83,83,83,85,86,87,88,88,90,92,94,95,95,94,94,91,90,90,90,90,90,91,92,94,94,94,92,91,90,90,90,90,90,91,94,94,95,95,94,92,90,88,88,87,86,85,83,83,83,81,82,81,81,81,82,82,82,82,82,83,83,83,86,83,81,80,79,78,78,77,77,77,77,76,73,73,72,72,72,72,71,71,70,70,69,69,69,68,68,68,68,68,67,67,67,67,67,67,67,67,67,67,67,67,67,67,67,66,66,66,66,65,65,63,63,62,62,62,61,61,59,59,59,59,59,59,60,60,60,60,61,61,62,62,62,62,62,62,62,63,64,64,65,65,65,65,65,66,66,67,67,67,67,67,67,67,67,67,67,67,67,68,69,70,69,72,73,74,75,76,76,75,75,75,75,76,76,77,80,82,83,84,87,90,91,93,96,97,96,97,95,94\",\"distances\":\"14,132,49,134,6,36,44,6,55,37,14,17,31,41,10,8,15,6,3,5,71,30,47,33,37,36,24,14,4,10,70,5,49,36,20,63,46,38,81,70,6,64,20,47,18,49,73,9,10,15,22,19,37,23,64,26,51,17,93,25,56,80,70,87,82,41,52,61,25,71,14,10,68,68,37,2,36,35,40,64,38,42,44,58,32,44,36,50,59,38,54,72,11,7,8,2,38,47,23,7,11,17,26,63,46,119,34,30,11,16,24,24,47,51,38,12,2,8,3,12,18,16,19,15,23,28,18,22,12,9,21,24,31,29,72,150,100,77,30,30,30,37,26,11,43,33,19,19,31,89,44,13,39,52,27,26,38,31,47,43,64,23,35,45,19,5,67,32,30,28,46,98,42,79,27,5,20,32,21,10,16,12,8,6,7,7,28,23,18,7,11,68,33,10,12,19,6,7,7,62,35,23,5,34,37,32,9,28,28,43,72,24,321,18,22,59,29,21,17,137,60,69,9,49,22,70,136,68,12,12,68,136,70,22,49,9,69,60,137,17,21,29,59,22,18,321,24,72,43,28,28,9,32,37,34,5,23,35,62,7,7,6,19,12,10,33,68,22,24,20,12,14,5,15,23,19,6,11,41,26,26,21,12,29,18,14,13,12,17,7,6,14,23,5,4,3,3,8,5,4,3,8,6,3,5,4,5,7,5,5,41,54,11,4,5,8,47,74,10,13,31,6,50,20,46,18,52,12,9,54,42,7,11,13,13,19,38,6,12,8,4,6,3,17,29,14,12,12,6,6,10,15,12,14,14,20,22,17,18,6,12,8,8,6,11,21,60,37,40,69,36,37,33,47,30,71,5,3,6,15,8,10,41,31,17,14,37,55,6,44,36,6,134,49,132,14\",\"grammesCO2saved\":\"2403\",\"calories\":\"236\",\"otherRoutes\":\"leisure1,leisure2,leisure3,leisure4,leisure5,leisure6,leisure7,leisure8\",\"edition\":\"routing210619\",\"type\":\"route\"}},{\"@attributes\":{\"name\":\"Link with SR100\",\"legNumber\":\"1\",\"distance\":\"146\",\"time\":\"160\",\"busynance\":\"1041\",\"quietness\":\"20\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"\",\"startBearing\":\"351\",\"color\":\"#008800\",\"points\":\"0.17117,51.30253 0.17114,51.30265 0.17073,51.30381\",\"distances\":\"0,14,132\",\"elevations\":\"94,95,97\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"SR100\",\"legNumber\":\"1\",\"distance\":\"183\",\"time\":\"186\",\"busynance\":\"1004\",\"quietness\":\"20\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"349\",\"color\":\"#008800\",\"points\":\"0.17073,51.30381 0.1706,51.30424 0.17004,51.30539\",\"distances\":\"0,49,134\",\"elevations\":\"97,96,97\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Link with SR100\",\"legNumber\":\"1\",\"distance\":\"6\",\"time\":\"21\",\"busynance\":\"15\",\"quietness\":\"20\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn right\",\"startBearing\":\"68\",\"color\":\"#008800\",\"points\":\"0.17004,51.30539 0.17012,51.30541\",\"distances\":\"0,6\",\"elevations\":\"97,96\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Bridge\",\"legNumber\":\"1\",\"distance\":\"36\",\"time\":\"30\",\"busynance\":\"90\",\"quietness\":\"20\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"66\",\"color\":\"#008800\",\"points\":\"0.17012,51.30541 0.17059,51.30554\",\"distances\":\"0,36\",\"elevations\":\"96,93\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Telston Lane\",\"legNumber\":\"1\",\"distance\":\"287\",\"time\":\"33\",\"busynance\":\"479\",\"quietness\":\"30\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"62\",\"color\":\"#33aa33\",\"points\":\"0.17059,51.30554 0.17115,51.30573 0.17123,51.30576 0.17188,51.30604 0.17231,51.30623 0.17246,51.30631 0.17259,51.30644 0.1728,51.30669 0.17304,51.30703 0.17315,51.30709 0.17326,51.30711 0.17347,51.30709 0.17355,51.30709 0.1736,51.30709\",\"distances\":\"0,44,6,55,37,14,17,31,41,10,8,15,6,3\",\"elevations\":\"93,91,90,87,84,83,82,80,77,76,76,75,75,75\",\"provisionName\":\"Unclassified road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Telston Lane\",\"legNumber\":\"1\",\"distance\":\"223\",\"time\":\"55\",\"busynance\":\"804\",\"quietness\":\"30\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"bear left\",\"startBearing\":\"46\",\"color\":\"#33aa33\",\"points\":\"0.1736,51.30709 0.17365,51.30712 0.17408,51.3077 0.17432,51.30792 0.17461,51.3083 0.17479,51.30857 0.17501,51.30887\",\"distances\":\"0,5,71,30,47,33,37\",\"elevations\":\"75,75,76,76,75,74,73\",\"provisionName\":\"Unclassified road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Telston Lane\",\"legNumber\":\"1\",\"distance\":\"331\",\"time\":\"70\",\"busynance\":\"740\",\"quietness\":\"40\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"39\",\"color\":\"#000000\",\"points\":\"0.17501,51.30887 0.17533,51.30912 0.17554,51.30929 0.17566,51.30939 0.1757,51.30942 0.17575,51.3095 0.17612,51.31009 0.17614,51.31013 0.17635,51.31055 0.17652,51.31086 0.17661,51.31103 0.17681,51.31158\",\"distances\":\"0,36,24,14,4,10,70,5,49,36,20,63\",\"elevations\":\"73,72,72,72,72,72,72,72,72,71,71,69\",\"provisionName\":\"Residential street\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Pilgrims Way West\",\"legNumber\":\"1\",\"distance\":\"439\",\"time\":\"123\",\"busynance\":\"1792\",\"quietness\":\"30\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn left\",\"startBearing\":\"278\",\"color\":\"#33aa33\",\"points\":\"0.17681,51.31158 0.17615,51.31164 0.17567,51.3118 0.1746,51.31208 0.17361,51.31219 0.17353,51.31219 0.17261,51.31225 0.17232,51.31227 0.17164,51.3123 0.17138,51.3123 0.17068,51.31227\",\"distances\":\"0,46,38,81,70,6,64,20,47,18,49\",\"elevations\":\"69,70,71,71,72,72,73,73,73,72,72\",\"provisionName\":\"Minor road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Twitton Lane\",\"legNumber\":\"1\",\"distance\":\"690\",\"time\":\"304\",\"busynance\":\"4463\",\"quietness\":\"30\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn right\",\"startBearing\":\"358\",\"color\":\"#33aa33\",\"points\":\"0.17068,51.31227 0.17065,51.31293 0.17061,51.31301 0.17051,51.31308 0.17032,51.31315 0.17003,51.31323 0.16976,51.31323 0.16925,51.31315 0.16892,51.31317 0.16807,51.31338 0.16774,51.31349 0.16719,51.31379 0.167,51.31389 0.16601,51.31445 0.16571,51.31457 0.16502,51.31483 0.164,51.31517 0.16312,51.31548\",\"distances\":\"0,73,9,10,15,22,19,37,23,64,26,51,17,93,25,56,80,70\",\"elevations\":\"72,69,69,69,69,69,69,71,72,73,74,77,78,82,83,85,90,94\",\"provisionName\":\"Minor road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Filston Lane\",\"legNumber\":\"1\",\"distance\":\"1007\",\"time\":\"264\",\"busynance\":\"3892\",\"quietness\":\"30\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn right\",\"startBearing\":\"25\",\"color\":\"#33aa33\",\"points\":\"0.16312,51.31548 0.16365,51.31619 0.16422,51.31684 0.16436,51.3172 0.1643,51.31767 0.16398,51.31818 0.16401,51.3184 0.16423,51.31902 0.16427,51.31914 0.1643,51.31923 0.16452,51.31983 0.16472,51.32043 0.16498,51.32072 0.165,51.32073 0.16534,51.32097 0.16561,51.32124 0.16586,51.32156 0.16605,51.32212 0.16623,51.32244 0.16644,51.32279 0.16680,51.32312 0.16743,51.32346 0.16777,51.32365\",\"distances\":\"0,87,82,41,52,61,25,71,14,10,68,68,37,2,36,35,40,64,38,42,44,58,32\",\"elevations\":\"94,90,87,86,86,87,85,81,81,80,81,83,87,87,88,88,88,88,88,89,92,88,85\",\"provisionName\":\"Minor road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Filston Lane\",\"legNumber\":\"2\",\"distance\":\"659\",\"time\":\"122\",\"busynance\":\"1772\",\"quietness\":\"30\",\"flow\":\"0\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"49\",\"color\":\"#33aa33\",\"points\":\"0.16777,51.32365 0.16825,51.32391 0.16862,51.32414 0.16897,51.32453 0.16927,51.32503 0.16946,51.32535 0.16979,51.32579 0.17043,51.3263 0.17057,51.32635 0.17064,51.32639 0.17075,51.32641 0.17078,51.32642 0.17119,51.32664 0.17163,51.32696 0.17181,51.32713 0.17186,51.32718 0.17195,51.32726 0.17209,51.32739 0.17226,51.3276 0.17282,51.32804 0.17327,51.32834\",\"distances\":\"0,44,36,50,59,38,54,72,11,7,8,2,38,47,23,7,11,17,26,63,46\",\"elevations\":\"85,80,76,74,74,73,72,69,69,68,68,68,68,67,68,68,68,68,67,66,66\",\"provisionName\":\"Minor road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Water Lane\",\"legNumber\":\"2\",\"distance\":\"183\",\"time\":\"23\",\"busynance\":\"248\",\"quietness\":\"40\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn right\",\"startBearing\":\"121\",\"color\":\"#000000\",\"points\":\"0.17327,51.32834 0.17474,51.32779 0.17519,51.32766 0.17558,51.32755\",\"distances\":\"0,119,34,30\",\"elevations\":\"66,58,56,55\",\"provisionName\":\"Residential street\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Link with Water Lane\",\"legNumber\":\"2\",\"distance\":\"233\",\"time\":\"139\",\"busynance\":\"681\",\"quietness\":\"33\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"113\",\"color\":\"#888833\",\"points\":\"0.17558,51.32755 0.17573,51.32751 0.17596,51.3275 0.17626,51.3276 0.17658,51.32768 0.17722,51.32781 0.17787,51.32802 0.17837,51.32815 0.17853,51.3282 0.17856,51.32821 0.17867,51.32823\",\"distances\":\"0,11,16,24,24,47,51,38,12,2,8\",\"elevations\":\"55,55,55,55,55,55,55,54,54,54,54\",\"provisionName\":\"Bridleway\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Bridge\",\"legNumber\":\"2\",\"distance\":\"3\",\"time\":\"1\",\"busynance\":\"5\",\"quietness\":\"60\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"90\",\"color\":\"#008800\",\"points\":\"0.17867,51.32823 0.17872,51.32823\",\"distances\":\"0,3\",\"elevations\":\"54,54\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Short un-named link\",\"legNumber\":\"2\",\"distance\":\"80\",\"time\":\"81\",\"busynance\":\"169\",\"quietness\":\"45\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"bear right\",\"startBearing\":\"131\",\"color\":\"#888833\",\"points\":\"0.17872,51.32823 0.17885,51.32816 0.17911,51.32814 0.17934,51.32812 0.17961,51.32809 0.17982,51.32807\",\"distances\":\"0,12,18,16,19,15\",\"elevations\":\"54,54,54,54,54,54\",\"provisionName\":\"Track\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Short un-named link\",\"legNumber\":\"2\",\"distance\":\"69\",\"time\":\"26\",\"busynance\":\"176\",\"quietness\":\"60\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"110\",\"color\":\"#7777cc\",\"points\":\"0.17982,51.32807 0.18013,51.328 0.1805,51.32791 0.18075,51.32786\",\"distances\":\"0,23,28,18\",\"elevations\":\"54,54,55,55\",\"provisionName\":\"Service Road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Bridge\",\"legNumber\":\"2\",\"distance\":\"22\",\"time\":\"13\",\"busynance\":\"97\",\"quietness\":\"60\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"108\",\"color\":\"#7777cc\",\"points\":\"0.18075,51.32786 0.18105,51.3278\",\"distances\":\"0,22\",\"elevations\":\"55,56\",\"provisionName\":\"Service Road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Link with Home Farm\",\"legNumber\":\"2\",\"distance\":\"198\",\"time\":\"155\",\"busynance\":\"1145\",\"quietness\":\"60\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"123\",\"color\":\"#7777cc\",\"points\":\"0.18105,51.3278 0.1812,51.32774 0.18132,51.3277 0.18156,51.32759 0.18185,51.32747 0.18221,51.32731 0.18256,51.32716 0.18354,51.32694\",\"distances\":\"0,12,9,21,24,31,29,72\",\"elevations\":\"56,58,59,61,64,69,73,75\",\"provisionName\":\"Service Road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Un-named link\",\"legNumber\":\"2\",\"distance\":\"357\",\"time\":\"154\",\"busynance\":\"1129\",\"quietness\":\"60\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"106\",\"color\":\"#7777cc\",\"points\":\"0.18354,51.32694 0.18561,51.32657 0.18673,51.32601 0.18762,51.32559 0.18797,51.32543\",\"distances\":\"0,150,100,77,30\",\"elevations\":\"75,79,83,86,87\",\"provisionName\":\"Service Road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Bridge\",\"legNumber\":\"2\",\"distance\":\"60\",\"time\":\"31\",\"busynance\":\"226\",\"quietness\":\"60\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"121\",\"color\":\"#7777cc\",\"points\":\"0.18797,51.32543 0.18834,51.32529 0.1887,51.32514\",\"distances\":\"0,30,30\",\"elevations\":\"87,90,88\",\"provisionName\":\"Service Road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Short un-named link\",\"legNumber\":\"2\",\"distance\":\"74\",\"time\":\"69\",\"busynance\":\"509\",\"quietness\":\"60\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"115\",\"color\":\"#7777cc\",\"points\":\"0.1887,51.32514 0.18919,51.325 0.18956,51.32495 0.18971,51.32498\",\"distances\":\"0,37,26,11\",\"elevations\":\"88,93,95,97\",\"provisionName\":\"Service Road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Shoreham Road, A225\",\"legNumber\":\"2\",\"distance\":\"658\",\"time\":\"197\",\"busynance\":\"4356\",\"quietness\":\"20\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn right\",\"startBearing\":\"152\",\"color\":\"#3333aa\",\"points\":\"0.18971,51.32498 0.19,51.32464 0.1902,51.32437 0.19028,51.32421 0.19034,51.32404 0.19037,51.32376 0.19023,51.32296 0.1903,51.32257 0.19031,51.32245 0.19038,51.3221 0.19052,51.32164 0.19058,51.3214 0.1907,51.32118 0.19093,51.32087 0.19118,51.32064 0.19165,51.32034 0.19207,51.32005 0.19254,51.31955\",\"distances\":\"0,43,33,19,19,31,89,44,13,39,52,27,26,38,31,47,43,64\",\"elevations\":\"97,96,96,97,98,100,105,107,107,106,108,108,109,108,103,102,97,93\",\"provisionName\":\"Major road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Shoreham Road, A225\",\"legNumber\":\"3\",\"distance\":\"692\",\"time\":\"177\",\"busynance\":\"3863\",\"quietness\":\"20\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"155\",\"color\":\"#3333aa\",\"points\":\"0.19254,51.31955 0.19268,51.31936 0.19288,51.31907 0.19314,51.3187 0.19325,51.31854 0.19329,51.3185 0.19374,51.31797 0.19392,51.31771 0.19407,51.31746 0.19426,51.31724 0.19462,51.31689 0.1954,51.31616 0.19573,51.31584 0.19634,51.31524 0.19658,51.31505 0.19664,51.31502 0.19687,51.31491 0.19721,51.31471 0.19742,51.31457 0.19751,51.3145 0.19766,51.31439 0.19773,51.31429\",\"distances\":\"0,23,35,45,19,5,67,32,30,28,46,98,42,79,27,5,20,32,21,10,16,12\",\"elevations\":\"93,93,92,91,91,91,88,86,85,85,84,83,84,86,88,88,89,89,89,89,89,89\",\"provisionName\":\"Major road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Station Road, A225\",\"legNumber\":\"3\",\"distance\":\"115\",\"time\":\"22\",\"busynance\":\"443\",\"quietness\":\"20\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"165\",\"color\":\"#3333aa\",\"points\":\"0.19773,51.31429 0.19776,51.31422 0.19777,51.31417 0.19776,51.31411 0.19773,51.31405 0.19755,51.31382 0.19735,51.31365 0.19717,51.31353 0.19709,51.31349 0.19696,51.31343\",\"distances\":\"0,8,6,7,7,28,23,18,7,11\",\"elevations\":\"89,89,89,89,89,87,86,86,86,86\",\"provisionName\":\"Major road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"SR48\",\"legNumber\":\"3\",\"distance\":\"101\",\"time\":\"32\",\"busynance\":\"105\",\"quietness\":\"80\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn left\",\"startBearing\":\"155\",\"color\":\"#7777cc\",\"points\":\"0.19696,51.31343 0.19737,51.31287 0.19756,51.3126\",\"distances\":\"0,68,33\",\"elevations\":\"86,83,83\",\"provisionName\":\"Service Road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Pedestrian area\",\"legNumber\":\"3\",\"distance\":\"22\",\"time\":\"22\",\"busynance\":\"26\",\"quietness\":\"70\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"bear left\",\"startBearing\":\"90\",\"color\":\"#aaaacc\",\"points\":\"0.19756,51.3126 0.1977,51.3126 0.19784,51.31253\",\"distances\":\"0,10,12\",\"elevations\":\"83,83,82\",\"provisionName\":\"Pedestrianized area\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"0192\\/SR48\\/1\",\"legNumber\":\"3\",\"distance\":\"164\",\"time\":\"159\",\"busynance\":\"365\",\"quietness\":\"50\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"111\",\"color\":\"#008800\",\"points\":\"0.19784,51.31253 0.19809,51.31247 0.19816,51.31244 0.19825,51.31241 0.19828,51.31235 0.1986,51.31183 0.19884,51.31155 0.19917,51.31153 0.19924,51.31153\",\"distances\":\"0,19,6,7,7,62,35,23,5\",\"elevations\":\"82,82,82,82,82,81,81,81,82\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Tudor Drive\",\"legNumber\":\"3\",\"distance\":\"34\",\"time\":\"6\",\"busynance\":\"63\",\"quietness\":\"40\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn right\",\"startBearing\":\"182\",\"color\":\"#000000\",\"points\":\"0.19924,51.31153 0.19922,51.31122\",\"distances\":\"0,34\",\"elevations\":\"82,81\",\"provisionName\":\"Residential street\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Short dismounted link\",\"legNumber\":\"3\",\"distance\":\"69\",\"time\":\"76\",\"busynance\":\"254\",\"quietness\":\"50\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn left\",\"startBearing\":\"85\",\"color\":\"#008800\",\"points\":\"0.19922,51.31122 0.19975,51.31125 0.20019,51.31118\",\"distances\":\"0,37,32\",\"elevations\":\"81,83,83\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Link between Dynes Road and Knave Wood Road\",\"legNumber\":\"3\",\"distance\":\"691\",\"time\":\"293\",\"busynance\":\"1731\",\"quietness\":\"59\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"118\",\"color\":\"#888833\",\"points\":\"0.20019,51.31118 0.20031,51.31114 0.2007,51.31108 0.20107,51.31097 0.20158,51.31075 0.20239,51.31035 0.20265,51.31021 0.20635,51.30848 0.2066,51.30843 0.20691,51.30838 0.20772,51.30822 0.20812,51.30815 0.20839,51.30806 0.20862,51.30802\",\"distances\":\"0,9,28,28,43,72,24,321,18,22,59,29,21,17\",\"elevations\":\"83,83,85,86,87,88,88,90,92,94,95,95,94,94\",\"provisionName\":\"Bridleway\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Dynes Road\",\"legNumber\":\"3\",\"distance\":\"552\",\"time\":\"131\",\"busynance\":\"1429\",\"quietness\":\"40\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"106\",\"color\":\"#000000\",\"points\":\"0.20862,51.30802 0.21052,51.30768 0.21138,51.3076 0.21236,51.3075 0.21249,51.30748 0.21318,51.3074 0.21346,51.3073 0.21427,51.30693 0.2161,51.30649\",\"distances\":\"0,137,60,69,9,49,22,70,136\",\"elevations\":\"94,91,90,90,90,90,90,91,92\",\"provisionName\":\"Residential street\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"West End\",\"legNumber\":\"3\",\"distance\":\"80\",\"time\":\"29\",\"busynance\":\"323\",\"quietness\":\"40\",\"flow\":\"0\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"98\",\"color\":\"#000000\",\"points\":\"0.2161,51.30649 0.21707,51.30641 0.21724,51.30639\",\"distances\":\"0,68,12\",\"elevations\":\"92,94,94\",\"provisionName\":\"Residential street\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"West End\",\"legNumber\":\"4\",\"distance\":\"80\",\"time\":\"15\",\"busynance\":\"167\",\"quietness\":\"40\",\"flow\":\"0\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"double-back\",\"startBearing\":\"278\",\"color\":\"#000000\",\"points\":\"0.21724,51.30639 0.21707,51.30641 0.21610,51.30649\",\"distances\":\"0,12,68\",\"elevations\":\"94,94,92\",\"provisionName\":\"Residential street\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Dynes Road\",\"legNumber\":\"4\",\"distance\":\"552\",\"time\":\"150\",\"busynance\":\"1638\",\"quietness\":\"40\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"291\",\"color\":\"#000000\",\"points\":\"0.21610,51.30649 0.21427,51.30693 0.21346,51.3073 0.21318,51.3074 0.21249,51.30748 0.21236,51.3075 0.21138,51.3076 0.21052,51.30768 0.20862,51.30802\",\"distances\":\"0,136,70,22,49,9,69,60,137\",\"elevations\":\"92,91,90,90,90,90,90,91,94\",\"provisionName\":\"Residential street\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Link between Dynes Road and Knave Wood Road\",\"legNumber\":\"4\",\"distance\":\"691\",\"time\":\"204\",\"busynance\":\"1073\",\"quietness\":\"59\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"286\",\"color\":\"#888833\",\"points\":\"0.20862,51.30802 0.20839,51.30806 0.20812,51.30815 0.20772,51.30822 0.20691,51.30838 0.2066,51.30843 0.20635,51.30848 0.20265,51.31021 0.20239,51.31035 0.20158,51.31075 0.20107,51.31097 0.2007,51.31108 0.20031,51.31114 0.20019,51.31118\",\"distances\":\"0,17,21,29,59,22,18,321,24,72,43,28,28,9\",\"elevations\":\"94,94,95,95,94,92,90,88,88,87,86,85,83,83\",\"provisionName\":\"Bridleway\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Short dismounted link\",\"legNumber\":\"4\",\"distance\":\"69\",\"time\":\"61\",\"busynance\":\"121\",\"quietness\":\"50\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"284\",\"color\":\"#008800\",\"points\":\"0.20019,51.31118 0.19975,51.31125 0.19922,51.31122\",\"distances\":\"0,32,37\",\"elevations\":\"83,83,81\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Tudor Drive\",\"legNumber\":\"4\",\"distance\":\"34\",\"time\":\"14\",\"busynance\":\"152\",\"quietness\":\"40\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn right\",\"startBearing\":\"2\",\"color\":\"#000000\",\"points\":\"0.19922,51.31122 0.19924,51.31153\",\"distances\":\"0,34\",\"elevations\":\"81,82\",\"provisionName\":\"Residential street\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"0192\\/SR48\\/1\",\"legNumber\":\"4\",\"distance\":\"164\",\"time\":\"164\",\"busynance\":\"413\",\"quietness\":\"50\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn left\",\"startBearing\":\"270\",\"color\":\"#008800\",\"points\":\"0.19924,51.31153 0.19917,51.31153 0.19884,51.31155 0.1986,51.31183 0.19828,51.31235 0.19825,51.31241 0.19816,51.31244 0.19809,51.31247 0.19784,51.31253\",\"distances\":\"0,5,23,35,62,7,7,6,19\",\"elevations\":\"82,81,81,81,82,82,82,82,82\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Pedestrian area\",\"legNumber\":\"4\",\"distance\":\"22\",\"time\":\"28\",\"busynance\":\"64\",\"quietness\":\"70\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"309\",\"color\":\"#aaaacc\",\"points\":\"0.19784,51.31253 0.1977,51.3126 0.19756,51.3126\",\"distances\":\"0,12,10\",\"elevations\":\"82,83,83\",\"provisionName\":\"Pedestrianized area\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"SR48\",\"legNumber\":\"4\",\"distance\":\"101\",\"time\":\"50\",\"busynance\":\"205\",\"quietness\":\"80\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"bear right\",\"startBearing\":\"336\",\"color\":\"#7777cc\",\"points\":\"0.19756,51.3126 0.19737,51.31287 0.19696,51.31343\",\"distances\":\"0,33,68\",\"elevations\":\"83,83,86\",\"provisionName\":\"Service Road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Station Road, A225\",\"legNumber\":\"4\",\"distance\":\"413\",\"time\":\"75\",\"busynance\":\"1502\",\"quietness\":\"20\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn left\",\"startBearing\":\"243\",\"color\":\"#3333aa\",\"points\":\"0.19696,51.31343 0.19668,51.31334 0.19636,51.31327 0.19608,51.31325 0.19591,51.31323 0.19571,51.31322 0.19564,51.31321 0.19542,51.31321 0.19509,51.31318 0.19482,51.31315 0.19473,51.31314 0.19458,51.31312 0.19401,51.31303 0.19365,51.31297 0.19328,51.31293 0.19298,51.31291 0.19281,51.31292 0.19239,51.31293 0.19213,51.31293 0.19193,51.31294 0.19174,51.31294 0.19157,51.31293 0.19133,51.3129 0.19123,51.31289 0.19114,51.31288\",\"distances\":\"0,22,24,20,12,14,5,15,23,19,6,11,41,26,26,21,12,29,18,14,13,12,17,7,6\",\"elevations\":\"86,83,81,80,79,78,78,77,77,77,77,76,73,73,72,72,72,72,71,71,70,70,69,69,69\",\"provisionName\":\"Major road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"A225\",\"legNumber\":\"4\",\"distance\":\"89\",\"time\":\"21\",\"busynance\":\"401\",\"quietness\":\"20\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"bear left\",\"startBearing\":\"224\",\"color\":\"#3333aa\",\"points\":\"0.19114,51.31288 0.191,51.31279 0.19085,51.31261 0.1908,51.31258 0.19076,51.31256 0.19072,51.31255 0.19068,51.31255 0.19056,51.31255 0.19049,51.31256 0.19044,51.31258 0.19041,51.3126 0.19035,51.31266 0.19031,51.31271 0.1903,51.31274\",\"distances\":\"0,14,23,5,4,3,3,8,5,4,3,8,6,3\",\"elevations\":\"69,68,68,68,68,68,67,67,67,67,67,67,67,67\",\"provisionName\":\"Major road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"West High Street\",\"legNumber\":\"4\",\"distance\":\"31\",\"time\":\"8\",\"busynance\":\"119\",\"quietness\":\"30\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"bear left\",\"startBearing\":\"309\",\"color\":\"#33aa33\",\"points\":\"0.1903,51.31274 0.19024,51.31277 0.19021,51.3128 0.19016,51.31283 0.19007,51.31286 0.19,51.31288 0.18994,51.3129\",\"distances\":\"0,5,4,5,7,5,5\",\"elevations\":\"67,67,67,67,67,67,67\",\"provisionName\":\"Minor road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"High Street\",\"legNumber\":\"4\",\"distance\":\"614\",\"time\":\"143\",\"busynance\":\"1944\",\"quietness\":\"30\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"283\",\"color\":\"#33aa33\",\"points\":\"0.18994,51.3129 0.18937,51.31298 0.1886,51.31298 0.18844,51.31298 0.18838,51.31298 0.18831,51.31298 0.1882,51.31298 0.18752,51.31296 0.18646,51.31292 0.18631,51.31291 0.18613,51.3129 0.18569,51.31288 0.18561,51.31287 0.18492,51.31274 0.18466,51.31266 0.18402,51.31254 0.18378,51.31249 0.18308,51.31233 0.18292,51.31228 0.1828,51.31224 0.18213,51.312 0.18158,51.31184 0.18149,51.31181\",\"distances\":\"0,41,54,11,4,5,8,47,74,10,13,31,6,50,20,46,18,52,12,9,54,42,7\",\"elevations\":\"67,67,66,66,66,66,65,65,63,63,62,62,62,61,61,59,59,59,59,59,59,60,60\",\"provisionName\":\"Minor road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Rye Lane\",\"legNumber\":\"4\",\"distance\":\"112\",\"time\":\"39\",\"busynance\":\"567\",\"quietness\":\"30\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"bear left\",\"startBearing\":\"191\",\"color\":\"#33aa33\",\"points\":\"0.18149,51.31181 0.18146,51.31171 0.18139,51.3116 0.1813,51.3115 0.18114,51.31136 0.18079,51.3111 0.18074,51.31106 0.18074,51.31095\",\"distances\":\"0,11,13,13,19,38,6,12\",\"elevations\":\"60,60,60,61,61,62,62,62\",\"provisionName\":\"Unclassified road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Willow Park\",\"legNumber\":\"4\",\"distance\":\"537\",\"time\":\"188\",\"busynance\":\"2056\",\"quietness\":\"40\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn right\",\"startBearing\":\"270\",\"color\":\"#000000\",\"points\":\"0.18074,51.31095 0.18062,51.31095 0.18056,51.31095 0.18048,51.31095 0.18045,51.31093 0.18026,51.31083 0.17994,51.31066 0.17979,51.31058 0.17964,51.31053 0.17948,51.31049 0.17939,51.31049 0.17931,51.31049 0.17916,51.31048 0.17895,51.31046 0.17879,51.31043 0.1786,51.31038 0.17842,51.31032 0.1782,51.3102 0.17799,51.31005 0.17787,51.30992 0.17779,51.30977 0.17778,51.30972 0.17775,51.30961 0.17775,51.30954 0.17774,51.30947 0.17772,51.30942 0.17766,51.30933 0.17755,51.30915 0.17708,51.3087 0.1766,51.30856 0.17607,51.3087 0.17533,51.30912\",\"distances\":\"0,8,4,6,3,17,29,14,12,12,6,6,10,15,12,14,14,20,22,17,18,6,12,8,8,6,11,21,60,37,40,69\",\"elevations\":\"62,62,62,62,62,63,64,64,65,65,65,65,65,66,66,67,67,67,67,67,67,67,67,67,67,67,67,68,69,70,69,72\",\"provisionName\":\"Residential street\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Telston Lane\",\"legNumber\":\"4\",\"distance\":\"36\",\"time\":\"16\",\"busynance\":\"168\",\"quietness\":\"40\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn left\",\"startBearing\":\"219\",\"color\":\"#000000\",\"points\":\"0.17533,51.30912 0.17501,51.30887\",\"distances\":\"0,36\",\"elevations\":\"72,73\",\"provisionName\":\"Residential street\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Telston Lane\",\"legNumber\":\"4\",\"distance\":\"223\",\"time\":\"65\",\"busynance\":\"952\",\"quietness\":\"30\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"205\",\"color\":\"#33aa33\",\"points\":\"0.17501,51.30887 0.17479,51.30857 0.17461,51.3083 0.17432,51.30792 0.17408,51.3077 0.17365,51.30712 0.1736,51.30709\",\"distances\":\"0,37,33,47,30,71,5\",\"elevations\":\"73,74,75,76,76,75,75\",\"provisionName\":\"Unclassified road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Telston Lane\",\"legNumber\":\"4\",\"distance\":\"287\",\"time\":\"175\",\"busynance\":\"2586\",\"quietness\":\"30\",\"walk\":\"0\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"bear right\",\"startBearing\":\"270\",\"color\":\"#33aa33\",\"points\":\"0.1736,51.30709 0.17355,51.30709 0.17347,51.30709 0.17326,51.30711 0.17315,51.30709 0.17304,51.30703 0.1728,51.30669 0.17259,51.30644 0.17246,51.30631 0.17231,51.30623 0.17188,51.30604 0.17123,51.30576 0.17115,51.30573 0.17059,51.30554\",\"distances\":\"0,3,6,15,8,10,41,31,17,14,37,55,6,44\",\"elevations\":\"75,75,75,76,76,77,80,82,83,84,87,90,91,93\",\"provisionName\":\"Unclassified road\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Bridge\",\"legNumber\":\"4\",\"distance\":\"36\",\"time\":\"67\",\"busynance\":\"558\",\"quietness\":\"20\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"246\",\"color\":\"#008800\",\"points\":\"0.17059,51.30554 0.17012,51.30541\",\"distances\":\"0,36\",\"elevations\":\"93,96\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Link with SR100\",\"legNumber\":\"4\",\"distance\":\"6\",\"time\":\"15\",\"busynance\":\"186\",\"quietness\":\"20\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"248\",\"color\":\"#008800\",\"points\":\"0.17012,51.30541 0.17004,51.30539\",\"distances\":\"0,6\",\"elevations\":\"96,97\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"SR100\",\"legNumber\":\"4\",\"distance\":\"183\",\"time\":\"195\",\"busynance\":\"938\",\"quietness\":\"20\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"turn left\",\"startBearing\":\"163\",\"color\":\"#008800\",\"points\":\"0.17004,51.30539 0.1706,51.30424 0.17073,51.30381\",\"distances\":\"0,134,49\",\"elevations\":\"97,96,97\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}},{\"@attributes\":{\"name\":\"Link with SR100\",\"legNumber\":\"4\",\"distance\":\"146\",\"time\":\"130\",\"busynance\":\"642\",\"quietness\":\"20\",\"walk\":\"1\",\"signalledJunctions\":\"0\",\"signalledCrossings\":\"0\",\"turn\":\"straight on\",\"startBearing\":\"168\",\"color\":\"#008800\",\"points\":\"0.17073,51.30381 0.17114,51.30265 0.17117,51.30253\",\"distances\":\"0,132,14\",\"elevations\":\"97,95,94\",\"provisionName\":\"Footpath\",\"type\":\"segment\"}}],\"waypoint\":[{\"@attributes\":{\"longitude\":\"0.17117\",\"latitude\":\"51.30253\",\"sequenceId\":\"1\"}}],\"poi\":[{\"@attributes\":{\"poitypeId\":\"naturereserves\",\"name\":\"Polhill Bank Nature Reserve\",\"website\":\"\",\"longitude\":\"0.16682\",\"latitude\":\"51.32432\",\"sequenceId\":\"2\"}},{\"@attributes\":{\"poitypeId\":\"naturereserves\",\"name\":\"Fackenden Down Nature Reserve\",\"website\":\"https:\\/\\/www.wildlifetrusts.org\\/reserves\\/fackenden-down\",\"longitude\":\"0.19566\",\"latitude\":\"51.32082\",\"sequenceId\":\"3\"}},{\"@attributes\":{\"poitypeId\":\"busstops\",\"name\":\"West End\",\"website\":\"\",\"longitude\":\"0.21723\",\"latitude\":\"51.30633\",\"sequenceId\":\"4\"}}]}");
    return JourneyStringTransformerKt.fromV1ApiJson(response.body());
  }

  public String retrievePreviousJourneyJson(final String plan,
                                            final long itineraryId) throws IOException {
    Response<String> response = v1Api.retrievePreviousJourneyJson(plan, itineraryId, REPORT_ERRORS).execute();
    return JourneyStringTransformerKt.fromV1ApiJson(response.body());
  }

  public Blog getBlogEntries() throws IOException {
    Response<BlogFeedDto> response = blogApi.getBlogEntries().execute();
    return response.body().toBlog();
  }

  // --------------------------------------------------------------------------------
  // V2 APIs
  // --------------------------------------------------------------------------------

  public POICategories getPOICategories() throws IOException {
    Response<PoiTypesDto> response = v2Api.getPOICategories().execute();
    return response.body().toPOICategories(context);
  }

  public List<POI> getPOIs(final String type,
                           final double lonW,
                           final double latS,
                           final double lonE,
                           final double latN) throws IOException {
    String bbox = toBboxString(lonW, latS, lonE, latN);
    Response<FeatureCollection> response = v2Api.getPOIs(type, bbox).execute();
    return PoiFactory.toPoiList(response.body());
  }

  public List<POI> getPOIs(final String type,
                           final double lon,
                           final double lat,
                           final int radius) throws IOException {
    Response<FeatureCollection> response = v2Api.getPOIs(type, lon, lat, radius).execute();
    return PoiFactory.toPoiList(response.body());
  }

  public GeoPlaces geoCoder(final String search,
                            final double lonW,
                            final double latS,
                            final double lonE,
                            final double latN) throws IOException {
    String bbox = toBboxString(lonW, latS, lonE, latN);
    Response<FeatureCollection> response = v2Api.geoCoder(search, bbox).execute();
    return GeoPlacesFactory.toGeoPlaces(response.body());
  }

  public PhotomapCategories getPhotomapCategories() throws IOException {
    Response<PhotomapCategoriesDto> response = v2Api.getPhotomapCategories().execute();
    return response.body().toPhotomapCategories();
  }

  public Photos getPhotos(final double lonW,
                          final double latS,
                          final double lonE,
                          final double latN) throws IOException {
    String bbox = toBboxString(lonW, latS, lonE, latN);
    Response<FeatureCollection> response = v2Api.getPhotos(bbox).execute();
    return PhotosFactory.toPhotos(response.body());
  }

  public Photos getPhoto(final long photoId) throws IOException {
    Response<FeatureCollection> response = v2Api.getPhoto(photoId).execute();
    return PhotosFactory.toPhotos(response.body());
  }

  public UserJourneys getUserJourneys(final String username) throws IOException {
    Response<UserJourneysDto> response = v2Api.getUserJourneys(username).execute();
    return response.body().toUserJourneys();
  }

  public Result register(final String username,
                         final String password,
                         final String name,
                         final String email) throws IOException {
    Response<UserCreateResponseDto> response = v2Api.register(username, password, name, email).execute();
    return response.body().toRegistrationResult();
  }

  public Signin.Result authenticate(final String identifier,
                                    final String password) throws IOException {
    Response<UserAuthenticateResponseDto> response = v2Api.authenticate(identifier, password).execute();
    return response.body().toSigninResult();
  }

  public Result sendFeedback(final int itinerary,
                             final String comments,
                             final String name,
                             final String email) throws IOException {
    Response<SendFeedbackResponseDto> response = v2Api.sendFeedback("routing", itinerary, comments, name, email).execute();
    return response.body().toFeedbackResult();
  }

  public Upload.Result uploadPhoto(final String username,
                                   final String password,
                                   final double lon,
                                   final double lat,
                                   final long dateTime,
                                   final String category,
                                   final String metaCat,
                                   final String caption,
                                   final String filename) throws IOException {
    MultipartBody.Part filePart = null;
    if (filename != null) {
      File file = new File(filename);
      RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
      filePart = MultipartBody.Part.createFormData("mediaupload", file.getName(), fileBody);
    }
    // Unfortunately we have to do all this faff, otherwise the JSON converter will insert quotes!
    RequestBody usernamePart = RequestBody.create(MediaType.parse("text/plain"), username);
    RequestBody passwordPart = RequestBody.create(MediaType.parse("text/plain"), password);
    RequestBody categoryPart = RequestBody.create(MediaType.parse("text/plain"), category);
    RequestBody metaCatPart = RequestBody.create(MediaType.parse("text/plain"), metaCat);
    RequestBody captionPart = RequestBody.create(MediaType.parse("text/plain"), caption);
    Response<UploadPhotoResponseDto> response = v2Api.uploadPhoto(usernamePart, passwordPart, lon, lat, dateTime, categoryPart, metaCatPart, captionPart, filePart).execute();
    return response.body().toUploadResult();
  }
}
