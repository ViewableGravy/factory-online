#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

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
	"$script_dir/gradlew" "${gradle_args[@]}" runClient
else
	"$script_dir/gradlew" "${gradle_args[@]}" -q runClient
fi