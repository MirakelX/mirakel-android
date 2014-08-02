#!/bin/bash

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

if hash astyle 2>/dev/null; then
	astyle --options=${DIR}/style.options -v --recursive "/${DIR}/../*java" --exclude="scripts" >/dev/null 
fi
