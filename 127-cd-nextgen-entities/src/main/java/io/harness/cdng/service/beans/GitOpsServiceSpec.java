package io.harness.cdng.service.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.visitor.helpers.serviceconfig.GitOpsServiceSpecVisitorHelper;
import io.harness.cdng.visitor.helpers.serviceconfig.KubernetesServiceSpecVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(ServiceSpecType.KUBERNETES)
@SimpleVisitorHelper(helperClass = GitOpsServiceSpecVisitorHelper.class)
@TypeAlias("gitopsServiceSpec")
@RecasterAlias("io.harness.cdng.service.beans.GitOpsServiceSpec")
@OwnedBy(HarnessTeam.CDP)
public class GitOpsServiceSpec implements ServiceSpec, Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;

  List<NGVariable> variables;
  ArtifactListConfig artifacts;
  List<ManifestConfigWrapper> manifests;

  @Override
  public String getType() {
    return ServiceDefinitionType.GITOPS.getYamlName();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    if (EmptyPredicate.isNotEmpty(variables)) {
      variables.forEach(ngVariable -> children.add("variables", ngVariable));
    }
    return children;
  }
}
