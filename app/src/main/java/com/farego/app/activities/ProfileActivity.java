package com.farego.app.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.farego.app.R;
import com.farego.app.db.AppDatabase;
import com.farego.app.db.entity.User;
import com.farego.app.db.entity.UserProfile;
import com.farego.app.utils.ImageUtils;
import com.farego.app.utils.LocationStorageHelper;
import com.farego.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ImageView         ivAvatar;
    private TextInputEditText etName, etEmail, etPhone;
    private Button            btnSave, btnLogout;
    private ProgressBar       pbSaving;
    private MaterialToolbar   toolbar;

    // Stats
    private TextView tvTripCount, tvTotalKm, tvFavTransport;

    // Saved-location labels
    private TextView tvHomeSaved, tvWorkSaved;
    private Button   btnSaveHome, btnSaveWork;

    private SessionManager session;
    private AppDatabase    db;
    private Uri            cameraUri;

    // ── Activity result launchers ──────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) handleImageUri(uri);
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && cameraUri != null) {
                    handleImageUri(cameraUri);
                }
            });

    /** Launcher for SelectLocationActivity — handles both HOME and WORK results */
    private final ActivityResultLauncher<Intent> locationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;

                Intent data    = result.getData();
                String type    = data.getStringExtra(SelectLocationActivity.RESULT_TYPE);
                String address = data.getStringExtra(SelectLocationActivity.RESULT_ADDRESS);
                double lat     = data.getDoubleExtra(SelectLocationActivity.RESULT_LAT, 0);
                double lng     = data.getDoubleExtra(SelectLocationActivity.RESULT_LNG, 0);

                if (address == null) return;

                // Update UI immediately
                if ("HOME".equals(type) && tvHomeSaved != null) tvHomeSaved.setText(address);
                if ("WORK".equals(type) && tvWorkSaved != null) tvWorkSaved.setText(address);

                // Persist into Room UserProfile so it survives reinstalls / wipes
                int userId = session.getUserId();
                AppDatabase.DB_EXECUTOR.execute(() -> {
                    UserProfile profile = db.userProfileDao().getByUserId(userId);
                    if (profile == null) {
                        profile        = new UserProfile();
                        profile.userId = userId;
                        User user = db.userDao().findById(userId);
                        if (user != null) { profile.name = user.username; profile.email = user.email; }
                    }
                    if ("HOME".equals(type)) profile.homeLocation = address;
                    else                      profile.workLocation = address;
                    db.userProfileDao().upsert(profile);
                });
            });

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);
        db      = AppDatabase.getInstance(this);

        bindViews();
        setupToolbar();
        loadProfile();

        ivAvatar.setOnClickListener(v -> showImageSourceDialog());
        btnSave.setOnClickListener(v -> saveProfile());

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                session.clearSession();
                Intent intent = new Intent(this, AuthActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        // ── Saved location buttons: open map picker ────────────────────────────
        if (btnSaveHome != null) {
            btnSaveHome.setOnClickListener(v -> openLocationPicker("HOME"));
        }
        if (btnSaveWork != null) {
            btnSaveWork.setOnClickListener(v -> openLocationPicker("WORK"));
        }
    }

    // ── Open map picker ────────────────────────────────────────────────────────

    private void openLocationPicker(String type) {
        Intent intent = new Intent(this, SelectLocationActivity.class);
        intent.putExtra(SelectLocationActivity.EXTRA_TYPE, type);
        locationLauncher.launch(intent);
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void bindViews() {
        toolbar        = findViewById(R.id.toolbar_profile);
        ivAvatar       = findViewById(R.id.iv_avatar);
        etName         = findViewById(R.id.et_name);
        etEmail        = findViewById(R.id.et_email);
        etPhone        = findViewById(R.id.et_phone);
        btnSave        = findViewById(R.id.btn_save_profile);
        btnLogout      = findViewById(R.id.btn_logout);
        pbSaving       = findViewById(R.id.pb_saving);

        tvTripCount    = findViewById(R.id.tv_trip_count);
        tvTotalKm      = findViewById(R.id.tv_total_km);
        tvFavTransport = findViewById(R.id.tv_fav_transport);

        tvHomeSaved    = findViewById(R.id.tv_home_saved);
        tvWorkSaved    = findViewById(R.id.tv_work_saved);
        btnSaveHome    = findViewById(R.id.btn_save_home);
        btnSaveWork    = findViewById(R.id.btn_save_work);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ── Load profile + stats ───────────────────────────────────────────────────

    private void loadProfile() {
        int userId = session.getUserId();
        if (userId == -1) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load saved locations from SharedPreferences immediately (fast, no DB needed)
        String homeAddr = LocationStorageHelper.getDisplayAddress(this, "HOME");
        String workAddr = LocationStorageHelper.getDisplayAddress(this, "WORK");
        if (tvHomeSaved != null) tvHomeSaved.setText(homeAddr);
        if (tvWorkSaved != null) tvWorkSaved.setText(workAddr);

        AppDatabase.DB_EXECUTOR.execute(() -> {
            User        user        = db.userDao().findById(userId);
            UserProfile profile     = db.userProfileDao().getByUserId(userId);
            int         tripCount   = db.routeHistoryDao().countForUser(userId);
            double      totalKm     = db.routeHistoryDao().totalKmForUser(userId);
            String      favTransport = db.routeHistoryDao().favTransportForUser(userId);

            runOnUiThread(() -> {
                if (user == null) {
                    Toast.makeText(this, "Session expired. Please log in again.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Stats
                if (tvTripCount    != null) tvTripCount.setText(String.valueOf(tripCount));
                if (tvTotalKm      != null) tvTotalKm.setText(String.format("%.1f km", totalKm));
                if (tvFavTransport != null)
                    tvFavTransport.setText((favTransport != null && !favTransport.isEmpty())
                            ? favTransport : "—");

                // Profile fields
                if (profile != null) {
                    setFieldText(etName,  profile.name);
                    setFieldText(etEmail, profile.email != null ? profile.email : user.email);
                    setFieldText(etPhone, profile.phone);
                    loadAvatarFromPath(profile.avatarPath);

                    // Saved locations — prefer SharedPreferences (has lat/lng too), fall back to DB
                    String hAddr = LocationStorageHelper.getDisplayAddress(this, "HOME");
                    String wAddr = LocationStorageHelper.getDisplayAddress(this, "WORK");
                    // If SP is "Not set" but DB has data, use DB value
                    if ("Not set".equals(hAddr) && profile.homeLocation != null)
                        hAddr = profile.homeLocation;
                    if ("Not set".equals(wAddr) && profile.workLocation != null)
                        wAddr = profile.workLocation;

                    if (tvHomeSaved != null) tvHomeSaved.setText(hAddr);
                    if (tvWorkSaved != null) tvWorkSaved.setText(wAddr);
                } else {
                    setFieldText(etName,  user.username);
                    setFieldText(etEmail, user.email);
                    setFieldText(etPhone, "");
                    ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                }

                TextView tvUsername    = findViewById(R.id.tv_profile_username);
                TextView tvHeaderEmail = findViewById(R.id.tv_profile_email);
                if (tvUsername != null)
                    tvUsername.setText(profile != null && profile.name != null
                            ? profile.name : user.username);
                if (tvHeaderEmail != null)
                    tvHeaderEmail.setText(profile != null && profile.email != null
                            ? profile.email : user.email);
            });
        });
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void setFieldText(TextInputEditText field, String value) {
        if (field != null && value != null) field.setText(value);
    }

    // ── Image picker ───────────────────────────────────────────────────────────

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Change Profile Photo")
                .setItems(new String[]{"Choose from Gallery", "Take a Photo", "Remove Photo"},
                        (d, which) -> {
                            if      (which == 0) openGallery();
                            else if (which == 1) openCamera();
                            else                 removeAvatar();
                        })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        try {
            File imageFile = ImageUtils.createTempImageFile(this);
            cameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", imageFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(this, "Cannot open camera: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handleImageUri(Uri uri) {
        pbSaving.setVisibility(View.VISIBLE);
        AppDatabase.DB_EXECUTOR.execute(() -> {
            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                try (InputStream probe = getContentResolver().openInputStream(uri)) {
                    BitmapFactory.decodeStream(probe, null, opts);
                }
                opts.inSampleSize       = ImageUtils.calculateInSampleSize(opts, 512, 512);
                opts.inJustDecodeBounds = false;
                Bitmap bitmap;
                try (InputStream stream = getContentResolver().openInputStream(uri)) {
                    bitmap = BitmapFactory.decodeStream(stream, null, opts);
                }
                if (bitmap == null) throw new IOException("Failed to decode image");

                String savedPath = ImageUtils.saveBitmapToPrivateStorage(this, bitmap);

                UserProfile profile = db.userProfileDao().getByUserId(session.getUserId());
                if (profile == null) {
                    profile        = new UserProfile();
                    profile.userId = session.getUserId();
                    User user = db.userDao().findById(session.getUserId());
                    if (user != null) { profile.name = user.username; profile.email = user.email; }
                }
                profile.avatarPath = savedPath;
                db.userProfileDao().upsert(profile);

                String finalPath = savedPath;
                runOnUiThread(() -> {
                    pbSaving.setVisibility(View.GONE);
                    loadAvatarFromPath(finalPath);
                    Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pbSaving.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not save image. Please try again.",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void removeAvatar() {
        AppDatabase.DB_EXECUTOR.execute(() -> {
            UserProfile profile = db.userProfileDao().getByUserId(session.getUserId());
            if (profile != null) {
                if (profile.avatarPath != null) new File(profile.avatarPath).delete();
                profile.avatarPath = null;
                db.userProfileDao().upsert(profile);
            }
            runOnUiThread(() -> {
                ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                Toast.makeText(this, "Profile picture removed.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void loadAvatarFromPath(@Nullable String path) {
        if (path != null && new File(path).exists()) {
            Glide.with(this).load(new File(path)).transform(new CircleCrop())
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder).into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }
    }

    // ── Save profile ───────────────────────────────────────────────────────────

    private void saveProfile() {
        String name  = etName  != null && etName.getText()  != null
                ? etName.getText().toString().trim()  : "";
        String email = etEmail != null && etEmail.getText() != null
                ? etEmail.getText().toString().trim() : "";
        String phone = etPhone != null && etPhone.getText() != null
                ? etPhone.getText().toString().trim() : "";

        if (name.isEmpty()) {
            if (etName != null) etName.setError("Name is required");
            return;
        }

        pbSaving.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        AppDatabase.DB_EXECUTOR.execute(() -> {
            int userId = session.getUserId();
            UserProfile profile = db.userProfileDao().getByUserId(userId);
            if (profile == null) {
                profile        = new UserProfile();
                profile.userId = userId;
            }
            profile.name  = name;
            profile.email = email;
            profile.phone = phone;
            // avatarPath, homeLocation, workLocation NOT touched here
            db.userProfileDao().upsert(profile);

            User user = db.userDao().findById(userId);
            if (user != null && !email.isEmpty()) {
                user.email = email;
                db.userDao().updateUser(user);
            }

            runOnUiThread(() -> {
                pbSaving.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}