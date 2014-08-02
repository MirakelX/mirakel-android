#!/usr/bin/python2

import sys
import os


def split(line,isGetter):
    line=line.replace("final ","")
    if isGetter:
        line=line.replace("()","")
        if "get" in line:
            prefix="get"
            line=line.replace("get","")
        else:
            prefix="is"
            line=line.replace("is","")
    else:
        prefix="set"
        line=line.replace("set","")
    parts=line.split(' ')
    if isGetter:
        t=parts[1]
    else:
        p=parts[2].split('(')
        t=p[1]
        parts[2]=p[0]
    name=parts[2]
    functionName=prefix+name
    return ((t,name),functionName)
    
def parseConstructorList(s):
    # print(s)
    parameterList=s[s.find("(")+1:s.find(")")].split(',')
    first=True
    ret=""
    for param in parameterList:
        if not param.strip():
            break
        if not first:
            ret+=","
        else:
            first=False
        typeName=param.replace("final","").strip().split(' ')[0]
        ret+="RandomHelper.getRandom"+typeName.strip()+"()"

    # print(ret)
    return ret


if len(sys.argv)!=3:
    print sys.argv[0]+"<pathToClass> <pathToTestDir>" 
    sys.exit()
getter={}
setter={}
imports=""
path=str(sys.argv[1]).split('/')
classBaseName=path[len(path)-1].replace(".java","")
if os.path.isfile(sys.argv[1].replace(classBaseName,classBaseName.replace("Base",""))):
    className=classBaseName.replace("Base","")
elif os.path.isfile(sys.argv[1].replace(classBaseName,classBaseName.replace("Base","Mirakel"))):
    className=classBaseName.replace("Base","Mirakel")
    
constructor=""
constructorState=-1;
with open(sys.argv[1]) as f:
    for s in f:
        s=s.strip()
        if "public" in s:
            if "set" in s and "(" in s and ")" in s:
                #print s
                res=split(s,False)
                setter[res[0]]=res[1]
            elif ("get" in s or "is" in s) and "(" in s and ")" in s:
                #print s
                res=split(s,True)
                getter[res[0]]=res[1]
        elif "import" in s:
            imports+=s+"\n";
path[len(path)-1]=""
isSrc=False
fullClass=""
testPath=""

with open(sys.argv[1].replace(classBaseName,className)) as f:
    for s in f:
        s=s.replace("public","")
        s=s.strip()
        if s.startswith(className+"(") and constructorState==-1 and not "Cursor" in s:
            constructor+=s
            constructorState=0;
        elif constructorState==0:
            constructor+=s
        if ")" in s and constructorState==0:
            constructorState=1

## print(str(getter))
## print(setter)
for p in path:
    if isSrc and p!="":
        if fullClass!="":
            fullClass+="."+p
            testPath+="/"+p
        else:
            fullClass=p
            testPath=p
    elif "src" in p:
        isSrc=True

getTestClass="\t\tfinal "+className+" obj= new "+className+"("+parseConstructorList(constructor)+");\n"

# print(getTestClass)

output="/*******************************************************************************\n"
output+=" * Mirakel is an Android App for managing your ToDo-Lists\n"
output+=" * \n"
output+=" * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.\n"
output+=" * \n"
output+=" *     This program is free software: you can redistribute it and/or modify\n"
output+=" *     it under the terms of the GNU General Public License as published by\n"
output+=" *     the Free Software Foundation, either version 3 of the License, or\n"
output+=" *     any later version.\n"
output+=" * \n"
output+=" *     This program is distributed in the hope that it will be useful,\n"
output+=" *     but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
output+=" *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
output+=" *     GNU General Public License for more details.\n"
output+=" * \n"
output+=" *     You should have received a copy of the GNU General Public License\n"
output+=" *     along with this program.  If not, see <http://www.gnu.org/licenses/>.\n"
output+=" ******************************************************************************/\n"

output+="package "+fullClass+";\n\n"
output+="//Testcases for "+className+"\n"
output+="import "+fullClass+"."+className+";\n\n"
output+=imports+"\n\n"

output+="import de.azapps.mirakelandroid.test.RandomHelper;\n"
output+="import de.azapps.mirakelandroid.test.MirakelTestCase;\n"
output+="import junit.framework.TestCase;\n\n"
output+="import android.test.suitebuilder.annotation.SmallTest;\n"

output+="public class "+classBaseName+"Test extends MirakelTestCase {\n\n"

output+="\t@Override\n"
output+="\tprotected void setUp() throws Exception {\n"
output+="\t\tsuper.setUp();\n"
output+="\t\tRandomHelper.init(getContext());\n\t}\n"

for s in setter:
    if s in getter:
        test="\t//Test for getting and setting "+s[1]+"\n"
        test+="\t@SmallTest\n"
        test+="\tpublic void test"+s[1]+"() {\n"
        test+=getTestClass
        if s[1]=="Priority":
            test+="\t\tfinal "+s[0]+" t=RandomHelper.getRandomPriority();\n"
        else:
            test+="\t\tfinal "+s[0]+" t=RandomHelper.getRandom"+s[0]+"();\n"
        test+="\t\tobj."+setter[s]+"(t);\n";
        test+="\t\tassertEquals(\"Getting and setting "+s[1]+" does not match\",t,obj."+getter[s]+"());\n\t}"
        output+=test+"\n\n"

      #  print setter[s]
      #  print getter[s]

output+="}"

directory=sys.argv[2]+'/src/'+testPath+'/';
# print("Finish generating Test for "+className)
if not os.path.exists(directory):
    os.makedirs(directory)
f = open(directory+classBaseName+'Test.java', 'w')
f.write(output)
f.close()
