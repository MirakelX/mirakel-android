#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

CONFIG="upload.cfg"
SECRET="googleapi_secret.json"
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
TRACK="production"

if [[ $VERSION == *beta* ]]
then
    IS_BETA=1
    TRACK="beta"
fi

if [ -n "$WEBPAGE_GIT_DIR" ] && [ -n "$WEBPAGE_APK_NAME" ] && [- n "$CHANGELOG"]; then
	
	APK_FILE="apks/mirakel-$VERSION.apk"
	
	cd $WEBPAGE_GIT_DIR
	git pull
	
	cp $APK_DIR/$WEBPAGE_APK_NAME $WEBPAGE_GIT_DIR/$APK_FILE
	cp $CHANGELOG $WEBPAGE_GIT_DIR/changelog.xml
	
	xsltproc updateChangelog.xslt changelog.xml >changelog.md
	
	SHA1SUM=`sha1sum $APK_FILE | tr -s " " "\012" |head -n 1`
	DATE=`date +%Y-%m-%d`
	STRING="* $VERSION ($DATE) [Download apk](/$APK_FILE) sha1sum: $SHA1SUM"
	
	if [[ $IS_BETA -eq 1 ]];then
		sed -i "s|## Beta releases|## Beta releases\n$STRING|g" releases.md
	else
		sed -i "s|## Final releases|## Final releases\n$STRING|g" releases.md
	fi
	
	git add $APK_FILE releases.md changelog.xml changelog.md
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
    echo "Add script as build/scripts/$UPLOAD_TO_PLAYSTORE"
    exit
fi

if [ -n $PLAY_APK_NAME ]; then
  $(./$UPLOAD_TO_PLAYSTORE -p de.azapps.mirakelandroid -s $SECRET -t $TRACK -a $APK_DIR/$PLAY_APK_NAME)
fi


