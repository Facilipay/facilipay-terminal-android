#!/usr/bin/env bash
# Builds and checks everything integrators get, the way CI and publishing do:
#   1. the library's unit tests (including GiftProtocolContractTest) and lint;
#   2. the library into a local Maven repository (build/terminal-sdk-maven), plus its API reference;
#   3. the sample app against that local repository, proving the published artifact works;
#   4. the guide, with the API reference, in strict mode.
#
# Usage: scripts/build.sh <version> [staging|production|none]
#   version      0.0.0-pr for checks, 1.2.3-rc.1 for staging, 1.2.3 for production
#   environment  staging points the guide at the staging site and Maven repository
#
# Needs JDK 17+, an Android SDK (local.properties or ANDROID_HOME) and the guide's Python packages
# (pip install -r docs/requirements.txt). Run from anywhere; it works in the repository root.
set -euo pipefail

version="${1:?usage: scripts/build.sh <version> [staging|production|none]}"
environment="${2:-none}"
cd "$(dirname "$0")/.."
root="$PWD"
repo="$root/build/terminal-sdk-maven"

echo "::group::Library tests and lint"
./gradlew :terminal-sdk:testDebugUnitTest :terminal-sdk:lintDebug --stacktrace
echo "::endgroup::"

echo "::group::Library into a local Maven repository, and its API reference"
rm -rf "$repo"
./gradlew :terminal-sdk:publishReleasePublicationToTerminalSdkRepository dokkaGeneratePublicationHtml \
  -PterminalSdkVersion="$version" -PterminalSdkRepo="$repo" --stacktrace
echo "::endgroup::"

# A separate invocation: the sample must resolve the files just published, not the project.
echo "::group::Sample app against the published library"
./gradlew :sample:assembleDebug -PfacilipayMaven="file://$repo" -PterminalSdkVersion="$version" --stacktrace
insight="$(./gradlew -q :sample:dependencyInsight --dependency io.facilipay:terminal-sdk \
  --configuration debugRuntimeClasspath -PfacilipayMaven="file://$repo" -PterminalSdkVersion="$version")"
if ! grep -qF "io.facilipay:terminal-sdk:$version" <<<"$insight" || grep -qF "project :terminal-sdk" <<<"$insight"; then
  echo "$insight"
  echo "::error::The sample didn't resolve io.facilipay:terminal-sdk:$version from the local Maven repository"
  exit 1
fi
echo "::endgroup::"

echo "::group::Guide"
python3 scripts/check_protocol_docs.py
rm -rf docs/pages/api && cp -R terminal-sdk/build/dokka/html docs/pages/api
# When publishing, the guide shows the version being published. (Checks leave the pages as they are.)
if [[ "$environment" != none ]]; then
  grep -rl 'io.facilipay:terminal-sdk:1.0.0' docs/pages | xargs sed -i.bak "s/io.facilipay:terminal-sdk:1.0.0/io.facilipay:terminal-sdk:$version/g"
fi
if [[ "$environment" == staging ]]; then
  # The staging guide sends integrators to the staging Maven repository and site.
  grep -rl 'https://developers.facilipay.io/maven' docs/pages | xargs sed -i.bak 's#https://developers.facilipay.io/maven#https://dev-developers.facilipay.io/maven#g'
  export TERMINAL_SITE_URL=https://dev-developers.facilipay.io/terminal/
fi
find docs/pages -name '*.bak' -delete
(cd docs && mkdocs build --strict)
echo "::endgroup::"

echo "Built io.facilipay:terminal-sdk:$version into build/terminal-sdk-maven and the guide into docs/site"
