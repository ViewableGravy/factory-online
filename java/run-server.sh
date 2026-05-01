#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd -- "$script_dir/.." && pwd)"

shopt -s nullglob
jdk_candidates=(
	"$repo_dir/.jdk/jdk-26+35/Contents/Home"
	"$repo_dir/.jdk"/jdk-17*
	"$HOME/.local/jdks"/jdk-17*
	"/usr/lib/jvm/java-17-openjdk-amd64"
)
shopt -u nullglob

for jdk_home in "${jdk_candidates[@]}"; do
	if [[ -x "$jdk_home/bin/java" ]]; then
		export JAVA_HOME="$jdk_home"
		export PATH="$JAVA_HOME/bin:$PATH"
		break
	fi
done

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
