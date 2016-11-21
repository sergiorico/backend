Contributing
============

This file is mirrored [here](https://raw.githubusercontent.com/emenlu/connect/master/CONTRIBUTING.md).

Documentation
-------------
All documentation is avaiable at [readthedocs](https://serp-connect.rtfd.io) for developer guides.

Depedencies
-----------
Connect itself is mainly built on [pippo](https://pippo.ro) and [jcypher](https://github.com/Wolfgang-Schuetzelhofer/jcypher/wiki) but requires some tools to actually compile/test:

 - maven 3.X.Y ([website](https://maven.apache.org/index.html))
 - java 1.8
 - neo4j 2.X.Y ([website](https://neo4j.com/download/other-releases/))

Work work
---------
It is recommended to pick existing issues but we accept contributions not related to any issue. After picking an issue, please comment and state something like "hi, i'm calling dibs on this one". This will allow people with access to update status flags and what not to reduce risk of collisions.


Landing the PR
--------------
After you've done some of that sweet sweet coding we suggest that you try and `mvn verify` it. This is mainly a step to check that no regressions or other strange errors were made. If you can't get this to run then don't worry too much because the CI will run it for you anyway.


Now it is time to submit the pull request. Please refer to the relevant issue and provide a description of your solution.

Process
-------
 * Find issue (`easy-pick` = recommended for new contributors)
 * Fork project and create issue branch
  * ex. branch name `i33-fix-heartbleed`
 * Work work. Test!
 * Make pull request.
 * Watch CircleCI and CodeCov reports (fix problems if any)

Thanks for the interest, we hope to see you soon!
