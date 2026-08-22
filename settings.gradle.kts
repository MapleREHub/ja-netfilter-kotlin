// ============================================================================
// Gradle Settings - 多模块项目
// ============================================================================

rootProject.name = "ja-netfilter"

include(
    "ja-netfilter",
    "plugins:dns",
    "plugins:env",
    "plugins:hideme",
    "plugins:native",
    "plugins:power",
    "plugins:privacy",
    "plugins:url"
)