package com.farego.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.farego.app.BuildConfig;
import com.farego.app.R;
import com.farego.app.db.AppDatabase;
import com.farego.app.db.entity.FareRate;
import com.farego.app.model.FareResult;
import com.farego.app.model.RouteInfo;
import com.farego.app.network.RetrofitClient;
import com.farego.app.network.model.DirectionsResponse;
import com.farego.app.service.TripNavigationService;
import com.farego.app.ui.bottomsheet.TransportBottomSheet;
import com.farego.app.utils.FareCalculator;
import com.farego.app.utils.PolylineDecoder;
import com.farego.app.utils.SessionManager;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.farego.app.db.entity.RouteHistory;
import com.farego.app.db.dao.RouteHistoryDao;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, TextToSpeech.OnInitListener {

    private static final int PERM_LOCATION   = 100;
    private static final int REQ_DESTINATION = 200;

    // Only reroute if user is more than 50m off the route
    private static final float REROUTE_THRESHOLD_METERS = 50f;

    // Map & Location
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private LatLng currentLatLng;
    private Marker userMarker;
    private Marker destMarker;
    private Polyline routePolyline;

    // Track whether a reroute fetch is already in flight
    private boolean isRerouting = false;

    // UI
    private TextView tvOrigin, tvDestination, tvEta, tvDistance, tvFare, tvTrafficBadge;
    private View cardTripInfo;
    private FloatingActionButton fabMyLocation;
    private ExtendedFloatingActionButton fabStartTrip;
    private ProgressBar pbRouteLoading;
    private BottomNavigationView bottomNav;

    // State
    private RouteInfo currentRoute;
    private FareCalculator.TransportType selectedTransport = FareCalculator.TransportType.TROTRO;
    private FareCalculator.TrafficCondition trafficCondition = FareCalculator.TrafficCondition.LOW;
    private boolean tripActive = false;

    // Services
    private TextToSpeech tts;
    private AppDatabase db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db      = AppDatabase.getInstance(this);
        session = new SessionManager(this);
        tts     = new TextToSpeech(this, this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY, Locale.ENGLISH);
        }

        AppDatabase.DB_EXECUTOR.execute(() -> AppDatabase.getInstance(this));

        bindViews();
        setupMap();
        setupLocationClient();
        setupClickListeners();
        setupBottomNav();

        int reuseId = getIntent().getIntExtra("reuse_route_id", -1);
        if (reuseId != -1) {
            AppDatabase.DB_EXECUTOR.execute(() -> {
                RouteHistory h = db.routeHistoryDao().getById(reuseId);
                if (h != null) runOnUiThread(() -> loadRoute(h));
            });
        }
    }

    private void bindViews() {
        tvOrigin       = findViewById(R.id.tv_origin);
        tvDestination  = findViewById(R.id.tv_destination);
        tvEta          = findViewById(R.id.tv_eta);
        tvDistance     = findViewById(R.id.tv_distance);
        tvFare         = findViewById(R.id.tv_fare);
        tvTrafficBadge = findViewById(R.id.tv_traffic_badge);
        cardTripInfo   = findViewById(R.id.card_trip_info);
        fabMyLocation  = findViewById(R.id.fab_my_location);
        fabStartTrip   = findViewById(R.id.fab_start_trip);
        pbRouteLoading = findViewById(R.id.pb_route_loading);
        bottomNav      = findViewById(R.id.bottom_nav);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    private void setupLocationClient() {
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (result.getLastLocation() == null) return;
                currentLatLng = new LatLng(
                        result.getLastLocation().getLatitude(),
                        result.getLastLocation().getLongitude());

                // Always update the marker position only — no camera movement
                updateUserMarker(currentLatLng);

                // Only reroute if trip is active AND user has gone off route
                if (tripActive && currentRoute != null) {
                    if (!isOnRoute(currentLatLng)) {
                        reroute();
                    }
                }
            }
        };
    }

    // Check if current position is within 50m of any point on the route
    // true  = still on route, do nothing
    // false = off route, trigger reroute
    private boolean isOnRoute(LatLng currentPos) {
        if (routePolyline == null) return false;

        List<LatLng> points = routePolyline.getPoints();
        for (LatLng point : points) {
            float[] result = new float[1];
            Location.distanceBetween(
                    currentPos.latitude, currentPos.longitude,
                    point.latitude,      point.longitude,
                    result
            );
            if (result[0] < REROUTE_THRESHOLD_METERS) {
                return true;
            }
        }
        return false;
    }

    private void setupClickListeners() {
        tvDestination.setOnClickListener(v -> launchDestinationAutocomplete());
        tvOrigin.setOnClickListener(v -> {
            if (currentLatLng != null) tvOrigin.setText("My Location");
        });
        fabMyLocation.setOnClickListener(v -> {
            if (currentLatLng != null && googleMap != null) animateCameraTo(currentLatLng, 16f);
        });
        fabStartTrip.setOnClickListener(v -> {
            if (currentRoute != null) {
                if (!tripActive) startTrip();
                else             stopTrip();
            } else {
                Toast.makeText(this, "Please select a destination first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)    return true;
            if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    // ── Map Ready ────────────────────────────────────────────
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        } catch (Exception e) { /* style not critical */ }
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(true);
        LatLng accra = new LatLng(5.6037, -0.1870);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(accra, 13f));
        checkLocationPermission();
    }

    // ── Permissions ──────────────────────────────────────────
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERM_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERM_LOCATION && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED)
            enableLocation();
    }

    private void enableLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateIntervalMillis(2000).build();
        fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
        fusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                currentLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
                animateCameraTo(currentLatLng, 15f);
                tvOrigin.setText("My Location");
            }
        });
    }

    // ── Destination Autocomplete ─────────────────────────────
    private void launchDestinationAutocomplete() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY, Locale.ENGLISH);
        }
        try {
            List<Place.Field> fields = Arrays.asList(
                    Place.Field.ID, Place.Field.NAME,
                    Place.Field.LAT_LNG, Place.Field.ADDRESS);
            Intent intent = new Autocomplete
                    .IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                    .setCountries(Arrays.asList("GH"))
                    .build(this);
            startActivityForResult(intent, REQ_DESTINATION);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Search unavailable. Check your API key.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_DESTINATION) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    Place place = Autocomplete.getPlaceFromIntent(data);
                    if (place.getLatLng() != null) {
                        onDestinationSelected(place.getName(), place.getLatLng());
                    } else {
                        Toast.makeText(this,
                                "Could not get location for that place.",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this,
                            "Failed to read place. Try again.",
                            Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
                com.google.android.gms.common.api.Status status =
                        Autocomplete.getStatusFromIntent(data);
                Toast.makeText(this,
                        "Search error: " + status.getStatusMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onDestinationSelected(String name, LatLng latLng) {
        tvDestination.setText(name);
        placeDestMarker(latLng);
        if (currentLatLng == null) {
            Toast.makeText(this, "Waiting for GPS…", Toast.LENGTH_SHORT).show();
            return;
        }
        currentRoute = new RouteInfo(currentLatLng, "My Location", latLng, name);
        // true = animate camera to fit full route on screen (first load only)
        fetchRoute(currentRoute, true);
        showTransportSheet();
    }

    // ── Route Fetching ───────────────────────────────────────
    // animateCamera = true  → zoom to fit route (destination selected / history reuse)
    // animateCamera = false → just redraw polyline, leave camera alone (reroute)
    private void fetchRoute(RouteInfo route, boolean animateCamera) {
        pbRouteLoading.setVisibility(View.VISIBLE);
        String origin = route.originLatLng.latitude + "," + route.originLatLng.longitude;
        String dest   = route.destinationLatLng.latitude + "," + route.destinationLatLng.longitude;

        RetrofitClient.getInstance().directionsApi().getRoute(
                origin, dest, "driving", "now", "best_guess", BuildConfig.MAPS_API_KEY
        ).enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<DirectionsResponse> call,
                                   @NonNull Response<DirectionsResponse> response) {
                pbRouteLoading.setVisibility(View.GONE);
                isRerouting = false;
                if (response.isSuccessful() && response.body() != null
                        && !response.body().routes.isEmpty()) {
                    processRoute(response.body(), animateCamera);
                } else {
                    offlineFallback(route);
                }
            }

            @Override
            public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable t) {
                pbRouteLoading.setVisibility(View.GONE);
                isRerouting = false;
                offlineFallback(route);
            }
        });
    }

    private void processRoute(DirectionsResponse resp, boolean animateCamera) {
        DirectionsResponse.Route r   = resp.routes.get(0);
        DirectionsResponse.Leg   leg = r.legs.get(0);

        double distKm   = leg.distance.value / 1000.0;
        int baseDur     = leg.duration.value / 60;
        int withTraffic = (leg.durationInTraffic != null)
                ? leg.durationInTraffic.value / 60 : baseDur;
        double ratio    = (double) withTraffic / Math.max(baseDur, 1);

        if (ratio < 1.15)      trafficCondition = FareCalculator.TrafficCondition.LOW;
        else if (ratio < 1.40) trafficCondition = FareCalculator.TrafficCondition.MODERATE;
        else                   trafficCondition = FareCalculator.TrafficCondition.HEAVY;

        currentRoute.distanceKm       = distKm;
        currentRoute.durationMinutes  = withTraffic;
        currentRoute.distanceText     = leg.distance.text;
        currentRoute.durationText     = leg.duration.text;
        currentRoute.trafficCondition = trafficCondition;
        currentRoute.polylinePoints   = PolylineDecoder.decode(r.overviewPolyline.points);

        drawRoute(currentRoute.polylinePoints, animateCamera);
        updateTripCard();
        updateFares();
    }

    private void offlineFallback(RouteInfo route) {
        double distKm = haversine(
                route.originLatLng.latitude,    route.originLatLng.longitude,
                route.destinationLatLng.latitude, route.destinationLatLng.longitude);
        route.distanceKm      = distKm;
        route.durationMinutes = (int)(distKm / 30.0 * 60);
        route.distanceText    = String.format("%.1f km", distKm);
        route.durationText    = route.durationMinutes + " min";
        route.trafficCondition = trafficCondition;
        updateTripCard();
        updateFares();
        Toast.makeText(this, "Offline fare estimate (no API key set)",
                Toast.LENGTH_SHORT).show();
    }

    // ── Map Drawing ──────────────────────────────────────────
    private void drawRoute(List<LatLng> points, boolean animateCamera) {
        if (googleMap == null || points == null || points.isEmpty()) return;
        if (routePolyline != null) routePolyline.remove();

        int lineColor = trafficCondition == FareCalculator.TrafficCondition.LOW
                ? Color.parseColor("#4CAF50")
                : trafficCondition == FareCalculator.TrafficCondition.MODERATE
                ? Color.parseColor("#FF9800")
                : Color.parseColor("#F44336");

        routePolyline = googleMap.addPolyline(new PolylineOptions()
                .addAll(points).width(14f).color(lineColor).geodesic(true)
                .jointType(JointType.ROUND)
                .startCap(new RoundCap())
                .endCap(new RoundCap()));

        // Only zoom to fit the route when explicitly requested
        // (first load or history reuse) — never during an active trip
        if (animateCamera) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng p : points) builder.include(p);
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
        }
    }

    private void updateUserMarker(LatLng pos) {
        if (googleMap == null) return;
        if (userMarker == null) {
            userMarker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE))
                    .title("You"));
        } else {
            // Just move the marker silently — no camera change at all
            userMarker.setPosition(pos);
        }
        // ── REMOVED: if (tripActive) animateCameraTo(pos, 17f)
        // That line was re-animating the camera every 2-4 seconds during a trip
        // which caused the constant zoom in/out the user was seeing
    }

    private void placeDestMarker(LatLng pos) {
        if (googleMap == null) return;
        if (destMarker != null) destMarker.remove();
        destMarker = googleMap.addMarker(new MarkerOptions()
                .position(pos)
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_YELLOW))
                .title("Destination"));
    }

    // Only runs when tripActive is false
    // During a trip the camera is set once in startTrip() and never touched again
    private void animateCameraTo(LatLng pos, float zoom) {
        if (tripActive) return; // protect against any stray calls during trip

        CameraPosition camPos = new CameraPosition.Builder()
                .target(pos).zoom(zoom).tilt(0f).build();
        googleMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(camPos), 800, null);
    }

    // ── UI Updates ───────────────────────────────────────────
    private void updateTripCard() {
        if (currentRoute == null) return;
        cardTripInfo.setVisibility(View.VISIBLE);
        tvEta.setText(currentRoute.durationMinutes + " min");
        tvDistance.setText(currentRoute.distanceText != null
                ? currentRoute.distanceText
                : String.format("%.1f km", currentRoute.distanceKm));
        tvTrafficBadge.setText(trafficCondition.label);
        tvTrafficBadge.setTextColor(trafficCondition.color);
    }

    private void updateFares() {
        if (currentRoute == null) return;
        AppDatabase.DB_EXECUTOR.execute(() -> {
            FareRate rate = db.fareRateDao().getByType(selectedTransport.name());
            FareResult result = rate != null
                    ? FareCalculator.calculate(
                    currentRoute.distanceKm, rate, trafficCondition)
                    : FareCalculator.estimateOffline(
                    currentRoute.distanceKm, selectedTransport, trafficCondition);
            runOnUiThread(() -> tvFare.setText(result.getFormattedRange()));
        });
    }

    // ── Transport Sheet ──────────────────────────────────────
    private void showTransportSheet() {
        if (currentRoute == null) return;
        TransportBottomSheet sheet = TransportBottomSheet.newInstance(
                currentRoute.distanceKm, trafficCondition);
        sheet.setOnTransportSelectedListener(type -> {
            selectedTransport = type;
            updateFares();
        });
        sheet.show(getSupportFragmentManager(), "transport_sheet");
    }

    // ── Route Reuse ──────────────────────────────────────────
    public void loadRoute(RouteHistory history) {
        LatLng dest = new LatLng(history.destinationLat, history.destinationLng);
        tvDestination.setText(history.destinationLabel);
        placeDestMarker(dest);
        currentRoute = new RouteInfo(
                currentLatLng != null ? currentLatLng
                        : new LatLng(history.originLat, history.originLng),
                "My Location", dest, history.destinationLabel);
        fetchRoute(currentRoute, true); // animate camera on history reuse
        showTransportSheet();
    }

    // ── Trip Lifecycle ───────────────────────────────────────
    private void startTrip() {
        tripActive = true;
        fabStartTrip.setText("End Trip");
        fabStartTrip.setIconResource(R.drawable.ic_stop);
        speak("Navigation started. " + tvDestination.getText()
                + " in " + currentRoute.durationMinutes + " minutes.");

        // Set camera ONCE at a comfortable zoom showing location + surroundings
        // After this nothing moves the camera so user can freely pan and zoom
        if (currentLatLng != null && googleMap != null) {
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f), 800, null);
        }

        long etaMs = (long) currentRoute.durationMinutes * 60 * 1000;
        Intent serviceIntent = new Intent(this, TripNavigationService.class);
        serviceIntent.setAction(TripNavigationService.ACTION_START);
        serviceIntent.putExtra(TripNavigationService.EXTRA_ETA_MS, etaMs);
        startService(serviceIntent);
        saveRouteToHistory();
    }

    private void stopTrip() {
        tripActive  = false;
        isRerouting = false;
        fabStartTrip.setText("Start Trip");
        fabStartTrip.setIconResource(R.drawable.ic_navigation);
        speak("Trip ended.");
        stopService(new Intent(this, TripNavigationService.class));
    }

    private void reroute() {
        // Guard against multiple reroute calls firing before the first one returns
        if (isRerouting) return;
        isRerouting = true;

        if (currentRoute == null || currentLatLng == null) return;
        currentRoute.originLatLng = currentLatLng;
        // false = redraw polyline only, do not touch the camera
        fetchRoute(currentRoute, false);
    }

    // ── Database ─────────────────────────────────────────────
    private void saveRouteToHistory() {
        if (currentRoute == null) return;
        AppDatabase.DB_EXECUTOR.execute(() -> {
            FareRate rate = db.fareRateDao().getByType(selectedTransport.name());
            FareResult result = rate != null
                    ? FareCalculator.calculate(
                    currentRoute.distanceKm, rate, trafficCondition)
                    : FareCalculator.estimateOffline(
                    currentRoute.distanceKm, selectedTransport, trafficCondition);

            RouteHistory h     = new RouteHistory();
            h.userId           = session.getUserId();
            h.originLabel      = "My Location";
            h.originLat        = currentRoute.originLatLng.latitude;
            h.originLng        = currentRoute.originLatLng.longitude;
            h.destinationLabel = currentRoute.destinationLabel;
            h.destinationLat   = currentRoute.destinationLatLng.latitude;
            h.destinationLng   = currentRoute.destinationLatLng.longitude;
            h.distanceKm       = currentRoute.distanceKm;
            h.durationMinutes  = currentRoute.durationMinutes;
            h.estimatedFare    = result.estimatedFare;
            h.transportType    = selectedTransport.name();
            h.trafficCondition = trafficCondition.name();
            h.timestamp        = System.currentTimeMillis();
            db.routeHistoryDao().insert(h);
        });
    }

    // ── TTS ──────────────────────────────────────────────────
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.ENGLISH);
    }

    private void speak(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // ── Haversine ────────────────────────────────────────────
    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R    = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a    = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng/2) * Math.sin(dLng/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (fusedClient != null && locationCallback != null)
            fusedClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }
}