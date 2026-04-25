#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
java_dir="$(cd -- "$script_dir/.." && pwd)"

if ! command -v script >/dev/null 2>&1; then
	echo "error: 'script' command is required for pseudo-terminal delay verification" >&2
	exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

run_scenario() {
	local output_file="$1"
	local input_bytes="$2"

	(
		cd "$java_dir"
		printf '%b' "$input_bytes" | script -qefc './run-server.sh' "$output_file" >/dev/null
	)
}

require_line() {
	local pattern="$1"
	local file_path="$2"
	local message="$3"

	if ! grep -Eq "$pattern" "$file_path"; then
		echo "FAIL: $message" >&2
		exit 1
	fi
}

extract_server_tick_before_client_tick() {
	local client_tick="$1"
	local file_path="$2"
	local client_line

	client_line="$(grep -n "\[client\] Running batch of 1 simulations on tick $client_tick" "$file_path" | head -n 1 | cut -d: -f1)"
	if [[ -z "$client_line" ]]; then
		return 0
	fi

	awk -v max_line="$client_line" '
		NR < max_line && /server-SimulationThread/ && /\[name: Simulation 1\]/ {
			tick_line = $0
			sub(/^.*\[tick: /, "", tick_line)
			sub(/\].*$/, "", tick_line)
			last_server_tick = tick_line
		}
		END {
			print last_server_tick
		}
	' "$file_path"
}

startup_log="$tmp_dir/startup.log"
augment_log="$tmp_dir/augment.log"

run_scenario "$startup_log" '\r\r\r\r\r\r\rexit\r'
run_scenario "$augment_log" '\e[A\r\r\r\r\r\r\r\r\r\r\r\rexit\r'

require_line 'Server accepted join from client-1 for Simulation 1 at current server tick 0' "$startup_log" \
	"join request should be handled immediately at startup"
require_line 'Client client-1 attached Simulation 1 at snapshot tick 0 with startup buffer 4' "$startup_log" \
	"initial snapshot should arrive after the 2-tick transport delay"

startup_first_client_server_tick="$(extract_server_tick_before_client_tick 1 "$startup_log")"
if [[ "$startup_first_client_server_tick" != "6" ]]; then
	echo "FAIL: expected first client simulation tick to run after server tick 6, got '${startup_first_client_server_tick:-missing}'" >&2
	exit 1
fi

require_line 'Client client-1 sent input request for Simulation 1 on transport tick 1' "$augment_log" \
	"augment scenario should send the first input on transport tick 1"
require_line 'Server applied input from client-1 for tick 3 on Simulation 1' "$augment_log" \
	"server should apply the augment after the 2-tick transport delay"
require_line 'Client client-1 queued update for tick 3 on Simulation 1' "$augment_log" \
	"client should receive the replicated update two ticks after server application"
require_line 'client-SimulationThread-1 \[name: Simulation 1\] ran on \[tick: 2\] \[value: 2\]' "$augment_log" \
	"client tick 2 should still reflect the unaugmented state"
require_line 'client-SimulationThread-1 \[name: Simulation 1\] ran on \[tick: 3\] \[value: 4\]' "$augment_log" \
	"client should apply the queued augment when local tick 3 is simulated"

echo "PASS: startup and augment delay regressions verified"