#! /bin/bash

if [ $# -eq 0 ]; then
    echo "No arguments provided."
    exit 1
fi
 
for i in node0{1..4}
do
    echo --------- $i ----------
    ssh $i "$*"
done

