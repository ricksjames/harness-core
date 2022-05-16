/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer;

import static io.harness.rule.OwnerRule.JOHANNES;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.esotericsoftware.kryo.Kryo;
import java.util.Arrays;
import java.util.HashSet;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class KryoSerializerDtoTest extends CategoryTest {
  public static final Integer REGISTRATION_ID = 421;

  private static final KryoSerializer originalSerializer =
      new KryoSerializer(new HashSet<>(Arrays.asList(OriginalRegistrar.class)));
  private static final KryoSerializer dtoSerializer =
      new KryoSerializer(new HashSet<>(Arrays.asList(DtoRegistrar.class)));

  @Test
  @Owner(developers = JOHANNES)
  @Category(UnitTests.class)
  public void testSerializationFromOriginalToDto() {
    Original source = Original.builder().name("someName").value(42).originalOnlyField(2.1).build();

    byte[] serializedOriginal = originalSerializer.asBytes(source);
    OriginalDTO deserializedAsDto = (OriginalDTO) dtoSerializer.asObject(serializedOriginal);

    assertThat(deserializedAsDto.getName()).isEqualTo(source.getName());
    assertThat(deserializedAsDto.getValue()).isEqualTo(source.getValue());
  }

  @Test
  @Owner(developers = JOHANNES)
  @Category(UnitTests.class)
  public void testSerializationFromDtoToOriginal() {
    OriginalDTO sourceDto = OriginalDTO.builder().name("someName").value(42).build();

    byte[] serializedDto = dtoSerializer.asBytes(sourceDto);
    Original deserializedAsOriginal = (Original) originalSerializer.asObject(serializedDto);

    assertThat(deserializedAsOriginal.getName()).isEqualTo(sourceDto.getName());
    assertThat(deserializedAsOriginal.getValue()).isEqualTo(sourceDto.getValue());

    // original only field should be the default value
    assertThat(deserializedAsOriginal.getOriginalOnlyField())
        .isEqualTo(Original.builder().build().getOriginalOnlyField());
  }

  @Data
  @Builder
  public static class Original {
    private String name;
    private int value;
    private double originalOnlyField;
  }

  @Data
  @Builder
  public static class OriginalDTO {
    private String name;
    private int value;
  }

  public static class OriginalRegistrar implements KryoRegistrar {
    @Override
    public void register(Kryo kryo) {
      kryo.register(Original.class, REGISTRATION_ID);
    }
  }

  public static class DtoRegistrar implements KryoRegistrar {
    @Override
    public void register(Kryo kryo) {
      kryo.register(OriginalDTO.class, REGISTRATION_ID);
    }
  }
}
