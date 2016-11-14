Documentation
=============

 - Documentation is written in reStructuredText (with sphinx httpdomain plugin)
 

 - Install python and pip.
 - ``pip install sphinx sphinxcontrib.htmldomain``
 - Watch files & rebuild ``ls -d * | entr sphinx-build -b html . <path-to-output>``