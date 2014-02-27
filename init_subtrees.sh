#!/bin/sh
source ./.subrepos

for repo in ${repos[@]} ; do
    echo "Pulling "$repo
    git subtree add --prefix=$repo ssh://az@gerrit.azapps.de:29418/mirakel-android/$repo master
done
