#!/bin/bash
# Apply Bidhata-Q5 source patches. Run from the source root after repo sync.
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
for p in $(cd "$HERE" && find . -name '*.patch' | sort); do
  proj=$(dirname "$p" | sed 's#^\./##')
  if git -C "$proj" apply --check "$HERE/$p" 2>/dev/null; then
    git -C "$proj" apply "$HERE/$p" && echo "applied $p"
  elif git -C "$proj" apply --reverse --check "$HERE/$p" 2>/dev/null; then
    echo "already applied $p"
  else
    echo "FAILED $p" >&2; exit 1
  fi
done
