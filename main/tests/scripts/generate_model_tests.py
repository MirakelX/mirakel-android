#!/usr/bin/python2

import sys
import os
from helpers import *
import re
from airspeed import CachingFileLoader

if len(sys.argv) != 4:
    print("./model_test.py <Model.java> <ModelBase.java> <pathToTestDir>")
    sys.exit()

filename=sys.argv[1]
basefname=sys.argv[2]

parts=filename.split('/')
directory=sys.argv[3]+"/"
foundSrc=False
for p in parts:
    if not foundSrc and "src"==p:
        foundSrc=True
        directory+="src"
    elif ".java" in p:
        className=p.replace(".java","")
    elif foundSrc:
        directory+="/"+p

vars=getVars(filename)

vars["CREATEFUNCTIONS"]=[]
vars["UPDATEFUNCTIONS"]=[]

create_line=None

for line in open(filename):
    if create_line!=None:
        line=create_line.strip() + line.strip()
    m=re.search(r"public static " + vars["TESTCLASS"] + " (new[a-zA-Z]+)\((.*?)(\) {)?$",line)
    if m!=None:
        (fname,p_strings,bracketEnd)=m.groups()
        if bracketEnd==None:
            create_line=line
        else:
            create_line=None
            params=getParams(p_strings)
            f_ob={
                "function": vars["TESTCLASS"]+ "." +randomFunction(fname,params),
                "name": fname[0].upper() + fname[1:]
            }
            vars["CREATEFUNCTIONS"].append(f_ob)
    m=re.search("public (.*?) String TABLE = \"([a-z_]+)\";",line)
    if m!=None:
        vars["TABLE"] = m.group(2)

for line in open(basefname):
    m=re.search("public void (set[a-zA-Z]+)\((.*)\)",line)
    if m!=None:
        (fname,p_strings)=m.groups()
        params=getParams(p_strings)
        f_ob={
            "function":randomFunction(fname,params),
            "name":fname[0].upper() + fname[1:]
        }
        vars["UPDATEFUNCTIONS"].append(f_ob)
        
vars["GETALL_FUNCTION"]="all()"
vars["ID_TYPE"]="int"

# Exceptions
if className=="SpecialList":
    vars["GETALL_FUNCTION"]="allSpecial()"
if className=="Task":
    vars["ID_TYPE"]="long"

loader = CachingFileLoader(".")
template = loader.load_template(os.path.dirname(__file__) + "/model/base.java")




if not os.path.exists(directory):
    os.makedirs(directory)
f = open(directory+"/"+className+'Test.java', 'w')
f.write(template.merge(vars,loader=loader))
f.close()
