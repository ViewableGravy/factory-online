#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd -- "$script_dir/.." && pwd)"

if [[ -z "${JAVA_HOME-}" && -x "$repo_dir/.jdk/jdk-26+35/Contents/Home/bin/java" ]]; then
	export JAVA_HOME="$repo_dir/.jdk/jdk-26+35/Contents/Home"
	export PATH="$JAVA_HOME/bin:$PATH"
fi

verbose=false

if [[ "${1-}" == "-v" ]]; then
	verbose=true
	shift
fi

if [[ "$#" -gt 0 ]]; then
	echo "usage: $0 [-v]" >&2
	exit 1
fi

gradle_args=(--console=plain)

if [[ "$verbose" == true ]]; then
	"$script_dir/gradlew" "${gradle_args[@]}" runServer
else
	"$script_dir/gradlew" "${gradle_args[@]}" -q runServer
fi
