package com.jilk.ros.message;

@MessageType(string = "geometry_msgs/Point")
public class Point extends Message {
    public float x;
    public float y;
    public float z;
}
