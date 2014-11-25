#!/usr/bin/python2

import sys
import os
from helpers import *
import re
from airspeed import CachingFileLoader

if len(sys.argv) != 3:
    print sys.argv[0] + "<Model.java> <pathToTestDir>"
    sys.exit()

filenameModel=sys.argv[1]


parts=filenameModel.split('/')[:-1]
directory=sys.argv[2]+"/"
foundSrc=False
for p in parts:
    if not foundSrc and "src"==p:
        foundSrc=True
        directory+="src"
    elif foundSrc:
        directory+="/"+p
className = None
modelName = None

classRegex = re.compile("public class (\S+) extends (\S+)")
with open(filenameModel) as f:
    for line in f:
        classM = classRegex.search(line)
        if classM!=None:
            (modelName, className) = classM.groups()
            break
filename = filenameModel.replace(modelName,className)




vars = getVars(filename)
vars["GETTERS"] = []
vars["SETTERS"] = []
vars["IMPORTS"] = []
vars["CONSTRUCTORS"] = []
vars["MODELNAME"] = modelName


create_line=None
getterRegex = re.compile("public\s*(@NonNull)?(\s+)(\S+)\s+get[a-zA-Z]+" + paramRegexS + "\s+" + throwsRegexS + "\s*{")
isRegex = re.compile("public\s*(@NonNull)?(\s+)(\S+)\s+is[a-zA-Z]+" + paramRegexS + "\s+" + throwsRegexS + "\s*{")
setterRegex = re.compile("public void set([a-zA-Z]+)" + paramRegexS + "\s+" + throwsRegexS + "\s*{")
importRegex = re.compile(importRegexS)
constructorRegex = re.compile("public " + modelName + paramRegexS + "\s+" + throwsRegexS + "\s*({)?")
with open(filename) as f:
    for line in f:
        if create_line != None:
            line = create_line.strip() + line.strip()

        getterM = getterRegex.search(line)
        if getterM != None:
            groups = getterM.groups()
            isNonNull = groups[0] == "@NonNull"
            name = groups[1]
            type = groups[2]
            params = getParams(groups[3])
            vars["GETTERS"].append({"name": name,"params":params,"type":type})

        isM = getterRegex.search(line)
        if isM != None:
            groups = isM.groups()
            isNonNull = groups[0] == "@NonNull"
            name = groups[1]
            type = groups[2]
            params = getParams(groups[3])
            vars["GETTERS"].append({"name": name,"params":params,"type":type})

        setterM = setterRegex.search(line)
        if setterM != None:
            groups = setterM.groups()
            name = groups[0]
            params = getParams(groups[1])
            ptype = params[0]
            getterFunction = ""
            if ptype["type"] == "boolean":
                getterFunction = "is" + name + "()"
            else:
                getterFunction = "get" + name + "()"
            vars["SETTERS"].append({
                "name": name,
                "params":params,
                "type": ptype["type"].replace("@Nullable","").replace("@NonNull",""),
                "setterFunction":randomFunction("set"+name,params),
                "randomFunction": getRandom(params[0]),
                "getterFunction": getterFunction
            })

        importM = importRegex.search(line)
        if importM != None:
            groups = importM.groups()
            vars["IMPORTS"].append(groups[0])

create_line  = None
loader = CachingFileLoader(".")
template = loader.load_template(os.path.dirname(__file__) + "/templates/modelBase.java")

if not os.path.exists(directory):
    os.makedirs(directory)
f = open(directory+"/"+className+'Test.java', 'w')
f.write(template.merge(vars,loader=loader))
f.close()
