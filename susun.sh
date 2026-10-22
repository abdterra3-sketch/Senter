#!/bin/bash
# Menyusun file datar di root menjadi struktur proyek Android.
set -e
mkdir -p app/src/main/res/layout app/src/main/res/values app/src/main/res/mipmap-xxxhdpi app/src/main/assets

# salin dirinya ke assets (dipakai aplikasi ADJM Blok)
cp susun.sh app/src/main/assets/susun.sh

if [ -f app-build.gradle.kts ]; then mv app-build.gradle.kts app/build.gradle.kts; fi
if [ -f AndroidManifest.xml ]; then mv AndroidManifest.xml app/src/main/AndroidManifest.xml; fi

for f in *.kt *.java; do
  if [ -f "$f" ]; then
    pkg=$(grep -m1 '^package ' "$f" | sed 's/package \([A-Za-z0-9_.]*\).*/\1/')
    d="app/src/main/java/$(echo "$pkg" | tr . /)"
    mkdir -p "$d"
    mv "$f" "$d/"
  fi
done

for f in *.xml; do
  if [ -f "$f" ]; then
    case "$f" in
      strings*|colors*|styles*|themes*|dimens*) mv "$f" app/src/main/res/values/ ;;
      *) mv "$f" app/src/main/res/layout/ ;;
    esac
  fi
done

for f in ic_launcher.*; do
  if [ -f "$f" ]; then mv "$f" app/src/main/res/mipmap-xxxhdpi/; fi
done

for f in aset_*; do
  if [ -f "$f" ]; then mv "$f" "app/src/main/assets/${f#aset_}"; fi
done

echo "Struktur proyek selesai disusun."
