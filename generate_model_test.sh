#!/bin/bash
declare -a models=('model/src/de/azapps/mirakel/model/account/AccountBase.java' 'model/src/de/azapps/mirakel/model/file/FileBase.java' 'model/src/de/azapps/mirakel/model/list/ListBase.java' 'model/src/de/azapps/mirakel/model/recurring/RecurringBase.java' 'model/src/de/azapps/mirakel/model/semantic/SemanticBase.java' 'model/src/de/azapps/mirakel/model/tags/TagBase.java' 'model/src/de/azapps/mirakel/model/task/TaskBase.java')

for i in "${models[@]}"
do
	python3 main/tests/generate_model_tests.py $i
done
