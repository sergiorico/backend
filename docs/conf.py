# -*- coding: utf-8 -*-
#
import os
import sys
from recommonmark.parser import CommonMarkParser

sys.path.insert(0, os.path.abspath('..'))

sys.path.append(os.path.abspath('_ext'))
extensions = ['sphinxcontrib.httpdomain']

master_doc = 'index'

project = 'SERP Connect'
copyright = '2015-2016, ??'
author = '??'

source_parsers = {
    '.md': CommonMarkParser,
}

source_suffix = [ '.rst', '.md' ]
