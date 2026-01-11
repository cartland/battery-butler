load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_java//java:defs.bzl", "java_proto_library")
load("@rules_swift//swift:swift.bzl", "swift_proto_library")

package(default_visibility = ["//visibility:public"])

proto_library(
    name = "battery_butler_proto",
    srcs = glob(["com/chriscartland/batterybutler/*.proto"]),
    # Defines the root for imports.
    # We want to import "com/chriscartland/batterybutler/model.proto"
    # The files are in "protos/com/chriscartland/batterybutler/model.proto"
    # So we strip "protos".
    strip_import_prefix = "/protos",
    deps = [
        "@protobuf//:timestamp_proto",
        "@protobuf//:empty_proto",
    ],
)
java_proto_library(
    name = "battery_butler_java_proto",
    deps = [":battery_butler_proto"],
)

swift_proto_library(
    name = "battery_butler_swift_proto",
    deps = [":battery_butler_proto"],
)
