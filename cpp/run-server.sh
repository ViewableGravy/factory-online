#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
has_cmake="$(command -v cmake >/dev/null 2>&1 && echo "true" || echo "false")"

if [[ "$has_cmake" != "true" ]]; then
  echo "CMake not found, installing CMake"

  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    sudo apt-get update
    sudo apt-get install -y cmake
  elif [[ "$OSTYPE" == "darwin"* ]]; then
    brew install cmake
  else
    echo "Unsupported OS: $OSTYPE"
    exit 1
  fi

else
  echo "CMake found, using CMake presets"
fi

# if [[ "$has_cmake" == "true" ]]; then
cmake --preset debug -S "$script_dir" -B "$script_dir/build/debug"
cmake --build --preset debug
# else
# 	make -C "$script_dir" run
# 	exit 0
# fi

"$script_dir/build/debug/apps/server/factory_server"