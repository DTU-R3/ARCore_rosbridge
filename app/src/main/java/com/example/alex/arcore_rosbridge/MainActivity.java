package com.example.alex.arcore_rosbridge;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.ux.ArFragment;
import com.jilk.ros.ROSClient;
import com.jilk.ros.Topic;
import com.jilk.ros.message.Pose;
import com.jilk.ros.rosbridge.ROSBridgeClient;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;

    private boolean rosbridge_connected = false;
    private ROSBridgeClient rosclient;

    private EditText rosmaster_ip_txt;
    private EditText rosbridge_port_txt;
    private Button connect_btn;

    private TextView pos_x_txt;
    private TextView pos_y_txt;
    private TextView pos_z_txt;
    private TextView rot_x_txt;
    private TextView rot_y_txt;
    private TextView rot_z_txt;
    private TextView rot_w_txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        rosmaster_ip_txt = findViewById(R.id.rosmaster_ip_txt);
        rosbridge_port_txt = findViewById(R.id.rosbridge_port_txt);
        connect_btn = findViewById(R.id.connect_btn);

        pos_x_txt = findViewById(R.id.pos_x_txt);
        pos_y_txt = findViewById(R.id.pos_y_txt);
        pos_z_txt = findViewById(R.id.pos_z_txt);
        rot_x_txt = findViewById(R.id.rot_x_txt);
        rot_y_txt = findViewById(R.id.rot_y_txt);
        rot_z_txt = findViewById(R.id.rot_z_txt);
        rot_w_txt = findViewById(R.id.rot_w_txt);

        rosmaster_ip_txt.clearFocus();
        rosbridge_port_txt.clearFocus();

        connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rosmaster_ip_txt.clearFocus();
                rosbridge_port_txt.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                String socket = "ws://" + rosmaster_ip_txt.getText().toString().trim() + ":" + rosbridge_port_txt.getText().toString().trim();
                // Toast.makeText(getApplicationContext(), socket, Toast.LENGTH_LONG).show();
                rosclient = new ROSBridgeClient(socket);
                rosbridge_connected = rosclient.connect(new ROSClient.ConnectionStatusListener() {
                    @Override
                    public void onConnect() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(rosbridge_connected) {
                                    Toast.makeText(getApplicationContext(), "Connected to ROS", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }

                    @Override
                    public void onDisconnect(boolean normal, String reason, int code) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Disconnected to ROS", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(Exception ex) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if(!rosbridge_connected) {
                return;
            }
            Frame frame = arFragment.getArSceneView().getArFrame();
            // If there is no frame, just return.
            if (frame == null) {
                return;
            }
            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                return;
            }

            float[] trans =  arFragment.getArSceneView().getArFrame().getCamera().getDisplayOrientedPose().getTranslation();
            float[] quat =  arFragment.getArSceneView().getArFrame().getCamera().getDisplayOrientedPose().getRotationQuaternion();

            Topic<Pose> odom_topic = new com.jilk.ros.Topic<Pose>("/arcore/pose", Pose.class, rosclient);
            Pose pose_msg = new Pose();
            pose_msg.position.x = trans[0];
            pose_msg.position.y = trans[1];
            pose_msg.position.z = trans[2];
            pose_msg.orientation.x = quat[0];
            pose_msg.orientation.y = quat[1];
            pose_msg.orientation.z = quat[2];
            pose_msg.orientation.w = quat[3];
            odom_topic.advertise();
            odom_topic.publish(pose_msg);

            pos_x_txt.setText(String.format ("%.2f", trans[0]));
            pos_y_txt.setText(String.format ("%.2f", trans[1]));
            pos_z_txt.setText(String.format ("%.2f", trans[2]));
            rot_x_txt.setText(String.format ("%.2f", quat[0]));
            rot_y_txt.setText(String.format ("%.2f", quat[1]));
            rot_z_txt.setText(String.format ("%.2f", quat[2]));
            rot_w_txt.setText(String.format ("%.2f", quat[3]));
        });
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

}
