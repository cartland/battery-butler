load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_java//java:defs.bzl", "java_proto_library")
load("@rules_swift//swift:swift.bzl", "swift_proto_library")

package(default_visibility = ["//visibility:public"])

proto_library(
    name = "battery_butler_proto",
    srcs = [
        "com/chriscartland/batterybutler/protos/battery_service.proto",
        "com/chriscartland/batterybutler/protos/model.proto",
        "com/chriscartland/batterybutler/protos/sync_service.proto",
    ],
    # Defines the root for imports.
    # We want to import "com/chriscartland/batterybutler/protos/model.proto"
    # The files are in "protos/com/chriscartland/batterybutler/protos/model.proto"
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

genrule(
    name = "mobile_protos_tar",
    srcs = [
        ":battery_butler_java_proto",
        ":battery_butler_swift_proto",
    ],
    outs = ["mobile_protos.tar"],
    cmd = """
        mkdir -p build/java build/swift
        
        # Extract Java sources
        for f in $(locations :battery_butler_java_proto); do
            if [[ "$$f" == *"-src.jar" ]]; then
                unzip -q -o "$$f" -d build/java
            fi
        done
        
        # Copy Swift sources
        for f in $(locations :battery_butler_swift_proto); do
            if [[ "$$f" == *".swift" ]]; then
                cp "$$f" build/swift/
            fi
        done
        
        # Create tarball
        # Use -C to change directory so paths in tar are relative to 'build'
        # To avoid issues with tar on different platforms, assume standard tar.
        # on mac, tar is bsdtar. on linux, gnutar.
        # -c create, -f file
        tar -cf $@ -C build .
    """,
)
