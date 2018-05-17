Getting Started
===============

This page is dedicated to getting you ready to start developing. If you have questions, head over to our [slack](https://serp-group.slack.com) channel and fire away!

There are two different systems that must be installed in order for you to have a _complete_ development setup: _backend_ and _frontend_. It is possible, however, to skip the local _backend_ installation and instead use a shared testing backend hosted on lth. If you are only interested in frontend work then this is a good idea and you can skip the _backend_ part entirely.

## Backend

The backend is a web server programmed in Java, uses [maven](https://maven.apache.org/) as project management (dependency & build) and [neo4j](https://neo4j.com) as database. To simplify the development we build upon [pippo](http://www.pippo.ro), a small web server framework.

In order to get the backend up and running you need to complete these steps:
 - install java 8 (sdk & jre)
 - install maven version 3
 - install neo4j version 2.X.Y
 - logon `localhost:7474` and change password
 - update password in `application.properties` in repo.
 - start with `mvn exec:java -Dpippo.mode=dev`

### install java 8
Java 8 (SDK & JRE) is required. Almost all os have a standard way of installing and upgrading java. These guides may work for you, but ideally you should look it up yourself.

Test if you already have java 8 by running (any os):
 - JRE: `java -version`
 - SDK: `javac -version`

**ubuntu/debian**
 - JRE: `sudo apt-get install default-jre`
 - SDK: `sudo apt-get install default-jdk`

**windows**: Download the JRE and SDK from [oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

**os x/mac os**: install [homebrew](https://brew.sh/), then `brew install java`

**other linux**
 - try the default package manager (apt-get, yum, ...)
 - otherwise [check this out](http://openjdk.java.net/install/)

### maven
Maven is a java package manager, amongst other things. We use version 3.

**mac**: install [homebrew](https://brew.sh/), then `brew install maven30`

**general**

Try your luck with the package manager, otherwise these links are handy:
 - [download](https://maven.apache.org/download.cgi)
 - [install](https://maven.apache.org/install.html)

### install neo4j
Install the **community edition**, **version 2.3.X** where X is the highest you can find. Again, the installation process depends on your os/environment. Here is the [official documentation](https://neo4j.com/docs/operations-manual/current/installation/). Below are summaries:

**ubuntu/debian**
 - install neo4j via apt-get ([instructions](http://debian.neo4j.org/))
 - run `system neo4j start` to start
 - run `system neo4j stop` to stop

**windows**
 - download the installer (.exe) [from legacy](https://neo4j.com/download/other-releases/)
 - run it and install neo4j
 - start & stop neo4j using the `neo4j-ce.exe` program

**mac**
 - download the neo4j dmg [from legacy](https://neo4j.com/download/other-releases/)
 - drag neo4j to your applications folder
 - use that program to start & stop a neo4j server

**other linux**
 - download a binary [from legacy](https://neo4j.com/download/other-releases/)
 - untar it `tar -xzf <neo4j-download.tar.gz>`
 - run `pwd` to get current working directory
 - run `echo 'export $PATH=/full/path/to/neo4j-download/bin/:$PATH' >> ~/.profile`
 - run `source ~/.profile`
 - now your current and all new shells will be able to run the neo4j script
 - run `neo4j start` and `neo4j stop` to start & stop, respectively

After installation you should start a neo4j server and navigate to `http://localhost:7474` and login using neo4j/neo4j. Choose a new password, **and remember it**. You _will_ need it later on.

### First steps

After all required software has been installed you are ready to proceed. Here is a breakdown to get your installation configured:

```bash
cd ~
mkdir connect
cd ~/connect
git clone git@github.com:serpconnect/backend.git
cd backend
# vim/subl/code/nano src/main/resources/conf/application.properties
# set neo4j.password to the new password you entered previously 
mvn compile exec:java -Dpippo.mode=dev
```

Et voilà, you are ready!

(If you want to use Eclipse or IntelliJ, simply import the backend files as a github repository. We recommend using a maven plugin to facilitate running the server.)

## Frontend

The frontend project is often much simpler to install since it only depends on nodejs, thus this section is mainly on how to install nodejs. The high-level checklist is:

 - install nodejs (v5 or v6)
 - install packages (`npm install`)
 - run `npm run dev` or `npm run live`
 - browse to the address that was written in your terminal (e.g. `localhost:8181`)

### installing nodejs

The frontend relies on nodejs to compile page templates and style files. Node.js has its own package manager, called `npm`, which lists dependencies in a `package.json` file. Thus the only programs you need to manually install are `npm` and `node`. Thankfully, `npm` is bundled with `node` so installing `node` is sufficient.

Type `node -v` in a terminal to check version:
 - v5 and v6 are confirmed to work with the connect frontend
 - v7 and v8 are unknown

If you already have node v7 or v8 then you should install a version manager to switch between multiple version. Here are a few, though some only support specific operating systems:
 - [nvm](https://github.com/creationix/nvm)
 - [n](https://github.com/tj/n)
 - [nvs](https://github.com/jasongin/nvs)

The actual steps for installing Node.js vary depending on your operating system.

**windows**: Download the installer and run it. It will install `node` and `npm` and put them in your `%PATH%`.

**mac os**: Install with [homebrew](https://brew.sh). `brew install node@6`

**linux**

Most popular linux distros have up-to-date packages of `node` and this is the easiest way to install nodejs. There is a guide [here](https://nodejs.org/en/download/package-manager/) on doing this.

If this fails you must download a tarball and put `node` and `npm` into `/usr/bin` or similar. Some linuxes have a program called `alternative` to symlink files into `/usr/bin`.

### installing packages

Type `npm install` in the repository to install all dependencies. Then try to run the dev. server using `npm run dev`. If this fails, report this in the slack channel. Otherwise you are good to go!

