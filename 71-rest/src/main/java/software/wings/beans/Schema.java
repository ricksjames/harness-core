package software.wings.beans;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(value = "schema", noClassnameStored = true)
@HarnessExportableEntity
public class Schema implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static final String SCHEMA_ID = "schema";
  public static final String VERSION = "version";
  public static final String BACKGROUND_VERSION = "backgroundVersion";
  public static final String SEED_DATA_VERSION = "seedDataVersion";
  public static final String TIMESCALEDB_VERSION = "timescaleDbVersion";
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @SchemaIgnore @Indexed private long createdAt;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  private int version;
  private int backgroundVersion;
  private int seedDataVersion;
  private int timescaleDbVersion;

  @Override
  public String getUuid() {
    return SCHEMA_ID;
  }
}
