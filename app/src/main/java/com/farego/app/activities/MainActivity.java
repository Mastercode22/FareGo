package com.farego.app.activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.farego.app.BuildConfig;
import com.farego.app.R;
import com.farego.app.db.AppDatabase;
import com.farego.app.db.entity.FareRate;
import com.farego.app.model.FareResult;
import com.farego.app.model.RouteInfo;
import com.farego.app.navigation.NavigationEngine;               // ← NEW
import com.farego.app.network.RetrofitClient;
import com.farego.app.network.model.DirectionsResponse;
import com.farego.app.service.TripNavigationService;
import com.farego.app.ui.bottomsheet.TransportBottomSheet;
import com.farego.app.ui.dialog.VoiceListeningDialog;
import com.farego.app.utils.FareCalculator;
import com.farego.app.utils.PolylineDecoder;
import com.farego.app.utils.SessionManager;
import com.farego.app.utils.VoiceSearchManager;

import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.farego.app.db.entity.RouteHistory;
import com.farego.app.db.dao.RouteHistoryDao;

/**
 * ════════════════════════════════════════════════════════════════════════════
 *  MainActivity.java  — FareGo  (Navigation-upgraded version)
 *
 *  CHANGES FROM v1 (marked with ← NEW or ← CHANGED):
 *
 *  1. NavigationEngine field — holds the engine instance for the lifetime
 *     of the activity.
 *
 *  2. setupLocationClient() — locationCallback now calls
 *     navigationEngine.handleNavigationUpdates() on every GPS fix while
 *     a trip is active.
 *
 *  3. processRoute() — now also extracts steps from the API leg and stores
 *     them in currentRoute.steps.  Also resets the navigation engine when
 *     a fresh route arrives (including reroutes).
 *
 *  4. startTrip() — calls navigationEngine.resetForNewRoute() to begin
 *     guidance from step 0.
 *
 *  5. stopTrip() — calls navigationEngine.stopNavigation() to halt guidance.
 *
 *  6. reroute() — after the new route is fetched, resetForNewRoute() is
 *     called again automatically inside processRoute().
 *
 *  Everything else is unchanged from the original file.
 * ════════════════════════════════════════════════════════════════════════════
 */
public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, TextToSpeech.OnInitListener {

    // ── Request codes ─────────────────────────────────────────────────────────
    private static final int PERM_LOCATION   = 100;
    private static final int REQ_DESTINATION = 200;

    /** How far from the polyline before we consider the user off-route. */
    private static final float REROUTE_THRESHOLD_METERS = 50f;

    // ── Map & Location ────────────────────────────────────────────────────────
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private LatLng currentLatLng;
    private Marker userMarker;
    private Marker destMarker;
    private Polyline routePolyline;
    private boolean isRerouting = false;

    // ── UI ────────────────────────────────────────────────────────────────────
    private TextView    tvOrigin;
    private EditText    etDestination;
    private ImageButton btnMic;
    private TextView    tvEta, tvDistance, tvFare, tvTrafficBadge;
    private View        cardTripInfo;
    private FloatingActionButton         fabMyLocation;
    private ExtendedFloatingActionButton fabStartTrip;
    private ProgressBar  pbRouteLoading;
    private BottomNavigationView bottomNav;

    // ── State ─────────────────────────────────────────────────────────────────
    private RouteInfo currentRoute;
    private FareCalculator.TransportType    selectedTransport = FareCalculator.TransportType.TROTRO;
    private FareCalculator.TrafficCondition trafficCondition  = FareCalculator.TrafficCondition.LOW;
    private boolean tripActive      = false;
    private long    tripStartTimeMs = 0;

    // ── Services ──────────────────────────────────────────────────────────────
    private TextToSpeech tts;
    private AppDatabase  db;
    private SessionManager session;

    // ── Voice ─────────────────────────────────────────────────────────────────
    private VoiceSearchManager   voiceManager;
    private VoiceListeningDialog voiceDialog;

    // ── Navigation engine (NEW) ───────────────────────────────────────────────
    /**
     * NavigationEngine drives all turn-by-turn logic.
     * Created in onCreate() after TTS is ready.
     * It holds a reference to the TTS instance and the reroute listener below.
     */
    private NavigationEngine navigationEngine;                   // ← NEW

    // ═════════════════════════════════════════════════════════════════════════
    //  onCreate
    // ═════════════════════════════════════════════════════════════════════════
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
        initVoice();
        initNavigationEngine();   // ← NEW — must be after tts is created
        setupMap();
        setupLocationClient();
        setupClickListeners();
        setupBottomNav();
        adjustCardForBottomNav();

        // Handle route reuse from HistoryActivity
        int reuseId = getIntent().getIntExtra("reuse_route_id", -1);
        if (reuseId != -1) {
            AppDatabase.DB_EXECUTOR.execute(() -> {
                RouteHistory h = db.routeHistoryDao().getById(reuseId);
                if (h != null) runOnUiThread(() -> loadRoute(h));
            });
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Navigation engine initialisation (NEW)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Creates the NavigationEngine and wires up the reroute callback.
     *
     * The RerouteListener.onOffRoute() is invoked by the engine whenever it
     * determines the user has wandered more than 60 m from the current step.
     * We respond by calling our existing reroute() method, which fetches a
     * fresh Directions API response from the user's current position.
     */
    private void initNavigationEngine() {                        // ← NEW
        navigationEngine = new NavigationEngine(
                this,
                tts,
                () -> {
                    // Called on the background thread that runs GPS callbacks.
                    // Post to main thread before touching UI state.
                    runOnUiThread(() -> {
                        if (tripActive && !isRerouting) {
                            speak("Recalculating route.");
                            reroute();
                        }
                    });
                }
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  View binding (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    private void bindViews() {
        tvOrigin       = findViewById(R.id.tv_origin);
        etDestination  = findViewById(R.id.et_destination);
        btnMic         = findViewById(R.id.btn_mic);
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

    // ═════════════════════════════════════════════════════════════════════════
    //  Voice initialisation (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    private void initVoice() {
        voiceManager = new VoiceSearchManager(this, voiceCallback);
        voiceDialog  = new VoiceListeningDialog(this);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Click listeners (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    private void setupClickListeners() {
        etDestination.setOnClickListener(v -> launchDestinationAutocomplete());
        etDestination.setShowSoftInputOnFocus(false);

        btnMic.setOnClickListener(v -> {
            animateMicButton();
            startVoiceInput();
        });

        tvOrigin.setOnClickListener(v -> {
            if (currentLatLng != null) tvOrigin.setText("My Location");
        });

        fabMyLocation.setOnClickListener(v -> {
            if (currentLatLng != null && googleMap != null)
                animateCameraTo(currentLatLng, 16f);
        });

        fabStartTrip.setOnClickListener(v -> {
            if (currentRoute != null) {
                if (!tripActive) startTrip();
                else             stopTrip();
            } else {
                Toast.makeText(this,
                        "Please select a destination first",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Voice — startVoiceInput (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    private void startVoiceInput() {
        if (voiceManager == null) {
            voiceManager = new VoiceSearchManager(this, voiceCallback);
        }
        voiceManager.startListening(this);
    }

    private void animateMicButton() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(
                btnMic, "rotation", 0f, -15f, 15f, -10f, 10f, 0f);
        anim.setDuration(400);
        anim.setInterpolator(new OvershootInterpolator());
        anim.start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Voice — callback (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    private final VoiceSearchManager.VoiceSearchCallback voiceCallback =
            new VoiceSearchManager.VoiceSearchCallback() {

                @Override
                public void onListeningStarted() {
                    runOnUiThread(() -> {
                        if (voiceDialog != null && !voiceDialog.isShowing()) {
                            voiceDialog.show();
                        }
                        etDestination.setHint("Listening...");
                    });
                }

                @Override
                public void onListeningEnded() {
                    runOnUiThread(() -> {
                        etDestination.setHint("Where to?");
                        if (voiceDialog != null && voiceDialog.isShowing()) {
                            voiceDialog.setStatus("Processing...");
                        }
                    });
                }

                @Override
                public void onPartialResult(String partial) {
                    runOnUiThread(() -> {
                        if (voiceDialog != null) voiceDialog.updatePartial(partial);
                        etDestination.setText(partial);
                    });
                }

                @Override
                public void onDestinationResolved(String rawText,
                                                  LatLng latLng,
                                                  String ttsAnnounce) {
                    runOnUiThread(() -> {
                        if (voiceDialog != null && voiceDialog.isShowing()) {
                            voiceDialog.dismiss();
                        }
                        if (latLng != null) {
                            speak(ttsAnnounce);
                            onDestinationSelected(rawText, latLng);
                        } else {
                            etDestination.setText(rawText);
                            Toast.makeText(MainActivity.this,
                                    "Couldn't locate \"" + rawText + "\". Try searching instead.",
                                    Toast.LENGTH_LONG).show();
                            launchDestinationAutocomplete();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        if (voiceDialog != null && voiceDialog.isShowing()) {
                            voiceDialog.dismiss();
                        }
                        etDestination.setHint("Where to?");
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            };

    // ═════════════════════════════════════════════════════════════════════════
    //  Bottom nav (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
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

    private void adjustCardForBottomNav() {
        bottomNav.post(() -> {
            int navHeight = bottomNav.getHeight();

            CoordinatorLayout.LayoutParams cardParams =
                    (CoordinatorLayout.LayoutParams) cardTripInfo.getLayoutParams();
            cardParams.bottomMargin = navHeight + 16;
            cardTripInfo.setLayoutParams(cardParams);

            CoordinatorLayout.LayoutParams fabParams =
                    (CoordinatorLayout.LayoutParams) fabMyLocation.getLayoutParams();
            fabParams.bottomMargin = navHeight + 100;
            fabMyLocation.setLayoutParams(fabParams);
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Map (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        try {
            googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        } catch (Exception ignored) { }

        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(true);

        LatLng accra = new LatLng(5.6037, -0.1870);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(accra, 13f));

        googleMap.setOnMapClickListener(latLng -> {
            if (tripActive) return;
            reverseGeocodeAndConfirm(latLng);
        });

        checkLocationPermission();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Location  (CHANGED — navigation engine call added)
    // ═════════════════════════════════════════════════════════════════════════
    private void setupLocationClient() {
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (result.getLastLocation() == null) return;

                // Update current position
                currentLatLng = new LatLng(
                        result.getLastLocation().getLatitude(),
                        result.getLastLocation().getLongitude());

                // Move the user marker on the map
                updateUserMarker(currentLatLng);

                // ── Navigation engine tick (NEW) ──────────────────────────
                // Feed every GPS update to the engine while a trip is active.
                // The engine handles distance measuring, voice, and off-route.
                // We only call it if the route has step data (live API response);
                // offline fallback routes won't have steps so guidance is skipped.
                if (tripActive && currentRoute != null
                        && navigationEngine != null
                        && currentRoute.hasSteps()) {                // ← NEW
                    navigationEngine.handleNavigationUpdates(currentLatLng, currentRoute);
                }

                // ── Legacy polyline off-route check ──────────────────────
                // Keep the original reroute guard as a safety net for cases
                // where the engine's step-level check doesn't fire first.
                if (tripActive && currentRoute != null && !isRerouting
                        && !isOnRoute(currentLatLng)) {
                    reroute();
                }
            }
        };
    }

    private boolean isOnRoute(LatLng pos) {
        if (routePolyline == null) return false;
        for (LatLng point : routePolyline.getPoints()) {
            float[] res = new float[1];
            Location.distanceBetween(pos.latitude, pos.longitude,
                    point.latitude, point.longitude, res);
            if (res[0] < REROUTE_THRESHOLD_METERS) return true;
        }
        return false;
    }

    // ── Permissions (unchanged) ───────────────────────────────────────────────
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
    public void onRequestPermissionsResult(int code,
                                           @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERM_LOCATION && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            enableLocation();
        }
        if (voiceManager != null) {
            boolean granted = voiceManager.onPermissionResult(code, perms, results);
            if (granted) startVoiceInput();
        }
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

    // ═════════════════════════════════════════════════════════════════════════
    //  Destination Autocomplete (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
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
            Toast.makeText(this, "Search unavailable. Check your API key.",
                    Toast.LENGTH_LONG).show();
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
                        Toast.makeText(this, "Could not get location for that place.",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to read place. Try again.",
                            Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
                com.google.android.gms.common.api.Status status =
                        Autocomplete.getStatusFromIntent(data);
                Toast.makeText(this, "Search error: " + status.getStatusMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Destination selection (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    public void onDestinationSelected(String name, LatLng latLng) {
        etDestination.setText(name);
        etDestination.clearFocus();
        placeDestMarker(latLng);
        if (currentLatLng == null) {
            Toast.makeText(this, "Waiting for GPS...", Toast.LENGTH_SHORT).show();
            return;
        }
        currentRoute = new RouteInfo(currentLatLng, "My Location", latLng, name);
        fetchRoute(currentRoute, true);
        showTransportSheet();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Reverse geocode (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    private void reverseGeocodeAndConfirm(LatLng latLng) {
        if (destMarker != null) destMarker.remove();
        destMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                .title("Loading address..."));
        pbRouteLoading.setVisibility(View.VISIBLE);

        AppDatabase.DB_EXECUTOR.execute(() -> {
            String resolvedName = getAddressFromLatLng(latLng);
            runOnUiThread(() -> {
                pbRouteLoading.setVisibility(View.GONE);
                if (destMarker != null) destMarker.setTitle(resolvedName);
                new AlertDialog.Builder(this)
                        .setTitle("Set as destination?")
                        .setMessage(resolvedName)
                        .setPositiveButton("Yes, go here", (d, w) ->
                                onDestinationSelected(resolvedName, latLng))
                        .setNegativeButton("Cancel", (d, w) -> {
                            if (destMarker != null) { destMarker.remove(); destMarker = null; }
                        })
                        .show();
            });
        });
    }

    private String getAddressFromLatLng(LatLng latLng) {
        if (!Geocoder.isPresent()) return formatCoords(latLng);
        try {
            Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                String feature = address.getFeatureName();
                if (feature != null && !feature.matches("[-0-9.]+")) sb.append(feature);
                String street = address.getThoroughfare();
                if (street != null && !street.equals(feature)) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(street);
                }
                String locality = address.getLocality();
                if (locality != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(locality);
                }
                if (sb.length() == 0) {
                    String sub = address.getSubAdminArea();
                    if (sub != null) sb.append(sub);
                }
                return sb.length() > 0 ? sb.toString() : formatCoords(latLng);
            }
        } catch (IOException ignored) { }
        return formatCoords(latLng);
    }

    private String formatCoords(LatLng latLng) {
        return String.format(Locale.ENGLISH, "%.5f, %.5f",
                latLng.latitude, latLng.longitude);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Route fetching (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
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
            public void onFailure(@NonNull Call<DirectionsResponse> call,
                                  @NonNull Throwable t) {
                pbRouteLoading.setVisibility(View.GONE);
                isRerouting = false;
                offlineFallback(route);
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  processRoute  (CHANGED — step extraction added)
    // ═════════════════════════════════════════════════════════════════════════
    private void processRoute(DirectionsResponse resp, boolean animateCamera) {
        DirectionsResponse.Route r   = resp.routes.get(0);
        DirectionsResponse.Leg   leg = r.legs.get(0);

        double distKm   = leg.distance.value / 1000.0;
        int baseDur     = leg.duration.value / 60;
        int withTraffic = (leg.durationInTraffic != null)
                ? leg.durationInTraffic.value / 60 : baseDur;
        double ratio    = (double) withTraffic / Math.max(baseDur, 1);

        if      (ratio < 1.15) trafficCondition = FareCalculator.TrafficCondition.LOW;
        else if (ratio < 1.40) trafficCondition = FareCalculator.TrafficCondition.MODERATE;
        else                   trafficCondition = FareCalculator.TrafficCondition.HEAVY;

        currentRoute.distanceKm      = distKm;
        currentRoute.durationMinutes = withTraffic;
        currentRoute.distanceText    = leg.distance.text;
        currentRoute.durationText    = leg.duration.text;
        currentRoute.trafficCondition = trafficCondition;
        currentRoute.polylinePoints  = PolylineDecoder.decode(r.overviewPolyline.points);

        // ── Extract turn-by-turn steps from the leg (NEW) ─────────────────────
        // Store the raw Step objects in RouteInfo so NavigationEngine can
        // read htmlInstructions, endLocation, maneuver, and distance.
        currentRoute.steps = leg.steps;                           // ← NEW

        drawRoute(currentRoute.polylinePoints, animateCamera);
        updateTripCard();
        updateFares();

        // ── If a trip is already active (reroute scenario), reset the engine ──
        // This ensures the step index restarts from 0 for the new route and
        // any previously set announcement flags are cleared.
        if (tripActive && navigationEngine != null) {             // ← NEW
            navigationEngine.resetForNewRoute();
            // Announce that rerouting is complete
            if (isRerouting) speak("Route updated.");
        }
    }

    private void offlineFallback(RouteInfo route) {
        double distKm = haversine(
                route.originLatLng.latitude, route.originLatLng.longitude,
                route.destinationLatLng.latitude, route.destinationLatLng.longitude);
        route.distanceKm      = distKm;
        route.durationMinutes = (int)(distKm / 30.0 * 60);
        route.distanceText    = String.format("%.1f km", distKm);
        route.durationText    = route.durationMinutes + " min";
        route.trafficCondition = trafficCondition;
        // Note: steps will be null in offline mode — navigation engine will skip
        updateTripCard();
        updateFares();
        Toast.makeText(this, "Offline fare estimate (no network)", Toast.LENGTH_SHORT).show();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Map drawing (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
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
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("You"));
        } else {
            userMarker.setPosition(pos);
        }
    }

    private void placeDestMarker(LatLng pos) {
        if (googleMap == null) return;
        if (destMarker != null) destMarker.remove();
        destMarker = googleMap.addMarker(new MarkerOptions()
                .position(pos)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                .title("Destination"));
    }

    private void animateCameraTo(LatLng pos, float zoom) {
        if (tripActive) return;
        CameraPosition camPos = new CameraPosition.Builder()
                .target(pos).zoom(zoom).tilt(0f).build();
        googleMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(camPos), 800, null);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI updates (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
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
                    ? FareCalculator.calculate(currentRoute.distanceKm, rate, trafficCondition)
                    : FareCalculator.estimateOffline(currentRoute.distanceKm,
                    selectedTransport, trafficCondition);
            runOnUiThread(() -> tvFare.setText(result.getFormattedRange()));
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Transport sheet (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
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

    // ═════════════════════════════════════════════════════════════════════════
    //  Route reuse (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    public void loadRoute(RouteHistory history) {
        LatLng dest = new LatLng(history.destinationLat, history.destinationLng);
        etDestination.setText(history.destinationLabel);
        placeDestMarker(dest);
        currentRoute = new RouteInfo(
                currentLatLng != null ? currentLatLng
                        : new LatLng(history.originLat, history.originLng),
                "My Location", dest, history.destinationLabel);
        fetchRoute(currentRoute, true);
        showTransportSheet();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Trip lifecycle  (CHANGED — engine hooks added)
    // ═════════════════════════════════════════════════════════════════════════
    private void startTrip() {
        tripActive      = true;
        tripStartTimeMs = System.currentTimeMillis();
        fabStartTrip.setText("End Trip");
        fabStartTrip.setIconResource(R.drawable.ic_stop);

        // ── Announce departure via TTS ────────────────────────────────────────
        speak("Navigation started. "
                + etDestination.getText()
                + " in " + currentRoute.durationMinutes + " minutes.");

        // ── Begin turn-by-turn guidance from step 0 (NEW) ────────────────────
        // resetForNewRoute() clears all per-step flags and enables the engine.
        // It must be called AFTER tripActive = true so processRoute() doesn't
        // re-reset if a reroute fires immediately on the first GPS tick.
        if (navigationEngine != null) {                           // ← NEW
            navigationEngine.resetForNewRoute();
        }

        // ── Announce first step immediately (NEW) ─────────────────────────────
        // The user should hear "Head north on X" right after starting, rather
        // than waiting until they reach the 200 m threshold of the first turn.
        if (currentRoute.hasSteps()) {                            // ← NEW
            String firstInstruction = NavigationEngine.stripHtml(
                    currentRoute.steps.get(0).htmlInstructions);
            speak(firstInstruction);
        }

        if (currentLatLng != null && googleMap != null) {
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f), 800, null);
        }

        long etaMs = (long) currentRoute.durationMinutes * 60 * 1000;
        Intent serviceIntent = new Intent(this, TripNavigationService.class);
        serviceIntent.setAction(TripNavigationService.ACTION_START);
        serviceIntent.putExtra(TripNavigationService.EXTRA_ETA_MS, etaMs);
        startService(serviceIntent);
        saveRouteToHistory();
    }

    private void stopTrip() {
        String  destination  = currentRoute != null ? currentRoute.destinationLabel : "Unknown";
        double  distanceKm   = currentRoute != null ? currentRoute.distanceKm : 0;
        int     etaMins      = currentRoute != null ? currentRoute.durationMinutes : 0;
        String  trafficLabel = trafficCondition.label;
        String  transport    = selectedTransport.name();
        long    elapsedMs    = System.currentTimeMillis() - tripStartTimeMs;
        int     elapsedMins  = (int)(elapsedMs / 60000);

        AppDatabase.DB_EXECUTOR.execute(() -> {
            FareRate rate = db.fareRateDao().getByType(selectedTransport.name());
            FareResult result = rate != null
                    ? FareCalculator.calculate(distanceKm, rate, trafficCondition)
                    : FareCalculator.estimateOffline(distanceKm, selectedTransport, trafficCondition);
            String fareRange = result.getFormattedRange();
            runOnUiThread(() -> showTripSummaryDialog(
                    destination, distanceKm, etaMins, elapsedMins,
                    fareRange, trafficLabel, transport));
        });

        tripActive  = false;
        isRerouting = false;

        // ── Stop navigation engine (NEW) ──────────────────────────────────────
        if (navigationEngine != null) navigationEngine.stopNavigation(); // ← NEW

        stopService(new Intent(this, TripNavigationService.class));
        speak("Trip ended.");
        fabStartTrip.setText("Start Trip");
        fabStartTrip.setIconResource(R.drawable.ic_navigation);
    }

    private void showTripSummaryDialog(String destination, double distanceKm, int etaMins,
                                       int elapsedMins, String fareRange,
                                       String trafficLabel, String transport) {
        String elapsedText;
        if (elapsedMins >= 60)    elapsedText = (elapsedMins / 60) + "h " + (elapsedMins % 60) + "m";
        else if (elapsedMins > 0) elapsedText = elapsedMins + " min";
        else                      elapsedText = "< 1 min";

        String transportLabel = transport.charAt(0) + transport.substring(1).toLowerCase();

        String message =
                "\uD83C\uDFC1  Destination\n"       + destination + "\n\n" +
                        "\uD83D\uDCCF  Distance\n"           + String.format("%.1f km", distanceKm) + "\n\n" +
                        "\u23F1  Time on trip\n"             + elapsedText + "\n\n" +
                        "\uD83D\uDDFA  Estimated duration\n" + etaMins + " min\n\n" +
                        "\uD83D\uDEA6  Traffic condition\n"  + trafficLabel + "\n\n" +
                        "\uD83D\uDE8C  Transport type\n"     + transportLabel + "\n\n" +
                        "\uD83D\uDCB0  Fare estimate\n"      + fareRange;

        new AlertDialog.Builder(this, R.style.FareGoDialogTheme)
                .setTitle("Trip Summary")
                .setMessage(message)
                .setPositiveButton("Done", (dialog, which) -> {
                    dialog.dismiss();
                    resetMapToCurrentLocation();
                })
                .setCancelable(false)
                .show();
    }

    private void resetMapToCurrentLocation() {
        tvOrigin.setText("My Location");
        etDestination.setText("");
        etDestination.setHint("Where to?");
        cardTripInfo.setVisibility(View.GONE);
        if (routePolyline != null) { routePolyline.remove(); routePolyline = null; }
        if (destMarker    != null) { destMarker.remove();    destMarker    = null; }
        currentRoute = null;
        if (currentLatLng != null && googleMap != null) {
            CameraPosition camPos = new CameraPosition.Builder()
                    .target(currentLatLng).zoom(15f).tilt(0f).build();
            googleMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(camPos), 1000, null);
        }
    }

    private void reroute() {
        if (isRerouting || currentRoute == null || currentLatLng == null) return;
        isRerouting = true;
        currentRoute.originLatLng = currentLatLng;
        fetchRoute(currentRoute, false);
        // processRoute() will call navigationEngine.resetForNewRoute() after
        // the new steps arrive, so we don't need to reset here.
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Database (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    private void saveRouteToHistory() {
        if (currentRoute == null) return;
        AppDatabase.DB_EXECUTOR.execute(() -> {
            FareRate rate = db.fareRateDao().getByType(selectedTransport.name());
            FareResult result = rate != null
                    ? FareCalculator.calculate(currentRoute.distanceKm, rate, trafficCondition)
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

    // ═════════════════════════════════════════════════════════════════════════
    //  TTS (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.ENGLISH);
    }

    private void speak(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Haversine distance (unchanged)
    // ═════════════════════════════════════════════════════════════════════════
    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R    = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a    = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng/2) * Math.sin(dLng/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Lifecycle cleanup  (CHANGED — engine stop added)
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    protected void onDestroy() {
        if (voiceManager   != null) voiceManager.destroy();
        if (voiceDialog    != null && voiceDialog.isShowing()) voiceDialog.dismiss();
        if (tts            != null) { tts.stop(); tts.shutdown(); }
        if (navigationEngine != null) navigationEngine.stopNavigation(); // ← NEW
        if (fusedClient    != null && locationCallback != null)
            fusedClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }
}