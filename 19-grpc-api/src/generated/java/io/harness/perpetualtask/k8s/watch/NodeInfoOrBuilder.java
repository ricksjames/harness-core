// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/k8s.watch/k8s_messages.proto

package io.harness.perpetualtask.k8s.watch;

@javax.annotation.Generated(value = "protoc", comments = "annotations:NodeInfoOrBuilder.java.pb.meta")
public interface NodeInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.perpetualtask.k8s.watch.NodeInfo)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>string account_id = 1;</code>
   */
  java.lang.String getAccountId();
  /**
   * <code>string account_id = 1;</code>
   */
  com.google.protobuf.ByteString getAccountIdBytes();

  /**
   * <code>string cloud_provider_id = 2;</code>
   */
  java.lang.String getCloudProviderId();
  /**
   * <code>string cloud_provider_id = 2;</code>
   */
  com.google.protobuf.ByteString getCloudProviderIdBytes();

  /**
   * <code>string node_uid = 3;</code>
   */
  java.lang.String getNodeUid();
  /**
   * <code>string node_uid = 3;</code>
   */
  com.google.protobuf.ByteString getNodeUidBytes();

  /**
   * <code>string node_name = 4;</code>
   */
  java.lang.String getNodeName();
  /**
   * <code>string node_name = 4;</code>
   */
  com.google.protobuf.ByteString getNodeNameBytes();

  /**
   * <code>.google.protobuf.Timestamp creation_time = 5;</code>
   */
  boolean hasCreationTime();
  /**
   * <code>.google.protobuf.Timestamp creation_time = 5;</code>
   */
  com.google.protobuf.Timestamp getCreationTime();
  /**
   * <code>.google.protobuf.Timestamp creation_time = 5;</code>
   */
  com.google.protobuf.TimestampOrBuilder getCreationTimeOrBuilder();

  /**
   * <code>map&lt;string, string&gt; labels = 6;</code>
   */
  int getLabelsCount();
  /**
   * <code>map&lt;string, string&gt; labels = 6;</code>
   */
  boolean containsLabels(java.lang.String key);
  /**
   * Use {@link #getLabelsMap()} instead.
   */
  @java.lang.Deprecated java.util.Map<java.lang.String, java.lang.String> getLabels();
  /**
   * <code>map&lt;string, string&gt; labels = 6;</code>
   */
  java.util.Map<java.lang.String, java.lang.String> getLabelsMap();
  /**
   * <code>map&lt;string, string&gt; labels = 6;</code>
   */

  java.lang.String getLabelsOrDefault(java.lang.String key, java.lang.String defaultValue);
  /**
   * <code>map&lt;string, string&gt; labels = 6;</code>
   */

  java.lang.String getLabelsOrThrow(java.lang.String key);
}
