package io.harness.serializer;

import io.harness.beans.query.SortPattern;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class SortPatternWriteConverter implements Converter<SortPattern, String> {
  @Override
  public String convert(SortPattern sortPattern) {
    if (sortPattern == null) {
      return null;
    }
    return JsonUtils.asJson(sortPattern);
  }
}
