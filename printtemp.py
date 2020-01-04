#!/usr/bin/env python
# coding: utf-8

# In[1]:


import os
import time
import homematicip
from homematicip.home import Home
from homematicip.device import *
from homematicip.group import *
from homematicip.rule import *


# In[2]:


config = homematicip.find_and_load_config_file()


# In[3]:


home = Home()
home.set_auth_token(config.auth_token)
home.init(config.access_point)


# In[ ]:


home.get_current_state()

for group in filter(lambda g: g.groupType == 'META',home.groups):
    temps = list(map(str, map(lambda d: d.valveActualTemperature, 
                filter(lambda d: d.deviceType=='HEATING_THERMOSTAT',group.devices))))
    if len(temps) > 0:
        print(group.label + ": " + temps[0])


# In[ ]:




