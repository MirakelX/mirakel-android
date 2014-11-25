#!/usr/bin/python2
import re

def getVars(filename):
    vars = {}
    for line in open(filename):
        m=re.search("^package ([a-zA-Z-_\.]+);$",line)
        if m!=None:
            vars["FULLPACKAGE"] = m.group(1)
        m=re.search("^(public)?\s*(abstract)?\s*class ([a-zA-Z]+)",line)
        if m!=None:
            groups = m.groups()
            vars["TESTCLASS"] = groups[-1].strip()
    return vars

'''more beautiful but not working :( (Pumping lemma)
def getParams(p_string):
    paramRegex = re.compile("((final |@NonNull )*)(\S+) ([a-zA-Z]+),?")
    params = []
    for match in re.findall(paramRegex,p_string):
        (nonNullString,finalString, ptype, name) = match
        nonNull = "@NonNull"in nonNullString
        params.append({"type":ptype, "name": name, "nonNull": nonNull,})
    return params
'''

def getParams(p_string):
    if len(p_string) == 0:
        return []
    params = []
    parar = p_string.replace("final","").split(",")
    countParens = 0
    curr = ""
    for par in parar:
        if "<" in par and not ">" in par:
            countParens+=1
            if len(curr) > 0:
                curr += ","
            curr += par
        if ">" in par and not "<" in par:
            countParens-=1
            curr += "," + par
        if countParens == 0 and len(curr)==0:
            curr = par
        if countParens == 0:
            params.append(curr.strip())
            curr=""
    return map(parseParam,params)

def parseParam(param):
    regex = re.compile("(@NonNull)?\s*(.*) ([a-zA-Z]+)")
    m = regex.search(param)
    groups = m.groups()
    (nonNull, pType, name) = m.groups()
    return {"type":pType, "name": name, "nonNull": nonNull != None}

def randomFunction(fname,params):
    p_strings = map(getRandom, params)
    p_string=", ".join(p_strings)
    return fname +"(" + p_string + ")"

def getRandom(param):
    paramtype=param["type"].replace("<","_").replace(",","_").replace(">","").replace("@","").replace(" ","_");
    if param["name"]=="priority":
        paramtype="Priority"
    return "RandomHelper.getRandom" + paramtype +"()"

paramRegexS = "\(([^\)]*)(\))?"
throwsRegexS = "(throws\s*[a-zA-Z]+)?"
importRegexS = "^import ([a-zA-Z\.]+);$"
