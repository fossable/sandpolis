#!/bin/bash
for filename in *.svg; do
	f=`basename -s .svg $filename`
	inkscape "$filename" --export-png="$f"@2x.png -w66 -h88
done
