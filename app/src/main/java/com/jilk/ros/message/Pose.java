package com.jilk.ros.message;

@MessageType(string = "geometry_msgs/Pose")
public class Pose extends Message {
    public Point position = new Point();
    public Quaternion orientation = new Quaternion();
}
