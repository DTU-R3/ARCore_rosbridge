package com.jilk.ros.message;

@MessageType(string = "geometry_msgs/Quaternion")
public class Quaternion extends Message {
    public float x;
    public float y;
    public float z;
    public float w;
}
