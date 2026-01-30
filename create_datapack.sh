#!/usr/bin/env bash
set -euo pipefail

BUILD_DIR="build/meteorbow_datapack"
ZIP_PATH="build/meteorbow_datapack.zip"

rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}"

cp pack.mcmeta "${BUILD_DIR}/"
cp -R data "${BUILD_DIR}/"

rm -f "${ZIP_PATH}"
(
  cd "${BUILD_DIR}"
  zip -r "../meteorbow_datapack.zip" pack.mcmeta data
)
