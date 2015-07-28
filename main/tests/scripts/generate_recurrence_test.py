#!/usr/bin/python2

import sys
import os
from helpers import *
import random
from copy import deepcopy
from airspeed import CachingFileLoader

if len(sys.argv)  != 2:
    print(sys.argv[0] + "  <pathToTestDir>")
    sys.exit()

path="de/azapps/mirakel/sync/taskwarrior/model/test/"
filename="RecurrenceTest.java"

tests_single={
       "annual":{"getYears":1}
      ,"biannual":{"getYears":2}
      ,"bimonthly":{"getMonths":2}
      ,"biweekly":{"getDays":14}
      ,"biyearly":{"getYears":2}
      ,"daily":{"getDays":1}
      ,"fortnight":{"getDays":14}
      ,"monthly":{"getMonths":1}
      ,"quarterly":{"getMonths":3}
      ,"semiannual":{"getMonths":6}
      ,"sennight":{"getDays":7}
      #,"weekdays":{} # is in template
      ,"weekly":{"getDays":7}
      ,"yearly":{"getYears":1}
}

tests_multi={
      "days":{"getDays":1}
      ,"day":{"getDays":1}
      ,"d":{"getDays":1}
      ,"hours":{"getHours":1}
      ,"hour":{"getHours":1}
      ,"hrs":{"getHours":1}
      ,"hr":{"getHours":1}
      ,"h":{"getHours":1}
      ,"minutes":{"getMinutes":1}
      ,"mins":{"getMinutes":1}
      ,"min":{"getMinutes":1}
      ,"months":{"getMonths":1}
      ,"month":{"getMonths":1}
      ,"mnths":{"getMonths":1}
      ,"mths":{"getMonths":1}
      ,"mth":{"getMonths":1}
      ,"mos":{"getMonths":1}
      ,"mo":{"getMonths":1}
      ,"quarters":{"getMonths":3}
      ,"qrtrs":{"getMonths":3}
      ,"qtrs":{"getMonths":3}
      ,"qtr":{"getMonths":3}
      ,"q":{"getMonths":3}
      #,"seconds":{} Not supported by Mirakel
      #,"secs":{}
      #,"sec":{}
      #,"s":{}
      ,"weeks":{"getDays":7}
      ,"week":{"getDays":7}
      ,"wks":{"getDays":7}
      ,"wk":{"getDays":7}
      ,"w":{"getDays":7}
      ,"years":{"getYears":1}
      ,"year":{"getYears":1}
      ,"yrs":{"getYears":1}
      ,"yr":{"getYears":1}
      ,"y":{"getYears":1}
    }
VARS={}
VARS["FUNCTIONS"]=[]

for key,value in tests_multi.items():
    ranges={1,2,5,6,10}
    for i in ranges:
        cmd=deepcopy(value)
        for k in cmd.keys():
            cmd[k]*=i
        function={"name":str(i)+key,"value":cmd[cmd.keys()[0]],"function":cmd.keys()[0]}
        VARS["FUNCTIONS"].append(function)
        
for key,value in tests_multi.items():
    cmd=deepcopy(value)
    function={"name":key,"value":cmd[cmd.keys()[0]],"function":cmd.keys()[0]}
    VARS["FUNCTIONS"].append(function)
    
for key,value in tests_single.items():
    cmd=deepcopy(value)
    function={"name":key,"value":cmd[cmd.keys()[0]],"function":cmd.keys()[0]}
    VARS["FUNCTIONS"].append(function)


loader = CachingFileLoader(".")
template = loader.load_template(os.path.dirname(__file__) + "/templates/recurrenceTemplate.java")


directory=sys.argv[1]+'/src/'+path+'/';
# print("Finish generating Test for "+className)
if not os.path.exists(directory):
    os.makedirs(directory)
f = open(directory+filename, 'w')
f.write(template.merge(VARS,loader=loader))
f.close()

