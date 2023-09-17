#!/bin/bash

echo -n "Building PRISM..."
cd ~/3yp/prism-games/prism
make &> /dev/null
cd - > /dev/null
echo " build finished with exit code $?."

rm -f data.csv
echo "length,construction time (s),verification time (s),total mem (mb),states,transitions" > data.csv

for i in {2..25}
do
    python3 generator.py $i
    echo "Computing for model size $i..."
    # data=$(~/3yp/prism-games/prism/bin/prism out.emdp out.props | grep -o -P "(?<=iterations and ).*(?=second)")
    data=$(~/3yp/prism-games/prism/bin/prism out.emdp out.props)
    construction_time=$(echo "$data" | grep -o -P "(?<=construction done in ).*(?= secs.)")
    verification_time=$(echo "$data" | grep -o -P "(?<=iterations and ).*(?= seconds)")
    total_mem=$(echo "$data" | grep -o -P "(?<=extents in MB: ).*")
    states=$(echo "$data" | grep -o -P "(?<=reachable states... ).*(?= states)")
    transitions=$(echo "$data" | grep -o -P "(?<=Transitions: ).*")
    echo "$i,$construction_time,$verification_time,$total_mem,$states,$transitions" >> data.csv
done
