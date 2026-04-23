package com.farego.app.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.farego.app.R;
import com.farego.app.utils.LocationStorageHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelectLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ── Intent keys ──────────────────────────────────────────────────────────
    public static final String EXTRA_TYPE    = "location_type";   // "HOME" or "WORK"
    public static final String RESULT_ADDRESS = "result_address";
    public static final String RESULT_LAT     = "result_lat";
    public static final String RESULT_LNG     = "result_lng";
    public static final String RESULT_TYPE    = "result_type";

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    // Default map centre: Accra, Ghana
    private static final LatLng DEFAULT_LATLNG = new LatLng(5.6037, -0.1870);

    // ── Views ─────────────────────────────────────────────────────────────────
    private EditText etSearch;
    private Button   btnSaveLocation;
    private TextView tvSelectedAddress;
    private ProgressBar pbGeocoding;

    // ── Map / location state ──────────────────────────────────────────────────
    private GoogleMap         map;
    private Marker            selectedMarker;
    private LatLng            selectedLatLng;
    private String            selectedAddress;
    private String            locationType;

    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService       executor = Executors.newSingleThreadExecutor();
    private final Handler               mainHandler = new Handler(Looper.getMainLooper());

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);

        locationType = getIntent().getStringExtra(EXTRA_TYPE);
        if (locationType == null) locationType = "HOME";

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        bindViews();
        setupToolbar();
        setupMap();
        setupSearch();

        btnSaveLocation.setOnClickListener(v -> confirmAndSave());
        btnSaveLocation.setEnabled(false); // disabled until user picks a location
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews() {
        etSearch          = findViewById(R.id.et_location_search);
        btnSaveLocation   = findViewById(R.id.btn_save_location);
        tvSelectedAddress = findViewById(R.id.tv_selected_address);
        pbGeocoding       = findViewById(R.id.pb_geocoding);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_select_location);
        setSupportActionBar(toolbar);
        String title = "HOME".equals(locationType) ? "Set Home Location" : "Set Work Location";
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ── Google Maps ───────────────────────────────────────────────────────────

    private void setupMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);

        // Move to default location first, then try current
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LATLNG, 12f));

        // Pre-fill existing saved location if any
        prefillSavedLocation();

        // Tap to select
        map.setOnMapClickListener(this::onMapTap);

        // Try to show current location
        enableCurrentLocation();
    }

    private void prefillSavedLocation() {
        executor.execute(() -> {
            LocationStorageHelper.SavedLocation saved =
                    LocationStorageHelper.getLocation(this, locationType);
            if (saved != null) {
                mainHandler.post(() -> {
                    LatLng latlng = new LatLng(saved.lat, saved.lng);
                    placeMarker(latlng, saved.address);
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15f));
                });
            }
        });
    }

    private void enableCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            moveToCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    private void moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && selectedMarker == null) {
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 15f));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableCurrentLocation();
        }
    }

    private void onMapTap(LatLng latlng) {
        placeMarker(latlng, null);
        reverseGeocode(latlng);
    }

    private void placeMarker(LatLng latlng, String address) {
        if (selectedMarker != null) selectedMarker.remove();

        String snippet = address != null ? address : "Fetching address…";
        selectedMarker = map.addMarker(
                new MarkerOptions()
                        .position(latlng)
                        .title(locationType.equals("HOME") ? "Home" : "Work")
                        .snippet(snippet)
        );
        if (selectedMarker != null) selectedMarker.showInfoWindow();

        selectedLatLng    = latlng;
        selectedAddress   = address;
        btnSaveLocation.setEnabled(address != null);

        if (address != null) {
            tvSelectedAddress.setText(address);
            tvSelectedAddress.setVisibility(View.VISIBLE);
        }
    }

    // ── Geocoding ─────────────────────────────────────────────────────────────

    private void reverseGeocode(LatLng latlng) {
        pbGeocoding.setVisibility(View.VISIBLE);
        tvSelectedAddress.setText("Fetching address…");
        tvSelectedAddress.setVisibility(View.VISIBLE);
        btnSaveLocation.setEnabled(false);

        executor.execute(() -> {
            String result = null;
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses =
                        geocoder.getFromLocation(latlng.latitude, latlng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    // Build a readable address
                    StringBuilder sb = new StringBuilder();
                    if (addr.getSubLocality()  != null) sb.append(addr.getSubLocality()).append(", ");
                    if (addr.getLocality()     != null) sb.append(addr.getLocality()).append(", ");
                    if (addr.getAdminArea()    != null) sb.append(addr.getAdminArea());
                    result = sb.toString().replaceAll(", $", "").trim();
                    if (result.isEmpty()) result = addr.getAddressLine(0);
                }
            } catch (IOException e) {
                // network / geocoder unavailable
            }

            String finalResult = (result != null && !result.isEmpty())
                    ? result
                    : String.format(Locale.getDefault(), "%.5f, %.5f",
                    latlng.latitude, latlng.longitude);

            mainHandler.post(() -> {
                pbGeocoding.setVisibility(View.GONE);
                selectedAddress = finalResult;
                tvSelectedAddress.setText(finalResult);
                btnSaveLocation.setEnabled(true);

                if (selectedMarker != null) {
                    selectedMarker.setSnippet(finalResult);
                    selectedMarker.showInfoWindow();
                }
            });
        });
    }

    // ── Search bar ────────────────────────────────────────────────────────────

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            private final Handler h = new Handler(Looper.getMainLooper());
            private Runnable pending;

            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (pending != null) h.removeCallbacks(pending);
            }
            @Override public void afterTextChanged(Editable s) {
                pending = () -> geocodeSearch(s.toString().trim());
                h.postDelayed(pending, 600); // debounce 600 ms
            }
        });
    }

    private void geocodeSearch(String query) {
        if (query.isEmpty()) return;
        pbGeocoding.setVisibility(View.VISIBLE);
        btnSaveLocation.setEnabled(false);

        executor.execute(() -> {
            LatLng result   = null;
            String address  = null;
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    result  = new LatLng(addr.getLatitude(), addr.getLongitude());
                    address = addr.getAddressLine(0);
                }
            } catch (IOException e) {
                // silently fail
            }

            LatLng   finalLatLng  = result;
            String   finalAddress = address;
            mainHandler.post(() -> {
                pbGeocoding.setVisibility(View.GONE);
                if (finalLatLng != null) {
                    placeMarker(finalLatLng, finalAddress);
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(finalLatLng, 15f));
                } else {
                    Toast.makeText(this, "Location not found. Try a different search.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ── Save & return result ──────────────────────────────────────────────────

    private void confirmAndSave() {
        if (selectedLatLng == null || selectedAddress == null) {
            Toast.makeText(this, "Please select a location on the map first.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Persist using helper
        executor.execute(() -> {
            LocationStorageHelper.saveLocation(
                    this, locationType,
                    selectedLatLng.latitude, selectedLatLng.longitude, selectedAddress);

            // Also update Room DB (UserProfile.homeLocation / workLocation)
            // via the helper so ProfileActivity doesn't need extra logic
            mainHandler.post(() -> {
                Intent result = new Intent();
                result.putExtra(RESULT_TYPE,    locationType);
                result.putExtra(RESULT_ADDRESS, selectedAddress);
                result.putExtra(RESULT_LAT,     selectedLatLng.latitude);
                result.putExtra(RESULT_LNG,     selectedLatLng.longitude);
                setResult(Activity.RESULT_OK, result);

                Toast.makeText(this,
                        ("HOME".equals(locationType) ? "Home" : "Work") + " location saved!",
                        Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}