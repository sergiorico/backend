Documentation
=============

Most documentation is written in reStructuredText but it is ok to use markdown as well. Markdown rendering uses `recommonmark`_. 
 

 - Install pip: `instructions`_
 - ``pip install -r requirements.txt``
 - Use `entr`_ to watch files, rebuild and run a webserver:
  * ``cd docs/ && find ./ | entr -d -r 'sphinx-build . docsbin/ && cd docsbin/ && python -SimpleHTTPServer``
  * or ``cd docs/ && make watch`` 

   .. _instructions: https://packaging.python.org/installing/#install-pip-setuptools-and-wheel
   .. _recommonmark: https://github.com/rtfd/recommonmark
   .. _entr: http://entrproject.org/