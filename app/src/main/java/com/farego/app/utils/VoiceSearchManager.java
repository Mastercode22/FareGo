package com.farego.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * VoiceSearchManager — Handles voice-to-destination for FareGo.
 *
 * Flow:
 *   1. Caller invokes startListening().
 *   2. Manager shows an animated listening dialog.
 *   3. On speech result → cleans phrase → extracts destination text.
 *   4. Reverse-geocodes destination text → LatLng via Geocoder.
 *   5. Reports back through VoiceSearchCallback.
 *
 * Permissions:
 *   RECORD_AUDIO must be declared in AndroidManifest.xml.
 *   The caller is responsible for requesting it at runtime using
 *   REQUEST_CODE_AUDIO_PERM = 300; call onPermissionResult() to forward results.
 */
public class VoiceSearchManager {

    public static final int REQUEST_CODE_AUDIO_PERM = 300;

    // ── Callback interface ────────────────────────────────────────────────────
    public interface VoiceSearchCallback {
        /** Raw transcription before any processing, for live display. */
        void onPartialResult(String partial);

        /**
         * Called when a destination has been fully resolved.
         *
         * @param rawText      the cleaned destination phrase (e.g. "Kasoa Overhead")
         * @param latLng       geocoded coordinates, or null if geocoding failed
         * @param ttsAnnounce  a human-friendly string to read aloud (e.g. "Routing to Kasoa")
         */
        void onDestinationResolved(String rawText, LatLng latLng, String ttsAnnounce);

        /** Called when listening starts — show your animated mic. */
        void onListeningStarted();

        /** Called when listening ends (before result). */
        void onListeningEnded();

        /** Error description for display / Toast. */
        void onError(String message);
    }

    // ── Filler / command phrases to strip from raw transcription ─────────────
    private static final List<String> STRIP_PREFIXES = Arrays.asList(
            "take me to",
            "navigate to",
            "navigate home",
            "go to",
            "route to",
            "directions to",
            "get me to",
            "drive to",
            "find",
            "show me",
            "i want to go to",
            "i need to get to",
            "how do i get to",
            "how to get to",
            "where is",
            "locate",
            "search for",
            "nearest"
    );

    // Special "home" command — caller can handle however it wishes
    private static final String HOME_ALIAS = "__home__";

    private final Context             context;
    private final VoiceSearchCallback callback;
    private       SpeechRecognizer    speechRecognizer;
    private       boolean             isListening = false;

    // Own background thread — no dependency on AppDatabase or any DB class
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    public VoiceSearchManager(Context context, VoiceSearchCallback callback) {
        this.context  = context.getApplicationContext();
        this.callback = callback;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns true if RECORD_AUDIO has been granted. */
    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Request RECORD_AUDIO from the given Activity. */
    public void requestPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_CODE_AUDIO_PERM);
    }

    /**
     * Forward Activity.onRequestPermissionsResult here.
     * Returns true if permission was just granted.
     */
    public boolean onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_AUDIO_PERM) {
            return grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    /**
     * Start voice listening. If permission is missing, requests it first.
     * Caller must call startListening() again after the permission callback.
     */
    public void startListening(Activity activity) {
        if (!hasPermission()) {
            requestPermission(activity);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onError("Speech recognition is not available on this device.");
            return;
        }
        if (isListening) {
            stopListening();
        }
        beginRecognition();
    }

    /** Cancel any ongoing recognition cleanly. */
    public void stopListening() {
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        callback.onListeningEnded();
    }

    /** Release resources — call from onDestroy(). */
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        geocodeExecutor.shutdownNow();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void beginRecognition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override public void onReadyForSpeech(Bundle params) {
                isListening = true;
                callback.onListeningStarted();
            }

            @Override public void onBeginningOfSpeech() { /* user started speaking */ }

            @Override public void onRmsChanged(float rmsdB) { /* can drive a VU-meter if desired */ }

            @Override public void onBufferReceived(byte[] buffer) { }

            @Override public void onEndOfSpeech() {
                isListening = false;
                callback.onListeningEnded();
            }

            @Override
            public void onError(int error) {
                isListening = false;
                callback.onListeningEnded();
                callback.onError(speechErrorMessage(error));
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty()) {
                    callback.onError("No speech was detected. Please try again.");
                    return;
                }
                processSpeechResult(matches.get(0));
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial =
                        partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && !partial.isEmpty()) {
                    callback.onPartialResult(partial.get(0));
                }
            }

            @Override public void onEvent(int eventType, Bundle params) { }
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-GH");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a destination…");
        speechRecognizer.startListening(intent);
    }

    // ── Speech processing ─────────────────────────────────────────────────────

    /**
     * Main processing pipeline:
     *   rawInput → normalize → handle special commands → extractLocation
     *             → geocode → report via callback
     */
    private void processSpeechResult(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            callback.onError("Couldn't understand that. Please try again.");
            return;
        }

        String normalized = rawInput.toLowerCase(Locale.ENGLISH).trim();

        // ── Special command: "navigate home" ──────────────────────────────────
        if (normalized.contains("navigate home") || normalized.equals("home")) {
            callback.onDestinationResolved("Home", null, "Routing to home");
            return;
        }

        // ── Special command: "cheapest route" / "show cheapest" ───────────────
        if (normalized.contains("cheapest") || normalized.contains("cheapest route")) {
            callback.onError("Cheapest route mode selected — pick a destination first.");
            return;
        }

        String destination = extractLocation(normalized);
        if (destination.isEmpty()) {
            callback.onError("Couldn't find a destination in: " + rawInput);
            return;
        }

        // Title-case the result for display
        String displayName = toTitleCase(destination);
        String ttsMessage  = "Routing to " + displayName;

        // Geocode off the main thread using our own executor (no DB dependency)
        geocodeExecutor.execute(() -> {
            LatLng latLng = getCoordinatesFromText(displayName);
            // Report back — always report even if latLng is null; caller decides fallback
            callback.onDestinationResolved(displayName, latLng, ttsMessage);
        });
    }

    /**
     * Strips common navigation filler phrases and returns the bare destination.
     * Returns empty string if nothing remains.
     */
    public String extractLocation(String normalizedInput) {
        String text = normalizedInput.trim();

        // Strip leading filler phrases (longest first to avoid partial matches)
        List<String> sortedPrefixes = new ArrayList<>(STRIP_PREFIXES);
        sortedPrefixes.sort((a, b) -> b.length() - a.length());

        for (String prefix : sortedPrefixes) {
            if (text.startsWith(prefix)) {
                text = text.substring(prefix.length()).trim();
                break; // strip at most one prefix
            }
        }

        // Strip trailing noise like "please", "now", "quickly"
        text = text.replaceAll("\\b(please|now|quickly|fast|asap)\\b", "").trim();
        // Collapse multiple spaces
        text = text.replaceAll("\\s{2,}", " ").trim();

        return text;
    }

    /**
     * Geocodes a free-text place name to LatLng using Android Geocoder.
     * Biases results to Ghana (bounding box). Returns null if not found.
     *
     * Called off the main thread — safe to block here.
     */
    public LatLng getCoordinatesFromText(String locationName) {
        if (!Geocoder.isPresent()) return null;

        try {
            Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);

            // Ghana bounding box for bias
            List<Address> results = geocoder.getFromLocationName(
                    locationName + ", Ghana",
                    3,
                    4.5,   // lowerLeftLat
                    -3.5,  // lowerLeftLng
                    11.5,  // upperRightLat
                    1.5    // upperRightLng
            );

            if (results != null && !results.isEmpty()) {
                Address best = results.get(0);
                return new LatLng(best.getLatitude(), best.getLongitude());
            }

            // If Ghana-biased search fails, try unbiased
            List<Address> global = geocoder.getFromLocationName(locationName, 1);
            if (global != null && !global.isEmpty()) {
                return new LatLng(global.get(0).getLatitude(), global.get(0).getLongitude());
            }

        } catch (IOException e) {
            // Network error — caller will show a toast
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1).toLowerCase(Locale.ENGLISH));
            }
        }
        return sb.toString();
    }

    private String speechErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error. Check microphone.";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error. Please try again.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Microphone permission required.";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error. Check your connection.";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout. Please try again.";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match found. Try speaking more clearly.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service is busy. Please wait.";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error. Please try again later.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech detected. Tap the mic and speak.";
            default:
                return "Voice recognition failed. Please try again.";
        }
    }
}