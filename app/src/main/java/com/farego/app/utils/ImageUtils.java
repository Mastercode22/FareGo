package com.farego.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    /**
     * Creates a temporary file in the app's cache directory for camera capture.
     * Uses the app's private cache — no external storage permission needed.
     */
    public static File createTempImageFile(Context context) throws IOException {
        String fileName = "avatar_" + System.currentTimeMillis();
        File storageDir = context.getCacheDir();
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    /**
     * Calculates the largest inSampleSize that keeps the decoded image
     * at or above the requested dimensions. A power of 2 is not required
     * by Room — any integer works fine with BitmapFactory.
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width  = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth  = width  / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth  / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Saves a Bitmap as a JPEG to the app's private files directory.
     * Returns the absolute path of the saved file.
     * The file is named by timestamp so repeat saves don't overwrite each other.
     */
    public static String saveBitmapToPrivateStorage(Context context,
                                                    Bitmap bitmap) throws IOException {
        File dir = new File(context.getFilesDir(), "avatars");
        if (!dir.exists()) dir.mkdirs();

        File outFile = new File(dir, "avatar_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
        }
        return outFile.getAbsolutePath();
    }
}