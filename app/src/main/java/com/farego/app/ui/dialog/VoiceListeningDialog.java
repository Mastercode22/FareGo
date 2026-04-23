package com.farego.app.ui.dialog;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.farego.app.R;

/**
 * VoiceListeningDialog
 *
 * A non-cancellable overlay shown while SpeechRecognizer is listening.
 * Features a three-ring pulsing animation around a microphone icon.
 *
 * Usage:
 *   VoiceListeningDialog dialog = new VoiceListeningDialog(context);
 *   dialog.show();                          // when recognition starts
 *   dialog.updatePartial("Kasoa Ove…");    // as partial results arrive
 *   dialog.dismiss();                       // when recognition ends
 *
 * Layout: R.layout.dialog_voice_listening  (see dialog_voice_listening.xml)
 */
public class VoiceListeningDialog extends Dialog {

    private TextView    tvStatus;
    private TextView    tvPartial;
    private ImageView   ivMic;
    private View        ring1, ring2, ring3;

    private AnimatorSet pulseAnimator;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public VoiceListeningDialog(@NonNull Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_voice_listening);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setCancelable(false);
        bindViews();
    }

    private void bindViews() {
        tvStatus  = findViewById(R.id.tv_voice_status);
        tvPartial = findViewById(R.id.tv_voice_partial);
        ivMic     = findViewById(R.id.iv_mic_icon);
        ring1     = findViewById(R.id.ring_1);
        ring2     = findViewById(R.id.ring_2);
        ring3     = findViewById(R.id.ring_3);
    }

    @Override
    public void show() {
        super.show();
        startPulseAnimation();
        if (tvStatus != null) tvStatus.setText("Listening…");
        if (tvPartial != null) tvPartial.setText("");
    }

    @Override
    public void dismiss() {
        stopPulseAnimation();
        super.dismiss();
    }

    /** Update the partial-result subtitle as the user is speaking. */
    public void updatePartial(String partial) {
        uiHandler.post(() -> {
            if (tvPartial != null && partial != null) {
                tvPartial.setText(partial);
            }
        });
    }

    /** Switch the status label (e.g. "Processing…"). */
    public void setStatus(String status) {
        uiHandler.post(() -> {
            if (tvStatus != null) tvStatus.setText(status);
        });
    }

    // ── Pulse animation ───────────────────────────────────────────────────────

    /**
     * Three concentric rings pulse outward with staggered delays,
     * creating a sonar / heartbeat effect.
     */
    private void startPulseAnimation() {
        if (ring1 == null || ring2 == null || ring3 == null) return;

        pulseAnimator = new AnimatorSet();

        AnimatorSet set1 = makeRingPulse(ring1, 0);
        AnimatorSet set2 = makeRingPulse(ring2, 250);
        AnimatorSet set3 = makeRingPulse(ring3, 500);

        pulseAnimator.playTogether(
                set1.getChildAnimations().get(0),
                set1.getChildAnimations().get(1),
                set2.getChildAnimations().get(0),
                set2.getChildAnimations().get(1),
                set3.getChildAnimations().get(0),
                set3.getChildAnimations().get(1)
        );
        pulseAnimator.start();
    }

    private AnimatorSet makeRingPulse(View ring, long startDelay) {
        ring.setAlpha(0f);
        ring.setScaleX(0.5f);
        ring.setScaleY(0.5f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 0.5f, 1.8f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 0.5f, 1.8f);
        ObjectAnimator alpha  = ObjectAnimator.ofFloat(ring, "alpha",  0.8f, 0f);

        scaleX.setDuration(1400);
        scaleY.setDuration(1400);
        alpha.setDuration(1400);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleX.setStartDelay(startDelay);
        scaleY.setStartDelay(startDelay);
        alpha.setStartDelay(startDelay);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        return set;
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        // Reset ring visibility
        if (ring1 != null) ring1.setAlpha(0f);
        if (ring2 != null) ring2.setAlpha(0f);
        if (ring3 != null) ring3.setAlpha(0f);
    }
}