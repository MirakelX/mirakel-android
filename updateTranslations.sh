#!/bin/bash
java -jar misc/crowdin-cli.jar --config misc/crowdin.yaml download 
rm res/values-af/ res/values-bg/ res/values-ca/ res/values-da/ res/values-el/ res/values-en/ res/values-fi/ res/values-he/ res/values-hu/ res/values-it/ res/values-ja/ res/values-ko/ res/values-pl/ res/values-ro/ res/values-sr/ res/values-sv/ res/values-tr/ res/values-uk/ res/values-vi/ res/values-zh/ -r
