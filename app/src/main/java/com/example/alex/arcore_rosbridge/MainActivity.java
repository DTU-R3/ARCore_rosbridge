package com.example.alex.arcore_rosbridge;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.jilk.ros.ROSClient;
import com.jilk.ros.Topic;
import com.jilk.ros.message.Pose;
import com.jilk.ros.rosbridge.ROSBridgeClient;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;

    private boolean rosbridge_connected = false;
    private ROSBridgeClient rosclient;

    private EditText rosmaster_ip_txt;
    private EditText rosbridge_port_txt;
    private Button connect_btn;

    private Map<Integer, float[]> image_pos_dict;
    private Node map_node;
    private Node cam_node;
    private Boolean map_calibrated = false;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        rosmaster_ip_txt = findViewById(R.id.rosmaster_ip_txt);
        rosbridge_port_txt = findViewById(R.id.rosbridge_port_txt);
        connect_btn = findViewById(R.id.connect_btn);

        image_pos_dict = new HashMap<>();
        map_node = new Node();
        cam_node = new Node();

        pos_x_txt = findViewById(R.id.pos_x_txt);
        pos_y_txt = findViewById(R.id.pos_y_txt);
        pos_z_txt = findViewById(R.id.pos_z_txt);
        rot_x_txt = findViewById(R.id.rot_x_txt);
        rot_y_txt = findViewById(R.id.rot_y_txt);
        rot_z_txt = findViewById(R.id.rot_z_txt);
        rot_w_txt = findViewById(R.id.rot_w_txt);

        rosmaster_ip_txt.clearFocus();
        rosbridge_port_txt.clearFocus();

        // Load image map json file
        try {
            InputStream is = getAssets().open("augmentedImages.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            JSONObject map_json = new JSONObject(new String(buffer, "UTF-8"));
            for(int i=0; i < map_json.getJSONArray("AugmentedImages").length(); i++) {
                String img_str = map_json.getJSONArray("AugmentedImages").get(i).toString();
                JSONObject img_json = new JSONObject(img_str);
                JSONObject img_pos = img_json.getJSONObject("Position");
                JSONObject img_rot = img_json.getJSONObject("Rotation");
                Quaternion img_quat = new Quaternion(new Vector3((float) img_rot.getDouble("rx") , (float) img_rot.getDouble("ry"), (float) img_rot.getDouble("rz")));
                float[] img_pose = new float[7];
                img_pose[0] = (float) img_pos.getDouble("x");
                img_pose[1] = (float) img_pos.getDouble("y");
                img_pose[2] = (float) img_pos.getDouble("z");
                img_pose[3] = img_quat.x;
                img_pose[4] = img_quat.y;
                img_pose[5] = img_quat.z;
                img_pose[6] = img_quat.w;
                image_pos_dict.put(img_json.getInt("Id"), img_pose);
            }
        }
        catch (Exception ex) {
            Toast toast = Toast.makeText(this, "Unable to load images map", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }

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
                                FragmentTransaction tr = arFragment.getFragmentManager().beginTransaction();
                                tr.replace(R.id.ux_fragment, arFragment);
                                tr.commit();
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
            Frame frame = arFragment.getArSceneView().getArFrame();
            // If there is no frame, just return.
            if (frame == null) {
                return;
            }
            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                return;
            }

            Collection<AugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
            for (AugmentedImage augmentedImage : updatedAugmentedImages) {
                switch (augmentedImage.getTrackingMethod()) {
                    case NOT_TRACKING:
                        break;
                    case LAST_KNOWN_POSE:
                        break;
                    case FULL_TRACKING: // Update only when image in the camera view
                        Anchor imageAnchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());
                        AnchorNode imageAnchorNode = new AnchorNode(imageAnchor);
                        imageAnchorNode.setParent(arFragment.getArSceneView().getScene());
                        map_node.setParent(imageAnchorNode);
                        // Add local transformation
                        float[] img_map_pose = image_pos_dict.get(augmentedImage.getIndex());
                        map_node.setLocalPosition(new Vector3(img_map_pose[0], img_map_pose[1], img_map_pose[2]));
                        map_node.setLocalRotation(new Quaternion(-img_map_pose[3], -img_map_pose[4], -img_map_pose[5], img_map_pose[6]));

                        Vector3 map_trans = map_node.getWorldPosition();
                        Quaternion map_quat = map_node.getWorldRotation();
                        float[] map_trans_float = {map_trans.x, map_trans.y, map_trans.z};
                        float[] map_quat_float = {map_quat.x, map_quat.y, map_quat.z, map_quat.w};
                        com.google.ar.core.Pose map_pose = new com.google.ar.core.Pose(map_trans_float, map_quat_float);
                        Anchor mapAnchor = augmentedImage.createAnchor(map_pose);
                        AnchorNode mapAnchorNode = new AnchorNode(mapAnchor);
                        mapAnchorNode.setParent(arFragment.getArSceneView().getScene());
                        cam_node.setParent(mapAnchorNode);
                        map_calibrated = true;
                        break;
                }

            }

            float[] trans =  arFragment.getArSceneView().getArFrame().getCamera().getDisplayOrientedPose().getTranslation();
            float[] quat =  arFragment.getArSceneView().getArFrame().getCamera().getDisplayOrientedPose().getRotationQuaternion();

            cam_node.setWorldPosition(new Vector3(trans[0], trans[1], trans[2]));
            cam_node.setWorldRotation(new Quaternion(quat[0], quat[1], quat[2], quat[3]));

            pos_x_txt.setText(String.format ("%.2f", cam_node.getLocalPosition().x));
            pos_y_txt.setText(String.format ("%.2f", cam_node.getLocalPosition().y));
            pos_z_txt.setText(String.format ("%.2f", cam_node.getLocalPosition().z));
            rot_x_txt.setText(String.format ("%.2f", cam_node.getLocalRotation().x));
            rot_y_txt.setText(String.format ("%.2f", cam_node.getLocalRotation().y));
            rot_z_txt.setText(String.format ("%.2f", cam_node.getLocalRotation().z));
            rot_w_txt.setText(String.format ("%.2f", cam_node.getLocalRotation().w));

            if(rosbridge_connected) {
                Topic<Pose> cam_world_topic = new com.jilk.ros.Topic<Pose>("/arcore/cam_world_pose", Pose.class, rosclient);
                cam_world_topic.advertise();
                Pose odom_world_msg = new Pose();
                odom_world_msg.position.x = trans[0];
                odom_world_msg.position.y = trans[1];
                odom_world_msg.position.z = trans[2];
                odom_world_msg.orientation.x = quat[0];
                odom_world_msg.orientation.y = quat[1];
                odom_world_msg.orientation.z = quat[2];
                odom_world_msg.orientation.w = quat[3];
                cam_world_topic.publish(odom_world_msg);

                if(map_calibrated) {
                    Topic<Pose> cam_map_topic = new com.jilk.ros.Topic<Pose>("/arcore/cam_map_pose", Pose.class, rosclient);
                    cam_map_topic.advertise();
                    Pose odom_map_msg = new Pose();
                    odom_map_msg.position.x = cam_node.getLocalPosition().x;
                    odom_map_msg.position.y = cam_node.getLocalPosition().y;
                    odom_map_msg.position.z = cam_node.getLocalPosition().z;
                    odom_map_msg.orientation.x = cam_node.getLocalRotation().x;
                    odom_map_msg.orientation.y = cam_node.getLocalRotation().y;
                    odom_map_msg.orientation.z = cam_node.getLocalRotation().z;
                    odom_map_msg.orientation.w = cam_node.getLocalRotation().w;
                    cam_map_topic.publish(odom_map_msg);
                }
            }
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
