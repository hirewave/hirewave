#!/bin/bash

# set variables for clarity
JMX=/home/andrei/Desktop/hirewave/src/test/java/ro/unibuc/prodeng/jmeter/freelancers_tests.jmx
JMETER=/home/andrei/Downloads/apache-jmeter-5.6.3/bin/jmeter
LOGDIR="$(dirname "$JMX")"
OUTDIR="$LOGDIR/freelancer_logs"
REPORT_DIR="$OUTDIR/report"

mkdir -p "$OUTDIR"
rm -rf "$REPORT_DIR"

"$JMETER" -n -t "$JMX" \
  -l "$OUTDIR/jmeter_results.jtl" \
  -j "$OUTDIR/jmeter.log" \
  -e -o "$REPORT_DIR"