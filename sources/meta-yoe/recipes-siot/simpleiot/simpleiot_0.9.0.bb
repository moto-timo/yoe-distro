# Copyright (C) 2020 Cliff Brake <cbrake@bec-systems.com>
# Released under the MIT license (see COPYING.MIT for the terms)

SUMMARY = "Simple IOT Framework application"
HOMEPAGE = "http://simpleiot.org"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"
SECTION = "apps"

DEPENDS = "go-native elm-binary-native nodejs-native"

inherit systemd update-rc.d goarch

SRCREV = "cbd19256e302476de1c963760a70b27ac8acafa1"

BRANCH ?= "master"

SRC_URI = "git://github.com/simpleiot/simpleiot;branch=${BRANCH};protocol=https \
           "
S = "${WORKDIR}/git"

GOPATH = "${TMPDIR}/work-shared/go"

CGO_ENABLED:x86-64 = "0"
export CGO_ENABLED

INITSCRIPT_NAME = "siot"
INITSCRIPT_PARAMS = "start 99 5 . stop 20 6 ."

do_configure[network] = "1"
do_compile[network] = "1"

do_configure() {
    export GOPATH=${GOPATH}
    export GOFLAGS="-modcacherw"
    go install github.com/benbjohnson/genesis/...
}

do_compile() {
    export GOPATH=${GOPATH}
    export GOARCH=${TARGET_GOARCH}
    export PATH=${GOPATH}/bin:$PATH
    export GOFLAGS="-modcacherw"
    export CXX="${BUILD_CXX}"
    export CXXFLAGS="${BUILD_CXXFLAGS}"
    . ${S}/envsetup.sh
    # FIXME: get elm cache in ~/.elm moved to work-shared
    rm -rf frontend/elm-stuff
    siot_setup
    siot_build
}

do_install() {
    install -D -m 0755 ${S}/siot ${D}${bindir}/siot
    if ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'true', 'false', d)}; then
        install -D -m 0644 ${S}/contrib/siot.service ${D}${systemd_unitdir}/system/siot.service
    else
        install -D -m 0755 ${S}/contrib/siot.init ${D}${sysconfdir}/init.d/siot
    fi
}

SYSTEMD_SERVICE:${PN} = "siot.service"

INSANE_SKIP:${PN} += "ldflags already-stripped"

# we use sqlite and not all architectures seem to be supported see
# https://pkg.go.dev/modernc.org/sqlite#hdr-Supported_platforms_and_architectures
COMPATIBLE_HOST = "(x86_64.*|i.86.*|arm.*|aarch64.*|riscv64)-(linux)"
