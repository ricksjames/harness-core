package io.harness.persistence.converters;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.serializer.KryoSerializer;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

@Singleton
public class ObjectArrayConverter extends TypeConverter implements SimpleValueConverter {
  @Inject private KryoSerializer kryoSerializer;

  public ObjectArrayConverter() {
    super(Object[].class);
  }

  @Override
  public Object encode(Object value, MappedField optionalExtraInfo) {
    if (value == null) {
      return null;
    }
    return kryoSerializer.asBytes(value);
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
    if (fromDBObject == null) {
      return null;
    }
    return kryoSerializer.asObject((byte[]) fromDBObject);
  }
}
