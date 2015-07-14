#!/bin/bash
set -e

if [ -z "$1" ]
  then
    echo "DPI is missing"
    exit
fi

if [ -z "$2" ]
  then
	OUTPUT=$(pwd)
else
	OUTPUT=$2
fi

if [ -z "$3" ]
  then
  	INPUT=$(pwd)
else
	INPUT=$3
fi

DPI=$1

mkdir -p "${OUTPUT}"/drawable-ldpi
mkdir -p "${OUTPUT}"/drawable-mdpi
mkdir -p "${OUTPUT}"/drawable-hdpi
mkdir -p "${OUTPUT}"/drawable-xhdpi
mkdir -p "${OUTPUT}"/drawable-xxhdpi
mkdir -p "${OUTPUT}"/drawable-xxxhdpi


for f in "${INPUT}"/*.svg; do
	echo "${f}"
	filename=$(basename "$f")
    filename=${filename%.*}"_"$DPI"dp"
	inkscape -z -e "${OUTPUT}"/drawable-ldpi/${filename%.*}.png -w $(echo "$DPI * 0.75"|bc) -h $(echo "$DPI * 0.75"|bc) "${f}"
	inkscape -z -e "${OUTPUT}"/drawable-mdpi/${filename%.*}.png -w $DPI -h $DPI "${f}"
	inkscape -z -e "${OUTPUT}"/drawable-hdpi/${filename%.*}.png -w $(echo "$DPI * 1.5"|bc) -h $(echo "$DPI * 1.5"|bc) "${f}"
	inkscape -z -e "${OUTPUT}"/drawable-xhdpi/${filename%.*}.png -w $(echo "$DPI * 2"|bc) -h $(echo "$DPI * 2"|bc) "${f}"
	inkscape -z -e "${OUTPUT}"/drawable-xxhdpi/${filename%.*}.png -w $(echo "$DPI * 3"|bc) -h $(echo "$DPI * 3"|bc) "${f}"
	inkscape -z -e "${OUTPUT}"/drawable-xxxhdpi/${filename%.*}.png -w $(echo "$DPI * 4"|bc) -h $(echo "$DPI * 4"|bc) "${f}"
	echo ""
done
