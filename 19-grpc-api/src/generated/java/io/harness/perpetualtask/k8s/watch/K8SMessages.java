// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/k8s.watch/k8s_messages.proto

package io.harness.perpetualtask.k8s.watch;

@javax.annotation.Generated(value = "protoc", comments = "annotations:K8SMessages.java.pb.meta")
public final class K8SMessages {
  private K8SMessages() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_Resource_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_Resource_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_Resource_Quantity_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_Resource_Quantity_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_Resource_RequestsEntry_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_Resource_RequestsEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_Resource_LimitsEntry_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_Resource_LimitsEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_Container_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_Container_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_Owner_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_Owner_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_LabelsEntry_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_LabelsEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_PodEvent_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_PodEvent_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_LabelsEntry_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_LabelsEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_k8s_watch_NodeEvent_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_k8s_watch_NodeEvent_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n5io/harness/perpetualtask/k8s.watch/k8s"
        + "_messages.proto\022\"io.harness.perpetualtas"
        + "k.k8s.watch\032\037google/protobuf/timestamp.p"
        + "roto\"\252\003\n\010Resource\022L\n\010requests\030\001 \003(\0132:.io"
        + ".harness.perpetualtask.k8s.watch.Resourc"
        + "e.RequestsEntry\022H\n\006limits\030\002 \003(\01328.io.har"
        + "ness.perpetualtask.k8s.watch.Resource.Li"
        + "mitsEntry\0328\n\010Quantity\022\016\n\006amount\030\001 \001(\t\022\014\n"
        + "\004unit\030\002 \001(\t\022\016\n\006format\030\003 \001(\t\032f\n\rRequestsE"
        + "ntry\022\013\n\003key\030\001 \001(\t\022D\n\005value\030\002 \001(\01325.io.ha"
        + "rness.perpetualtask.k8s.watch.Resource.Q"
        + "uantity:\0028\001\032d\n\013LimitsEntry\022\013\n\003key\030\001 \001(\t\022"
        + "D\n\005value\030\002 \001(\01325.io.harness.perpetualtas"
        + "k.k8s.watch.Resource.Quantity:\0028\001\"h\n\tCon"
        + "tainer\022\014\n\004name\030\001 \001(\t\022\r\n\005image\030\002 \001(\t\022>\n\010r"
        + "esource\030\003 \001(\0132,.io.harness.perpetualtask"
        + ".k8s.watch.Resource\"0\n\005Owner\022\013\n\003uid\030\001 \001("
        + "\t\022\014\n\004kind\030\002 \001(\t\022\014\n\004name\030\003 \001(\t\"\270\004\n\007PodInf"
        + "o\022\022\n\naccount_id\030\001 \001(\t\022\031\n\021cloud_provider_"
        + "id\030\002 \001(\t\022\017\n\007pod_uid\030\003 \001(\t\022\020\n\010pod_name\030\004 "
        + "\001(\t\022\021\n\tnamespace\030\005 \001(\t\022\021\n\tnode_name\030\006 \001("
        + "\t\022D\n\016total_resource\030\007 \001(\0132,.io.harness.p"
        + "erpetualtask.k8s.watch.Resource\0226\n\022creat"
        + "ion_timestamp\030\010 \001(\0132\032.google.protobuf.Ti"
        + "mestamp\022G\n\006labels\030\t \003(\01327.io.harness.per"
        + "petualtask.k8s.watch.PodInfo.LabelsEntry"
        + "\0228\n\005owner\030\n \003(\0132).io.harness.perpetualta"
        + "sk.k8s.watch.Owner\022B\n\017top_level_owner\030\013 "
        + "\001(\0132).io.harness.perpetualtask.k8s.watch"
        + ".Owner\022A\n\ncontainers\030\014 \003(\0132-.io.harness."
        + "perpetualtask.k8s.watch.Container\032-\n\013Lab"
        + "elsEntry\022\013\n\003key\030\001 \001(\t\022\r\n\005value\030\002 \001(\t:\0028\001"
        + "\"\226\002\n\010PodEvent\022\022\n\naccount_id\030\001 \001(\t\022\031\n\021clo"
        + "ud_provider_id\030\002 \001(\t\022\017\n\007pod_uid\030\003 \001(\t\022D\n"
        + "\004type\030\004 \001(\01626.io.harness.perpetualtask.k"
        + "8s.watch.PodEvent.EventType\022-\n\ttimestamp"
        + "\030\005 \001(\0132\032.google.protobuf.Timestamp\"U\n\tEv"
        + "entType\022\026\n\022EVENT_TYPE_INVALID\020\000\022\030\n\024EVENT"
        + "_TYPE_SCHEDULED\020\001\022\026\n\022EVENT_TYPE_DELETED\020"
        + "\002\"\212\002\n\010NodeInfo\022\022\n\naccount_id\030\001 \001(\t\022\031\n\021cl"
        + "oud_provider_id\030\002 \001(\t\022\020\n\010node_uid\030\003 \001(\t\022"
        + "\021\n\tnode_name\030\004 \001(\t\0221\n\rcreation_time\030\005 \001("
        + "\0132\032.google.protobuf.Timestamp\022H\n\006labels\030"
        + "\006 \003(\01328.io.harness.perpetualtask.k8s.wat"
        + "ch.NodeInfo.LabelsEntry\032-\n\013LabelsEntry\022\013"
        + "\n\003key\030\001 \001(\t\022\r\n\005value\030\002 \001(\t:\0028\001\"\222\002\n\tNodeE"
        + "vent\022\022\n\naccount_id\030\001 \001(\t\022\031\n\021cloud_provid"
        + "er_id\030\002 \001(\t\022\020\n\010node_uid\030\003 \001(\t\022E\n\004type\030\004 "
        + "\001(\01627.io.harness.perpetualtask.k8s.watch"
        + ".NodeEvent.EventType\022-\n\ttimestamp\030\005 \001(\0132"
        + "\032.google.protobuf.Timestamp\"N\n\tEventType"
        + "\022\026\n\022EVENT_TYPE_INVALID\020\000\022\024\n\020EVENT_TYPE_S"
        + "TART\020\001\022\023\n\017EVENT_TYPE_STOP\020\002B\002P\001b\006proto3"};
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
            com.google.protobuf.TimestampProto.getDescriptor(),
        },
        assigner);
    internal_static_io_harness_perpetualtask_k8s_watch_Resource_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_perpetualtask_k8s_watch_Resource_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_Resource_descriptor,
            new java.lang.String[] {
                "Requests",
                "Limits",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_Resource_Quantity_descriptor =
        internal_static_io_harness_perpetualtask_k8s_watch_Resource_descriptor.getNestedTypes().get(0);
    internal_static_io_harness_perpetualtask_k8s_watch_Resource_Quantity_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_Resource_Quantity_descriptor,
            new java.lang.String[] {
                "Amount",
                "Unit",
                "Format",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_Resource_RequestsEntry_descriptor =
        internal_static_io_harness_perpetualtask_k8s_watch_Resource_descriptor.getNestedTypes().get(1);
    internal_static_io_harness_perpetualtask_k8s_watch_Resource_RequestsEntry_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_Resource_RequestsEntry_descriptor,
            new java.lang.String[] {
                "Key",
                "Value",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_Resource_LimitsEntry_descriptor =
        internal_static_io_harness_perpetualtask_k8s_watch_Resource_descriptor.getNestedTypes().get(2);
    internal_static_io_harness_perpetualtask_k8s_watch_Resource_LimitsEntry_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_Resource_LimitsEntry_descriptor,
            new java.lang.String[] {
                "Key",
                "Value",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_Container_descriptor = getDescriptor().getMessageTypes().get(1);
    internal_static_io_harness_perpetualtask_k8s_watch_Container_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_Container_descriptor,
            new java.lang.String[] {
                "Name",
                "Image",
                "Resource",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_Owner_descriptor = getDescriptor().getMessageTypes().get(2);
    internal_static_io_harness_perpetualtask_k8s_watch_Owner_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_Owner_descriptor,
            new java.lang.String[] {
                "Uid",
                "Kind",
                "Name",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_descriptor = getDescriptor().getMessageTypes().get(3);
    internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_descriptor,
            new java.lang.String[] {
                "AccountId",
                "CloudProviderId",
                "PodUid",
                "PodName",
                "Namespace",
                "NodeName",
                "TotalResource",
                "CreationTimestamp",
                "Labels",
                "Owner",
                "TopLevelOwner",
                "Containers",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_LabelsEntry_descriptor =
        internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_descriptor.getNestedTypes().get(0);
    internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_LabelsEntry_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_PodInfo_LabelsEntry_descriptor,
            new java.lang.String[] {
                "Key",
                "Value",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_PodEvent_descriptor = getDescriptor().getMessageTypes().get(4);
    internal_static_io_harness_perpetualtask_k8s_watch_PodEvent_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_PodEvent_descriptor,
            new java.lang.String[] {
                "AccountId",
                "CloudProviderId",
                "PodUid",
                "Type",
                "Timestamp",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_descriptor = getDescriptor().getMessageTypes().get(5);
    internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_descriptor,
            new java.lang.String[] {
                "AccountId",
                "CloudProviderId",
                "NodeUid",
                "NodeName",
                "CreationTime",
                "Labels",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_LabelsEntry_descriptor =
        internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_descriptor.getNestedTypes().get(0);
    internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_LabelsEntry_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_NodeInfo_LabelsEntry_descriptor,
            new java.lang.String[] {
                "Key",
                "Value",
            });
    internal_static_io_harness_perpetualtask_k8s_watch_NodeEvent_descriptor = getDescriptor().getMessageTypes().get(6);
    internal_static_io_harness_perpetualtask_k8s_watch_NodeEvent_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_k8s_watch_NodeEvent_descriptor,
            new java.lang.String[] {
                "AccountId",
                "CloudProviderId",
                "NodeUid",
                "Type",
                "Timestamp",
            });
    com.google.protobuf.TimestampProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
