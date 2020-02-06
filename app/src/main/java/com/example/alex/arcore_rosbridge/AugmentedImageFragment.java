package com.example.alex.arcore_rosbridge;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.Nullable;

public class AugmentedImageFragment extends ArFragment {
    private static final String TAG = "AugmentedImageFragment";

    // This is a pre-created database containing the sample image.
    private static final String SAMPLE_IMAGE_DATABASE = "robot.imgdb";

    // Do a runtime check for the OpenGL level available at runtime to avoid Sceneform crashing the
    // application.
    private static final double MIN_OPENGL_VERSION = 3.0;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Check for Sceneform being supported on this device.  This check will be integrated into
        // Sceneform eventually.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(getActivity(), "Sceneform requires Android N or later", Toast.LENGTH_LONG);
        }

        String openGlVersionString =
                ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later");
            Toast.makeText(getActivity(), "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG);
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }

    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = super.getSessionConfiguration(session);
        if (!setupAugmentedImageDatabase(config, session)) {

        }
        return config;
    }

    private boolean setupAugmentedImageDatabase(Config config, Session session) {
        AugmentedImageDatabase augmentedImageDatabase;

        AssetManager assetManager = getContext() != null ? getContext().getAssets() : null;
        if (assetManager == null) {
            Log.e(TAG, "Context is null, cannot intitialize image database.");
            return false;
        }

        try (InputStream is = getContext().getAssets().open(SAMPLE_IMAGE_DATABASE)) {
            augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image database.", e);
            return false;
        }

        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }
}