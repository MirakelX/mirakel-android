#!/bin/sh
source ./.subrepos
source ./.localconfig # declare user
export EDITOR=/bin/true
export GIT_EDITOR=/bin/true
export VISUAL=/bin/true
export EDITOR=/bin/true

for repo in ${repos[@]} ; do
    echo "Pulling "$repo
    git subtree pull --prefix=$repo ssh://$user@gerrit.azapps.de:29418/mirakel-android/$repo master 
done

#cp buildfiles
cp build/build.gradle .
cp build/settings.gradle .

if [ -n "$(git status --porcelain)" ]; then 
  git add build.gradle settings.gradle;
  git commit -m "Update buildfiles" 
fi

cp main/README.md .
if [ -n "$(git status --porcelain)" ]; then
  git add README.md;
  git commit -m "Update Readme"
fi

git push --force
