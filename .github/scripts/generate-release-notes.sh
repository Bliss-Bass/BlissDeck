#!/usr/bin/env bash
# Writes GitHub Release notes markdown to stdout.
# Usage: generate-release-notes.sh <tag>   e.g. v0.1.0
set -euo pipefail

TAG="${1:?Usage: generate-release-notes.sh <tag>}"
REPO="${GITHUB_REPOSITORY:-Bliss-Bass/BlissDeck}"
INTRO_FILE=".github/release/RELEASE_INTRO.md"
GRADLE_FILE="app/build.gradle.kts"

if [ ! -f "$INTRO_FILE" ]; then
  echo "::error::Missing $INTRO_FILE" >&2
  exit 1
fi

MIN_SDK="$(grep -E 'minSdk\s*=' "$GRADLE_FILE" | head -1 | grep -Eo '[0-9]+' || echo "26")"
VERSION_NAME="$(grep -E 'versionName\s*=' "$GRADLE_FILE" | head -1 | sed -E 's/.*"([^"]+)".*/\1/' || echo "${TAG#v}")"
VERSION_CODE="$(grep -E 'versionCode\s*=' "$GRADLE_FILE" | head -1 | grep -Eo '[0-9]+' || echo "?")"

PREV_TAG=""
if git rev-parse "$TAG" >/dev/null 2>&1; then
  PREV_TAG="$(git tag -l 'v*' --sort=-version:refname | grep -Fxv "$TAG" | head -1 || true)"
fi

COMMIT_COUNT="$(git log --since="30 days ago" --oneline --no-merges 2>/dev/null | wc -l | tr -d ' ')"
SINCE_DATE="$(date -u -d '30 days ago' '+%Y-%m-%d' 2>/dev/null || date -u -v-30d '+%Y-%m-%d' 2>/dev/null || echo '30 days ago')"

CHANGELOG="$(
  git log --since="30 days ago" --pretty=format:"- %s (\`%h\`)" --no-merges 2>/dev/null \
    | sed 's/Co-authored-by: Cursor *$//' \
    | sed '/^$/d' \
    || true
)"
if [ -z "$CHANGELOG" ]; then
  CHANGELOG="- No commits in the last 30 days."
fi

apk_size() { [ -f "$1" ] && du -h "$1" | cut -f1 || true; }
RELEASE_APK_SIZE="$(apk_size app/build/outputs/apk/release/app-release.apk)"
DEBUG_APK_SIZE="$(apk_size app/build/outputs/apk/debug/app-debug.apk)"

{
  echo "# BlissDeck ${TAG#v}"
  echo ""
  cat "$INTRO_FILE"
  echo ""
  echo "## What's included"
  echo ""
  echo "| File | Description |"
  echo "|------|-------------|"
  echo "| **app-release.apk**${RELEASE_APK_SIZE:+ (~$RELEASE_APK_SIZE)} | Signed release build. Use for installs and in-place updates. |"
  echo "| **app-debug.apk**${DEBUG_APK_SIZE:+ (~$DEBUG_APK_SIZE)} | Debug build with logging enabled. For testing only. |"
  echo ""
  echo "### Automatic updates (Obtainium)"
  echo ""
  echo "To get notified when new \`v*\` tags ship, add this repo in [Obtainium](https://github.com/ImranR98/Obtainium):"
  echo ""
  echo "| Setting | Value |"
  echo "|---------|-------|"
  echo "| **Source** | GitHub |"
  echo "| **Repository** | \`${REPO}\` |"
  echo "| **Release filter** | \`v*\` tags |"
  echo "| **APK filter** | \`app-release.apk\` |"
  echo ""
  echo "### Build info"
  echo ""
  echo "| Field | Value |"
  echo "|-------|-------|"
  echo "| **Release tag** | \`${TAG}\` |"
  echo "| **Gradle version** | \`${VERSION_NAME}\` (code ${VERSION_CODE}) |"
  echo "| **Application ID** | \`org.gamelauncher\` |"
  echo "| **Minimum Android** | API ${MIN_SDK}+ |"
  echo "| **Ideal form factor** | Landscape tablet, Bliss OS, Android-x86, Waydroid |"
  echo ""

  if [ -n "$PREV_TAG" ]; then
    echo "## Since ${PREV_TAG}"
    echo ""
    echo "Compare on GitHub: [${PREV_TAG}...${TAG}](https://github.com/${REPO}/compare/${PREV_TAG}...${TAG})"
    echo ""
  fi

  echo "## Changes in the last 30 days"
  echo ""
  echo "_Since ${SINCE_DATE} · ${COMMIT_COUNT} commits_"
  echo ""
  echo "$CHANGELOG"
  echo ""
  echo "---"
  echo ""
  echo "_Built from [\`${TAG}\`](https://github.com/${REPO}/releases/tag/${TAG}) · [All commits](https://github.com/${REPO}/commits/${TAG})_"
}
