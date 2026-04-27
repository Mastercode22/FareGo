package com.farego.app.navigation;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.util.Log;

import com.farego.app.model.RouteInfo;
import com.farego.app.network.model.DirectionsResponse;
import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

/**
 * ════════════════════════════════════════════════════════════════════════════
 *  NavigationEngine.java
 *
 *  Full turn-by-turn navigation engine for FareGo.
 *  Attach to your existing GPS LocationCallback and call handleNavigationUpdates()
 *  on every location fix.
 *
 *  Responsibilities:
 *    1. Track which step the user is currently on
 *    2. Measure distance from user to the step's end point (manoeuvre point)
 *    3. Speak voice instructions at the right distance thresholds
 *    4. Detect arrival at the destination
 *    5. Offer periodic "continue straight" reminders
 *    6. Detect off-route conditions and request rerouting
 *    7. Vibrate on imminent turns
 *
 *  Usage in MainActivity:
 *    // 1 – Create once, keep as a field
 *    navigationEngine = new NavigationEngine(this, tts, rerouteListener);
 *
 *    // 2 – Reset whenever a new route is loaded
 *    navigationEngine.resetForNewRoute();
 *
 *    // 3 – Feed every GPS location update while trip is active
 *    locationCallback = new LocationCallback() {
 *        public void onLocationResult(LocationResult result) {
 *            ...
 *            if (tripActive && currentRoute != null) {
 *                navigationEngine.handleNavigationUpdates(currentLatLng, currentRoute);
 *            }
 *        }
 *    };
 *
 *    // 4 – Call stopNavigation() when trip ends
 *    navigationEngine.stopNavigation();
 * ════════════════════════════════════════════════════════════════════════════
 */
public class NavigationEngine {

    private static final String TAG = "NavigationEngine";

    // ── Distance thresholds (metres) ──────────────────────────────────────────

    /** Distance at which the "prepare to turn" announcement fires. */
    private static final float THRESHOLD_PREPARE_M   = 200f;

    /** Distance at which the "turn now" announcement fires. */
    private static final float THRESHOLD_TURN_NOW_M  = 30f;

    /** Distance within which the user is considered to have arrived. */
    private static final float THRESHOLD_ARRIVED_M   = 30f;

    /**
     * If the user strays this far from any polyline point on the current step,
     * we raise an off-route event so MainActivity can call reroute().
     */
    private static final float THRESHOLD_OFF_ROUTE_M = 60f;

    /**
     * Minimum time between "continue straight" reminders (ms).
     * Prevents spamming when the user is on a long straight road.
     */
    private static final long CONTINUE_REMINDER_INTERVAL_MS = 45_000L; // 45 seconds

    // ── State ─────────────────────────────────────────────────────────────────

    /** Index of the step we are currently navigating towards. */
    private int  currentStepIndex   = 0;

    /** Prevents the "prepare" announcement from firing more than once per step. */
    private boolean hasAnnouncedPrepare = false;

    /** Prevents the "turn now" announcement from firing more than once per step. */
    private boolean hasAnnouncedTurnNow = false;

    /** Prevents the arrival announcement from repeating. */
    private boolean hasAnnouncedArrival = false;

    /** Timestamp of the last "continue straight" announcement. */
    private long lastContinueStraightMs = 0L;

    /** Whether navigation is currently active (between start and stop). */
    private boolean isNavigating = false;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final Context        context;
    private final TextToSpeech   tts;
    private final RerouteListener rerouteListener;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param context         Application or Activity context (for Vibrator).
     * @param tts             The already-initialised TextToSpeech instance from MainActivity.
     * @param rerouteListener Callback fired when the user appears to be off-route.
     */
    public NavigationEngine(Context context,
                            TextToSpeech tts,
                            RerouteListener rerouteListener) {
        this.context         = context;
        this.tts             = tts;
        this.rerouteListener = rerouteListener;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Public API
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Call this every time a new GPS location arrives AND the trip is active.
     *
     * This is the main engine loop. It:
     *   1. Measures distance to current step end point
     *   2. Fires voice at 200 m ("prepare") and 30 m ("turn now")
     *   3. Advances to the next step when the user passes the manoeuvre point
     *   4. Checks for arrival
     *   5. Issues occasional "continue straight" reminders
     *   6. Detects off-route and calls rerouteListener
     *
     * @param userLocation Current GPS position
     * @param route        The active RouteInfo (must have steps populated)
     */
    public void handleNavigationUpdates(LatLng userLocation, RouteInfo route) {
        // Guard: do nothing if navigation is not active or route has no steps
        if (!isNavigating || route == null || !route.hasSteps()) return;
        if (hasAnnouncedArrival) return;  // already finished

        // Safety: clamp index to valid range
        if (currentStepIndex >= route.stepCount()) {
            currentStepIndex = route.stepCount() - 1;
        }

        DirectionsResponse.Step currentStep = route.steps.get(currentStepIndex);
        boolean isLastStep = (currentStepIndex == route.stepCount() - 1);

        // ── 1. Distance to the end of the current step (manoeuvre point) ──────
        float[] distResult = new float[1];
        Location.distanceBetween(
                userLocation.latitude,  userLocation.longitude,
                currentStep.endLocation.lat, currentStep.endLocation.lng,
                distResult);
        float distToStepEnd = distResult[0];

        // Update RouteInfo's live state so the UI can reflect it
        route.distanceRemainingKm = distToStepEnd / 1000.0;

        // ── 2. Arrival detection (check last step only) ───────────────────────
        if (isLastStep && distToStepEnd <= THRESHOLD_ARRIVED_M) {
            announceArrival(route.destinationLabel);
            return;
        }

        // ── 3. Voice at 200 m — "In 200 metres, turn right onto X" ───────────
        if (!hasAnnouncedPrepare && distToStepEnd <= THRESHOLD_PREPARE_M) {
            hasAnnouncedPrepare = true;
            String instruction = stripHtml(currentStep.htmlInstructions);
            String prepareText = buildPrepareAnnouncement(instruction, distToStepEnd);
            speak(prepareText);
            Log.d(TAG, "Prepare announced: " + prepareText);
        }

        // ── 4. Voice at 30 m — "Now turn right" ──────────────────────────────
        if (!hasAnnouncedTurnNow && distToStepEnd <= THRESHOLD_TURN_NOW_M) {
            hasAnnouncedTurnNow = true;
            String instruction = stripHtml(currentStep.htmlInstructions);
            String nowText     = buildTurnNowAnnouncement(instruction, currentStep.maneuver);
            speak(nowText);
            vibrateForTurn();   // tactile feedback
            Log.d(TAG, "Turn-now announced: " + nowText);
        }

        // ── 5. Advance to next step once the user passes the manoeuvre point ──
        if (distToStepEnd <= THRESHOLD_TURN_NOW_M && !isLastStep) {
            advanceToNextStep(route);
            return; // re-evaluate on next GPS tick with fresh step data
        }

        // ── 6. "Continue straight" reminder when far from a turn ─────────────
        if (distToStepEnd > THRESHOLD_PREPARE_M) {
            issueContinueStraightReminder(route, currentStep);
        }

        // ── 7. Off-route detection ────────────────────────────────────────────
        checkOffRoute(userLocation, currentStep);
    }

    /**
     * Resets all step-tracking state.
     * Call this whenever a new route is loaded or a reroute completes.
     */
    public void resetForNewRoute() {
        currentStepIndex     = 0;
        hasAnnouncedPrepare  = false;
        hasAnnouncedTurnNow  = false;
        hasAnnouncedArrival  = false;
        lastContinueStraightMs = 0L;
        isNavigating         = true;
        Log.d(TAG, "Navigation reset for new route.");
    }

    /**
     * Stops navigation and clears state.
     * Call when the user taps "End Trip".
     */
    public void stopNavigation() {
        isNavigating = false;
        Log.d(TAG, "Navigation stopped.");
    }

    /** Returns true if navigation is currently active. */
    public boolean isNavigating() {
        return isNavigating;
    }

    /** Returns the index of the step currently being navigated. */
    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Private — step management
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Moves the engine forward to the next step and resets per-step flags.
     * Also reads out the instruction for the new step immediately.
     */
    private void advanceToNextStep(RouteInfo route) {
        currentStepIndex++;
        hasAnnouncedPrepare  = false;
        hasAnnouncedTurnNow  = false;

        if (currentStepIndex >= route.stepCount()) return;

        DirectionsResponse.Step nextStep = route.steps.get(currentStepIndex);
        String nextInstruction = stripHtml(nextStep.htmlInstructions);

        // Announce the next instruction immediately after the turn
        speak(nextInstruction);
        Log.d(TAG, "Advanced to step " + currentStepIndex + ": " + nextInstruction);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Private — announcement builders
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Builds the "prepare" announcement fired at ~200 m.
     *
     * Examples:
     *   "In 200 metres, turn right onto Ring Road East"
     *   "In 180 metres, keep left at the roundabout"
     *
     * @param instruction Plain-text instruction (HTML already stripped)
     * @param distMetres  Actual measured distance in metres
     */
    private String buildPrepareAnnouncement(String instruction, float distMetres) {
        // Round distance to nearest 50 m for natural speech
        int rounded = (int)(Math.round(distMetres / 50.0) * 50);
        if (rounded < 50) rounded = 50;
        return String.format(Locale.ENGLISH, "In %d metres, %s", rounded, instruction);
    }

    /**
     * Builds the "turn now" announcement fired at ~30 m.
     * Uses the maneuver field for a crisp, natural prompt when available.
     *
     * Examples:
     *   "Now turn right"
     *   "Now keep left"
     *   "Turn right onto Ring Road East"   ← fallback when maneuver is null
     */
    private String buildTurnNowAnnouncement(String instruction, String maneuver) {
        if (maneuver != null) {
            TurnDirection dir = detectTurnDirection(maneuver);
            switch (dir) {
                case LEFT:       return "Now turn left";
                case RIGHT:      return "Now turn right";
                case STRAIGHT:   return "Continue straight";
                case ROUNDABOUT: return "Take the roundabout";
                case U_TURN:     return "Make a U-turn";
                case MERGE:      return "Merge onto the road";
                case RAMP:       return "Take the ramp";
                case FERRY:      return "Take the ferry";
                default:         break;
            }
        }
        // Fallback to full instruction text
        return instruction;
    }

    /**
     * Builds a "continue straight" reminder for long straight sections.
     *
     * Example: "Continue straight for 1.2 kilometres"
     */
    private String buildContinueStraightText(DirectionsResponse.Step step) {
        if (step.distance != null) {
            double km = step.distance.value / 1000.0;
            if (km >= 1.0) {
                return String.format(Locale.ENGLISH, "Continue straight for %.1f kilometres", km);
            } else {
                return String.format(Locale.ENGLISH,
                        "Continue straight for %d metres", step.distance.value);
            }
        }
        return "Continue straight";
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Private — reminder / arrival / off-route
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Speaks a "continue straight" reminder if enough time has passed since
     * the last one. Prevents the TTS from being too chatty on motorways.
     */
    private void issueContinueStraightReminder(RouteInfo route,
                                               DirectionsResponse.Step currentStep) {
        long now = System.currentTimeMillis();
        if (now - lastContinueStraightMs < CONTINUE_REMINDER_INTERVAL_MS) return;
        lastContinueStraightMs = now;
        speak(buildContinueStraightText(currentStep));
    }

    /**
     * Fires the arrival announcement and stops navigation.
     *
     * @param destinationLabel Human-readable destination name (e.g. "Accra Mall")
     */
    private void announceArrival(String destinationLabel) {
        if (hasAnnouncedArrival) return;
        hasAnnouncedArrival = true;
        isNavigating        = false;

        String dest = (destinationLabel != null && !destinationLabel.isEmpty())
                ? destinationLabel : "your destination";
        speak("You have arrived at " + dest);
        Log.d(TAG, "Arrival announced.");
    }

    /**
     * Measures the user's distance to the start and end of the current step.
     * If both are > THRESHOLD_OFF_ROUTE_M, the user may have gone off-route
     * and we notify the listener so MainActivity can trigger a reroute.
     */
    private void checkOffRoute(LatLng userLocation, DirectionsResponse.Step step) {
        if (rerouteListener == null) return;

        float[] toEnd   = new float[1];
        float[] toStart = new float[1];

        Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                step.endLocation.lat,  step.endLocation.lng,   toEnd);
        Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                step.startLocation.lat, step.startLocation.lng, toStart);

        if (toEnd[0] > THRESHOLD_OFF_ROUTE_M && toStart[0] > THRESHOLD_OFF_ROUTE_M) {
            Log.d(TAG, "Off-route detected. Notifying listener.");
            rerouteListener.onOffRoute();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Public utilities (accessible from MainActivity / UI layer)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Strips all HTML tags from a Directions API instruction string.
     * Uses Html.fromHtml() which handles &amp;, &lt;, <b>, <wbr> etc.
     *
     * Example input:  "Turn <b>right</b> onto <b>Ring Rd E</b>"
     * Example output: "Turn right onto Ring Rd E"
     *
     * @param html Raw htmlInstructions from the API
     * @return Clean plain text suitable for TTS
     */
    @SuppressWarnings("deprecation")
    public static String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        String stripped;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stripped = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            stripped = Html.fromHtml(html).toString();
        }
        // Collapse multiple spaces/newlines that Html.fromHtml sometimes introduces
        return stripped.replaceAll("\\s+", " ").trim();
    }

    /**
     * Converts a Google Directions maneuver string into a TurnDirection enum.
     * This lets us produce crisp voice prompts ("turn left") instead of
     * reading the full instruction text at every turn.
     *
     * @param maneuver Raw maneuver string from the API (may be null)
     * @return Corresponding TurnDirection, or UNKNOWN if unrecognised
     */
    public static TurnDirection detectTurnDirection(String maneuver) {
        if (maneuver == null) return TurnDirection.UNKNOWN;
        String m = maneuver.toLowerCase(Locale.ENGLISH);

        if (m.contains("turn-left")   || m.equals("left"))        return TurnDirection.LEFT;
        if (m.contains("turn-right")  || m.equals("right"))       return TurnDirection.RIGHT;
        if (m.contains("straight")    || m.equals("continue"))    return TurnDirection.STRAIGHT;
        if (m.contains("roundabout"))                              return TurnDirection.ROUNDABOUT;
        if (m.contains("uturn")       || m.contains("u-turn"))    return TurnDirection.U_TURN;
        if (m.contains("merge"))                                   return TurnDirection.MERGE;
        if (m.contains("ramp"))                                    return TurnDirection.RAMP;
        if (m.contains("ferry"))                                   return TurnDirection.FERRY;
        if (m.contains("keep-left")   || m.contains("fork-left")) return TurnDirection.LEFT;
        if (m.contains("keep-right")  || m.contains("fork-right"))return TurnDirection.RIGHT;

        return TurnDirection.UNKNOWN;
    }

    /**
     * Converts a plain-text instruction into the most natural TTS-friendly
     * version. Handles edge cases like "<wbr/>" artefacts and abbreviations.
     *
     * @param rawInstruction Plain text (after stripHtml)
     * @return Cleaned speech-friendly string
     */
    public static String toNaturalSpeech(String rawInstruction) {
        if (rawInstruction == null) return "";
        return rawInstruction
                .replace("&amp;",  "and")
                .replace("&nbsp;", " ")
                // Expand common road abbreviations for clearer TTS pronunciation
                .replace(" Rd ",   " Road ")
                .replace(" Rd.",   " Road")
                .replace(" St ",   " Street ")
                .replace(" St.",   " Street")
                .replace(" Ave ",  " Avenue ")
                .replace(" Ave.",  " Avenue")
                .replace(" Blvd ", " Boulevard ")
                .replace(" Dr ",   " Drive ")
                .replace(" Dr.",   " Drive")
                .replace(" Ln ",   " Lane ")
                .replace(" N ",    " North ")
                .replace(" S ",    " South ")
                .replace(" E ",    " East ")
                .replace(" W ",    " West ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Private — TTS + Vibration helpers
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Speaks text via TTS.
     * Uses QUEUE_ADD so navigation instructions don't cut each other off
     * (unlike QUEUE_FLUSH which would interrupt a current utterance mid-word).
     */
    private void speak(String text) {
        if (tts == null || text == null || text.isEmpty()) return;
        String clean = toNaturalSpeech(text);
        tts.speak(clean, TextToSpeech.QUEUE_ADD, null, "nav_" + System.currentTimeMillis());
        Log.d(TAG, "TTS: " + clean);
    }

    /**
     * Vibrates the device when a turn is imminent (≤ 30 m).
     * Uses a short double-buzz pattern to signal urgency without being annoying.
     *
     * API breakdown:
     *   API 31+ : VibratorManager  + VibrationEffect.createWaveform()
     *   API 26–30: Vibrator        + VibrationEffect.createWaveform()
     *   API 24–25: Vibrator        + legacy vibrate(long[], int)  ← no VibrationEffect
     *
     * Silently swallows any SecurityException (permission not granted).
     */
    @SuppressWarnings({"MissingPermission", "deprecation"})
    private void vibrateForTurn() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ — VibratorManager is the new entry point
                VibratorManager vm = (VibratorManager)
                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    Vibrator v = vm.getDefaultVibrator();
                    v.vibrate(VibrationEffect.createWaveform(
                            new long[]{0, 120, 80, 120}, -1));
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26–30 — VibrationEffect exists, but use legacy Vibrator service
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    v.vibrate(VibrationEffect.createWaveform(
                            new long[]{0, 120, 80, 120}, -1));
                }
            } else {
                // API 24–25 — VibrationEffect does not exist yet; use the
                // deprecated vibrate(long[], int) overload instead.
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    v.vibrate(new long[]{0, 120, 80, 120}, -1);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Vibration failed: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Inner types
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Direction enum derived from the API's maneuver string.
     * Used internally for producing clean, speech-friendly prompts.
     */
    public enum TurnDirection {
        LEFT,
        RIGHT,
        STRAIGHT,
        ROUNDABOUT,
        U_TURN,
        MERGE,
        RAMP,
        FERRY,
        UNKNOWN
    }

    /**
     * Callback interface for off-route events.
     * Implement in MainActivity and call reroute() inside onOffRoute().
     */
    public interface RerouteListener {
        /**
         * Called when the user appears to have deviated from the current step.
         * MainActivity should call reroute() in response.
         */
        void onOffRoute();
    }
}