#!/usr/bin/python2.7
import re
import fileinput
import sys



findXmlR = re.compile(ur'(<(?:[^>]*)>)(\s*)(<\!\[CDATA\[(.*)]]>|[^<]*)(\s*)(</(?:[^>]*)>)', re.DOTALL)
escapeR = re.compile(ur'([^\\])((?:\\\\)*)(\')')

def replaceXml(match):
    global escapeR
    (start, whitespace1, inner, _, whitespace2, end) = match.groups()
    newInner = re.sub(escapeR, r"\1\2\\\3",inner)
    return start + whitespace1 + newInner + whitespace2 + end
fin = sys.stdin.read()
sys.stdout.write(re.sub(findXmlR, replaceXml, fin))    
