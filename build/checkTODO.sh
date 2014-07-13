#!/bin/bash
export TODOS=`ag --color --nobreak TODO $PWD`
if [ -z "$TODOS" ]; then
    exit
else
    printf "$TODOS"
    exit 1
fi
