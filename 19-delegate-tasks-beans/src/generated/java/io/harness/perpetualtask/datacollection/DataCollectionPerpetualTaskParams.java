// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/datacollection/data_collection_task.proto

package io.harness.perpetualtask.datacollection;

/**
 * Protobuf type {@code io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:DataCollectionPerpetualTaskParams.java.pb.meta")
public final class DataCollectionPerpetualTaskParams extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams)
    DataCollectionPerpetualTaskParamsOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use DataCollectionPerpetualTaskParams.newBuilder() to construct.
  private DataCollectionPerpetualTaskParams(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private DataCollectionPerpetualTaskParams() {
    cvConfigId_ = "";
    dataCollectionInfo_ = com.google.protobuf.ByteString.EMPTY;
  }

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new DataCollectionPerpetualTaskParams();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private DataCollectionPerpetualTaskParams(
      com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            cvConfigId_ = s;
            break;
          }
          case 18: {
            dataCollectionInfo_ = input.readBytes();
            break;
          }
          default: {
            if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return io.harness.perpetualtask.datacollection.DataCollectionTask
        .internal_static_io_harness_perpetualtask_datacollection_DataCollectionPerpetualTaskParams_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.perpetualtask.datacollection.DataCollectionTask
        .internal_static_io_harness_perpetualtask_datacollection_DataCollectionPerpetualTaskParams_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams.class,
            io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams.Builder.class);
  }

  public static final int CV_CONFIG_ID_FIELD_NUMBER = 1;
  private volatile java.lang.Object cvConfigId_;
  /**
   * <code>string cv_config_id = 1[json_name = "cvConfigId"];</code>
   * @return The cvConfigId.
   */
  public java.lang.String getCvConfigId() {
    java.lang.Object ref = cvConfigId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      cvConfigId_ = s;
      return s;
    }
  }
  /**
   * <code>string cv_config_id = 1[json_name = "cvConfigId"];</code>
   * @return The bytes for cvConfigId.
   */
  public com.google.protobuf.ByteString getCvConfigIdBytes() {
    java.lang.Object ref = cvConfigId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      cvConfigId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int DATA_COLLECTION_INFO_FIELD_NUMBER = 2;
  private com.google.protobuf.ByteString dataCollectionInfo_;
  /**
   * <code>bytes data_collection_info = 2[json_name = "dataCollectionInfo"];</code>
   * @return The dataCollectionInfo.
   */
  public com.google.protobuf.ByteString getDataCollectionInfo() {
    return dataCollectionInfo_;
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1)
      return true;
    if (isInitialized == 0)
      return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
    if (!getCvConfigIdBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, cvConfigId_);
    }
    if (!dataCollectionInfo_.isEmpty()) {
      output.writeBytes(2, dataCollectionInfo_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (!getCvConfigIdBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, cvConfigId_);
    }
    if (!dataCollectionInfo_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream.computeBytesSize(2, dataCollectionInfo_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams)) {
      return super.equals(obj);
    }
    io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams other =
        (io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams) obj;

    if (!getCvConfigId().equals(other.getCvConfigId()))
      return false;
    if (!getDataCollectionInfo().equals(other.getDataCollectionInfo()))
      return false;
    if (!unknownFields.equals(other.unknownFields))
      return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + CV_CONFIG_ID_FIELD_NUMBER;
    hash = (53 * hash) + getCvConfigId().hashCode();
    hash = (37 * hash) + DATA_COLLECTION_INFO_FIELD_NUMBER;
    hash = (53 * hash) + getDataCollectionInfo().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseFrom(
      java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseFrom(
      com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parseFrom(
      com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() {
    return newBuilder();
  }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(
      io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams)
      io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParamsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.perpetualtask.datacollection.DataCollectionTask
          .internal_static_io_harness_perpetualtask_datacollection_DataCollectionPerpetualTaskParams_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.perpetualtask.datacollection.DataCollectionTask
          .internal_static_io_harness_perpetualtask_datacollection_DataCollectionPerpetualTaskParams_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams.class,
              io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams.Builder.class);
    }

    // Construct using io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      cvConfigId_ = "";

      dataCollectionInfo_ = com.google.protobuf.ByteString.EMPTY;

      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.perpetualtask.datacollection.DataCollectionTask
          .internal_static_io_harness_perpetualtask_datacollection_DataCollectionPerpetualTaskParams_descriptor;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams getDefaultInstanceForType() {
      return io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams build() {
      io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams buildPartial() {
      io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams result =
          new io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams(this);
      result.cvConfigId_ = cvConfigId_;
      result.dataCollectionInfo_ = dataCollectionInfo_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field, int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams) {
        return mergeFrom((io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams other) {
      if (other == io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams.getDefaultInstance())
        return this;
      if (!other.getCvConfigId().isEmpty()) {
        cvConfigId_ = other.cvConfigId_;
        onChanged();
      }
      if (other.getDataCollectionInfo() != com.google.protobuf.ByteString.EMPTY) {
        setDataCollectionInfo(other.getDataCollectionInfo());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
      io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage =
            (io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object cvConfigId_ = "";
    /**
     * <code>string cv_config_id = 1[json_name = "cvConfigId"];</code>
     * @return The cvConfigId.
     */
    public java.lang.String getCvConfigId() {
      java.lang.Object ref = cvConfigId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        cvConfigId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string cv_config_id = 1[json_name = "cvConfigId"];</code>
     * @return The bytes for cvConfigId.
     */
    public com.google.protobuf.ByteString getCvConfigIdBytes() {
      java.lang.Object ref = cvConfigId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        cvConfigId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string cv_config_id = 1[json_name = "cvConfigId"];</code>
     * @param value The cvConfigId to set.
     * @return This builder for chaining.
     */
    public Builder setCvConfigId(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      cvConfigId_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string cv_config_id = 1[json_name = "cvConfigId"];</code>
     * @return This builder for chaining.
     */
    public Builder clearCvConfigId() {
      cvConfigId_ = getDefaultInstance().getCvConfigId();
      onChanged();
      return this;
    }
    /**
     * <code>string cv_config_id = 1[json_name = "cvConfigId"];</code>
     * @param value The bytes for cvConfigId to set.
     * @return This builder for chaining.
     */
    public Builder setCvConfigIdBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      cvConfigId_ = value;
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString dataCollectionInfo_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <code>bytes data_collection_info = 2[json_name = "dataCollectionInfo"];</code>
     * @return The dataCollectionInfo.
     */
    public com.google.protobuf.ByteString getDataCollectionInfo() {
      return dataCollectionInfo_;
    }
    /**
     * <code>bytes data_collection_info = 2[json_name = "dataCollectionInfo"];</code>
     * @param value The dataCollectionInfo to set.
     * @return This builder for chaining.
     */
    public Builder setDataCollectionInfo(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }

      dataCollectionInfo_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>bytes data_collection_info = 2[json_name = "dataCollectionInfo"];</code>
     * @return This builder for chaining.
     */
    public Builder clearDataCollectionInfo() {
      dataCollectionInfo_ = getDefaultInstance().getDataCollectionInfo();
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams)
  }

  // @@protoc_insertion_point(class_scope:io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams)
  private static final io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams();
  }

  public static io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<DataCollectionPerpetualTaskParams> PARSER =
      new com.google.protobuf.AbstractParser<DataCollectionPerpetualTaskParams>() {
        @java.lang.Override
        public DataCollectionPerpetualTaskParams parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new DataCollectionPerpetualTaskParams(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<DataCollectionPerpetualTaskParams> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<DataCollectionPerpetualTaskParams> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
