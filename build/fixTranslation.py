#!/usr/bin/python2.7
import re
import fileinput
import sys
import lxml.etree as et
fin = sys.stdin.read()
parser = et.XMLParser(strip_cdata=False,remove_comments=False)
doc = et.fromstring(fin, parser)

escapeR = re.compile(ur'([^\\])((?:\\\\)*)(\')')
for elem in doc.xpath('//string'):
    if elem.text:
        newtext = re.sub(escapeR, r"\1\2\\\3",elem.text)
        if newtext.find("\n") != -1:
            elem.text = et.CDATA(newtext.strip())
        else:
            elem.text = newtext
print """<?xml version="1.0" encoding="utf-8"?>
<!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  ~ Mirakel is an Android App for managing your ToDo-Lists
  ~
  ~   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
  ~
  ~       This program is free software: you can redistribute it and/or modify
  ~       it under the terms of the GNU General Public License as published by
  ~       the Free Software Foundation, either version 3 of the License, or
  ~       any later version.
  ~
  ~       This program is distributed in the hope that it will be useful,
  ~       but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~       GNU General Public License for more details.
  ~
  ~       You should have received a copy of the GNU General Public License
  ~       along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->"""
print et.tostring(doc,encoding='utf-8',pretty_print=True)
