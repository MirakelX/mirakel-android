#!/bin/bash
set -e

java -jar build/misc/crowdin-cli.jar --config build/misc/crowdin.yaml download 
rm */res/values-af/ */res/values-ca/ */res/values-da/ */res/values-el/ */res/values-en/ */res/values-fi/ */res/values-he/ */res/values-hu/  */res/values-ko/ */res/values-ro/ */res/values-sr/ */res/values-sv/ */res/values-tr/ */res/values-uk/ */res/values-vi/ */res/values-zh/ -r

# fix translations
FILES=`ls */res/values-*/*.xml -1 | grep "values-[a-z][a-z]/"`

for f in $FILES
do
    cat $f | build/fixTranslation.py > /tmp/mirakel_puffer
    cat /tmp/mirakel_puffer > $f
done
rm /tmp/mirakel_puffer
 
