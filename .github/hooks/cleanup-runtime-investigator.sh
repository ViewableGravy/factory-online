#!/usr/bin/env bash
set -euo pipefail

# Cleanup script for runtime-behavior-investigator artifacts.
# Usage: cleanup-runtime-investigator.sh [manifest-path]
# Manifest should contain one repo-relative path per line.

manifest="${1:-.github/hooks/runtime-investigator.created}"
repo_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

if [ ! -f "$manifest" ]; then
  echo "Manifest not found: $manifest"
  exit 0
fi

while IFS= read -r rel || [ -n "$rel" ]; do
  # skip empty lines and comments
  case "$rel" in
    ""|#*) continue ;;
  esac

  # Normalize path and ensure it's repo-relative
  target="$repo_root/$rel"
  case "$target" in
    "$repo_root"/*) ;;
    *) echo "Skipping unsafe path: $rel"; continue ;;
  esac

  if [ ! -e "$target" ]; then
    echo "Missing (already removed?): $rel"
    continue
  fi

  # Skip files tracked by git (protect versioned files)
  if git ls-files --error-unmatch "$rel" >/dev/null 2>&1; then
    echo "Skipping tracked file: $rel"
    continue
  fi

  rm -rf "$target" && echo "Removed: $rel"
done < "$manifest"

# Remove manifest after cleanup
rm -f "$manifest"
echo "Cleanup complete."
