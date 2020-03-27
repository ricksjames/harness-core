package software.wings.service.intfc.template;

import static software.wings.beans.template.TemplateVersion.ChangeType;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.template.TemplateVersion;

import java.util.List;
import java.util.Map;

public interface TemplateVersionService {
  PageResponse<TemplateVersion> listTemplateVersions(PageRequest<TemplateVersion> pageRequest);

  List<TemplateVersion> listImportedTemplateVersions(String templateId, String accountId);

  Map<String, String> listLatestVersionOfImportedTemplates(List<String> templateUuids, String accountId);

  TemplateVersion lastTemplateVersion(@NotEmpty String accountId, @NotEmpty String templateUuid);

  TemplateVersion newReferencedTemplateVersion(String accountId, String galleryId, String templateUuid,
      String templateType, String templateName, Long version, String versionDetails);

  TemplateVersion newTemplateVersion(@NotEmpty String accountId, @NotEmpty String galleryId,
      @NotEmpty String templateUuid, @NotEmpty String templateType, @NotEmpty String templateName,
      @NotEmpty ChangeType changeType);
}
