#!/bin/bash

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
astyle --options=${DIR}/style.options -v --recursive "${DIR}/../*java"
