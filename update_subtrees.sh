#!/bin/sh
source ./.subrepos
source ./.localconfig # declare user
EDITOR=/bin/true

for repo in ${repos[@]} ; do
    echo "Pulling "$repo
    git subtree pull --prefix=$repo ssh://$user@gerrit.azapps.de:29418/mirakel-android/$repo master
done

#cp buildfiles
cp build/build.gradle .
cp build/settings.gradle .
