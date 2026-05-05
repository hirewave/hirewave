#!/bin/bash

for i in $(seq 1 500000); do
  curl -s http://localhost:8080/api/users > /dev/null &
done
wait