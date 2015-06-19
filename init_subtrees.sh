#!/bin/sh
source ./.subrepos
if [ -f ./.localconfig ]; then
    source ./.localconfig
else
    echo "Declare \$user variable in .localconfig first" >&2
    exit 1
fi

for repo in ${repos[@]} ; do
    echo "Pulling "$repo
    git subtree add --prefix=$repo ssh://$user@gerrit.azapps.de:29418/mirakel-android/$repo master
done

for repo in ${extra_repos[@]} ; do
    echo "Pulling "$repo
    git subtree add --prefix=$repo ssh://$user@gerrit.azapps.de:29418/$repo master
done
