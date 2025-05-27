package io.opentelemetry.exporter.internal.otlp;

import io.opentelemetry.api.internal.StringUtils;
import io.opentelemetry.exporter.internal.marshal.MarshalerUtil;
import io.opentelemetry.exporter.internal.marshal.MarshalerWithSize;
import io.opentelemetry.exporter.internal.marshal.Serializer;
import io.opentelemetry.proto.common.v1.internal.EntityRef;
import io.opentelemetry.sdk.entities.Entity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import java.util.Collection;

import javax.annotation.Nullable;

/**
 * A Marshaler of {@link io.opentelemetry.sdk.resources.Entity}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class EntityRefMarshaler extends MarshalerWithSize {
  @Nullable private final byte[] schemaUrlUtf8;
  private final byte[] typeUtf8;
  private final byte[][] identityAttributeKeysUtf8;
  private final byte[][] descriptiveAttributeKeysUtf8;
  private static final EntityRefMarshaler[] EMPTY_REPEATED = new EntityRefMarshaler[0];

  @Override
  protected void writeTo(Serializer output) throws IOException {
    if (schemaUrlUtf8 != null) {
      output.writeString(EntityRef.SCHEMA_URL, schemaUrlUtf8);
    }
    output.writeString(EntityRef.TYPE, typeUtf8);
    output.writeRepeatedString(
        EntityRef.ID_KEYS, identityAttributeKeysUtf8);
    output.writeRepeatedString(
        EntityRef.DESCRIPTION_KEYS, descriptiveAttributeKeysUtf8);
  }

  public static EntityRefMarshaler createForEntity(Entity e) {
    byte[] schemaUrlUtf8 = null;
    if (!StringUtils.isNullOrEmpty(e.getSchemaUrl())) {
      schemaUrlUtf8 = e.getSchemaUrl().getBytes(StandardCharsets.UTF_8);
    }
    return new EntityRefMarshaler(
        schemaUrlUtf8,
        e.getType().getBytes(StandardCharsets.UTF_8),
        e.getIdentifyingAttributes().asMap().keySet().stream()
            .map(key -> key.getKey().getBytes(StandardCharsets.UTF_8))
            .toArray(byte[][]::new),
        e.getAttributes().asMap().keySet().stream()
            .map(key -> key.getKey().getBytes(StandardCharsets.UTF_8))
            .toArray(byte[][]::new));
  }

  @SuppressWarnings("AvoidObjectArrays")
  public static EntityRefMarshaler[] createForEntities(Collection<Entity> entities) {
    if (entities.isEmpty()) {
        return EMPTY_REPEATED;
    }
    return entities.stream().map(EntityRefMarshaler::createForEntity).toArray(size -> new EntityRefMarshaler[size]);
  }

  private EntityRefMarshaler(
      @Nullable byte[] schemaUrlUtf8,
      byte[] typeUtf8,
      byte[][] identityAttributeKeysUtf8,
      byte[][] descriptiveAttributeKeysUtf8) {
    super(
        calculateSize(
            schemaUrlUtf8, typeUtf8, identityAttributeKeysUtf8, descriptiveAttributeKeysUtf8));
    this.schemaUrlUtf8 = schemaUrlUtf8;
    this.typeUtf8 = typeUtf8;
    this.identityAttributeKeysUtf8 = identityAttributeKeysUtf8;
    this.descriptiveAttributeKeysUtf8 = descriptiveAttributeKeysUtf8;
  }

  private static int calculateSize(
      @Nullable byte[] schemaUrlUtf8,
      byte[] typeUtf8,
      byte[][] identityAttributeKeysUtf8,
      byte[][] descriptiveAttributeKeysUtf8) {
    int size = 0;
    if (schemaUrlUtf8 != null) {
      size += MarshalerUtil.sizeBytes(EntityRef.SCHEMA_URL, schemaUrlUtf8);
    }
    size += MarshalerUtil.sizeBytes(EntityRef.TYPE, typeUtf8);
    for (byte[] keyUtf8 : identityAttributeKeysUtf8) {
      size += MarshalerUtil.sizeBytes(EntityRef.ID_KEYS, keyUtf8);
    }
    for (byte[] keyUtf8 : descriptiveAttributeKeysUtf8) {
      size +=
          MarshalerUtil.sizeBytes(EntityRef.DESCRIPTION_KEYS, keyUtf8);
    }
    return size;
  }
}