#!/usr/bin/python2
import sys
import os
from airspeed import CachingFileLoader

if len(sys.argv) != 3:
    print(sys.argv[0]+" <taskList.json> <pathToTestDir>")
    sys.exit()

path="de/azapps/mirakel/model/task"
filename="TaskDeserializerTest.java"

vars = {}
vars["JSON_LIST"]=[]
with open(sys.argv[1]) as f:
    for s in f:
        vars["JSON_LIST"].append(s.strip())


loader = CachingFileLoader(".")
template = loader.load_template(os.path.dirname(__file__) + "/model/jsonTemplate.java")


directory=sys.argv[2]+'/src/'+path+'/';
# print("Finish generating Test for "+className)
if not os.path.exists(directory):
    os.makedirs(directory)
f = open(directory+filename, 'w')
f.write(template.merge(vars,loader=loader))
f.close()
