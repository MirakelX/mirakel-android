#!/usr/bin/python3
import re


def getVars(filename):
    vars = {}
    for line in open(filename):
        m=re.search("^package ([a-zA-Z-_\.]+);$",line)
        if m!=None:
            vars["FULLPACKAGE"] = m.group(1)
        m=re.search("^public class ([a-zA-Z]+)",line)
        if m!=None:
            vars["TESTCLASS"] = m.group(1)
    return vars

def getParams(p_string):
    parar = p_string.replace("final","").split(",")
    params = []
    isMap=False
    for i in range(len(parar)):
        p=parar[i].strip()
        if "<" in p:
            isMap=True
            typeName=""
            while not ">" in p and i<len(parar):
                typeName+=p.strip()
                i+=1
                p=parar[i]
            parts=p.split("> ")
            pd = {"type":typeName+","+parts[0].strip(),"name":parts[1].strip()}
        elif isMap and ">" in p:
            isMap=False
        elif not isMap:
            parts=p.split(' ');
            pd = {"type":parts[0].strip(),"name":parts[1].strip()}
        if not isMap:
            params.append(pd)
    return params

def randomFunction(fname,params):
    p_strings=[]
    for param in params:
        paramtype=param["type"].replace("<","_").replace(",","_").replace(">","");
        p_strings.append("RandomHelper.getRandom" + paramtype +"()")
    p_string=", ".join(p_strings)
    return fname +"(" + p_string + ")"

