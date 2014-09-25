#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

CONFIG="upload.cfg"
UPLOAD_TO_PLAYSTORE="upload_apks.py"

cd $DIR

if [ ! -f $CONFIG ]; then
    echo "Config file not found!"
    echo "Please create the config in build/scripts/$CONFIG"
    exit
fi

source $CONFIG

if [ -z "$APK_DIR" ]; then
	echo "No apk dir set in the config"
	echo "Please set $APK_DIR to point to the directory where the apks are found"
	echo "The path shold be absult or relative to the direcory with the config"
	exit
fi

if [ -z "$MAIN_MANIFEST" ]; then
	echo "No path to main manifest set in the config"
	echo "Please set $MAIN_MANIFEST to point to main/AndroidManifest.xml"
	echo "The path shold be absult or relative to the direcory with the config"
	exit
fi

VERSION=`awk -F'"' '/android:versionName/{print $(NF-1); exit}' $MAIN_MANIFEST`
IS_BETA=0

if [[ $VERSION == *beta* ]]
then
  IS_BETA=1
fi

if [ -n "$WEBPAGE_GIT_DIR" ] && [ -n "$WEBPAGE_APK_NAME" ]; then
	
	APK_FILE="apks/mirakel-$VERSION.apk"
	cp $APK_DIR/$WEBPAGE_APK_NAME $WEBPAGE_GIT_DIR/$APK_FILE
	
	cd $WEBPAGE_GIT_DIR
	git pull
	
	SHA1SUM=`sha1sum $APK_FILE | tr -s " " "\012" |head -n 1`
	DATE=`date +%Y-%m-%d`
	STRING="* $VERSION ($DATE) [Download apk](/$APK_FILE) sha1sum: $SHA1SUM"
	
	if [[ $IS_BETA -eq 1 ]];then
		sed -i "s|## Beta releases|## Beta releases\n$STRING|g" releases.md
	else
		sed -i "s|## Final releases|## Final releases\n$STRING|g" releases.md
	fi
	
	git add $APK_FILE releases.md
	git commit -m "Add mirakel $VERSION"
	git push
	if [ -n "$SSH_USER" ];then
		ssh $SSH_USER@azapps.de ./updatePage.sh
	else
		ssh azapps.de ./updatePage.sh
	fi
	cd $DIR
fi

if [ ! -f $UPLOAD_TO_PLAYSTORE ]; then
    echo "Upload to playstore script not found!"
    echo "Add script as build/$UPLOAD_TO_PLAYSTORE"
    exit
fi




