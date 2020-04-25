variable "deployment" {
  type = string
}

locals {
  qa_filter_prefix = join("\n", [
    "labels.managerHost=\"qa.harness.io\"",
    "labels.app=\"delegate\""
  ])

  stress_filter_prefix = join("\n", [
    "labels.managerHost=\"stress.harness.io\"",
    "labels.app=\"delegate\""
  ])

  prod_filter_prefix = join("\n", [
    "labels.managerHost=\"app.harness.io\"",
    "labels.app=\"delegate\""
  ])

  freemium_filter_prefix = join("\n", [
    "labels.managerHost=\"app.harness.io/gratis\"",
    "labels.app=\"delegate\""
  ])

  filter_prefix = (var.deployment == "qa"     ? local.qa_filter_prefix :
                  (var.deployment == "stress" ? local.stress_filter_prefix :
                  (var.deployment == "prod"   ? local.prod_filter_prefix :
                                                local.freemium_filter_prefix)))

  name_prefix = join("_", ["x", var.deployment, "agent"])

}