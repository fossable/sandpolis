#!/bin/bash

match=$(grep --text --line-number '^PAYLOAD:$' $0 | cut -d ':' -f 1)
payload_start=$((match + 1))

tail -n +$payload_start $0 | echo
exit 0

PAYLOAD:
test