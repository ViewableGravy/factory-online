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

if [[ "$verbose" == true ]]; then
	make -C "$script_dir" run-client
else
	make -s -C "$script_dir" run-client
fi