// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/event/payloads/ec2_messages.proto

package io.harness.event.payloads;

@javax.annotation.Generated(value = "protoc", comments = "annotations:Ec2InstanceInfoOrBuilder.java.pb.meta")
public interface Ec2InstanceInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.event.payloads.Ec2InstanceInfo)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>string instance_id = 1;</code>
   */
  java.lang.String getInstanceId();
  /**
   * <code>string instance_id = 1;</code>
   */
  com.google.protobuf.ByteString getInstanceIdBytes();

  /**
   * <code>string instance_type = 2;</code>
   */
  java.lang.String getInstanceType();
  /**
   * <code>string instance_type = 2;</code>
   */
  com.google.protobuf.ByteString getInstanceTypeBytes();

  /**
   * <code>string capacity_reservation_id = 3;</code>
   */
  java.lang.String getCapacityReservationId();
  /**
   * <code>string capacity_reservation_id = 3;</code>
   */
  com.google.protobuf.ByteString getCapacityReservationIdBytes();

  /**
   * <code>string spot_instance_request_id = 4;</code>
   */
  java.lang.String getSpotInstanceRequestId();
  /**
   * <code>string spot_instance_request_id = 4;</code>
   */
  com.google.protobuf.ByteString getSpotInstanceRequestIdBytes();

  /**
   * <code>string instance_lifecycle = 5;</code>
   */
  java.lang.String getInstanceLifecycle();
  /**
   * <code>string instance_lifecycle = 5;</code>
   */
  com.google.protobuf.ByteString getInstanceLifecycleBytes();

  /**
   * <code>.io.harness.event.payloads.InstanceState instance_state = 6;</code>
   */
  boolean hasInstanceState();
  /**
   * <code>.io.harness.event.payloads.InstanceState instance_state = 6;</code>
   */
  io.harness.event.payloads.InstanceState getInstanceState();
  /**
   * <code>.io.harness.event.payloads.InstanceState instance_state = 6;</code>
   */
  io.harness.event.payloads.InstanceStateOrBuilder getInstanceStateOrBuilder();

  /**
   * <code>string cluster_arn = 7;</code>
   */
  java.lang.String getClusterArn();
  /**
   * <code>string cluster_arn = 7;</code>
   */
  com.google.protobuf.ByteString getClusterArnBytes();

  /**
   * <code>string region = 8;</code>
   */
  java.lang.String getRegion();
  /**
   * <code>string region = 8;</code>
   */
  com.google.protobuf.ByteString getRegionBytes();
}
