Getting Started
===============

## Backend

### Setup

#### java 8
Java 8 (SDK & JRE) is required. Almost all os have a standard way of installing and upgrading java. These guides may work for you, but ideally you should look it up.

Test if you already have java 8 by running:
 - JRE: `java -version`
 - SDK: `javac -version`

**ubuntu/debian**
 - JRE: `sudo apt-get install default-jre`
 - SDK: `sudo apt-get install default-jdk`

**windows**
Download the JRE and SDK from [oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

**os x/mac os**
 - install [homebrew](https://brew.sh/)
 - `brew install java`

**other linux**
 - try the default package manager
 - otherwise [check this out](http://openjdk.java.net/install/)

#### neo4j
Install the **community edition** and **version 2.3.X**, where X is the highest you can find. The installation process depends on your os/environment. Here is the [official documentation](https://neo4j.com/docs/operations-manual/current/installation/). Below are summaries:

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

After installation you should start a neo4j server and navigate to `http://localhost:7474` and login using neo4j/neo4j. Choose a new password, **and remember it**. You will need it later on. 

#### maven
Maven is a java package manager, amongst other things. We use version 3.

**mac**
 - `brew install maven30`

**general**

Try your luck with the package manager, otherwise these links are handy:
 - [download](https://maven.apache.org/download.cgi)
 - [install](https://maven.apache.org/install.html)

### First steps

After all required software has been installed you are ready to proceed.

 - Strap up! `cd ~`
 - Organise `mkdir connect && cd ~/connect`
 - Clone `git clone git@github.com:emenlu/connect.git backend`
 - Charge in `cd backend`
 - Open `/src/main/resources/conf/application.properties` in your editor of choice
 - Change `neo4j.password` to what you entered previously in the web ui
 - Then run `mvn compile`
 - If this errors, contact Axel
 - Otherwie, run `mvn exec:java`

Et voilà, you are ready!

In the future, run `mvn compile exec:java -Dpippo.mode=dev` (maybe alias it). It does both commands after each other, and also runs the server in dev mode.

### Design

Here is an overview of the different components that make up the backend. Each box corresponds to a java package with a similar name.

![img](../images/overview.svg)

The design is very simple:
 - The application (`Connect.java`) is the entry point and registers routers from the `connect.routes` package.
 - Each router defines endpoints (e.g. `GET /login`) that it can serve and pippo (our main dependency) performs the actual route lookups.
 - All endpoints located inside classes in the `connect.routes` package may use the common classes (`Graph`, `*Exception`, `TrustLevel`) from the `connect` package.
 - Many endpoints also rely on the `connect.modules` package to do account stuff (`AccountSystem.java`), perform database queries (`Database.java`) or send mail (`Mailman.java`).
 - It is a goal (that we haven't reached) to expose the API endpoints as wrappers around a program, such that program logic isn't in the route handlers.
 - An example of a work-in-progress idea is the account router (`Account.java`), which heavily uses the account system (`AccountSystem.java`).
 - The two exceptions (`DatabaseException.java` and `RequestException.java`) are thrown by handlers and handled by a function in `Connect.java`, like a bubble-style event.
 - Database access is only done through the `Database.java` class, it is also responsible for parsing any database errors and throwing a `DatabaseException` if needed.

### Tips
 - We use a graph-database called Neo4j. The quickest way to get up to speed of what it is and how it works is to visit their website. 
 - Neo4j runs a graphical interface located at
`http://localhost:7474/browser/`. This very helpful to try and prototype commands
and later to see if correct connections and data was added to the database.
 - jcypher is used for querying the database from the backend much like SQL but with different syntax. The documentation is somewhat limited but there are examples on their github wiki. You should probably look at how things are done in the connect.routes package before trying your luck at the wiki, though. Note that jcypher can't
always translate a Neo4j query directly. 
 - The `application.properties` file has to be updated with the correct username and password for neo4j database.

### Eclipse
To use Eclipse, simply import the backend files as a github repository. We recommend using a maven plugin to facilitate running the server. 

## Frontend

### Setup

#### nodejs

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

#### packages

Type `npm install` in the repository to install all dependencies. Then try to run the dev. server using `make dev`. If this fails, report to Axel. Otherwise you are good to go!

### Design

Here is an overview of the different components that make up the frontend. 
![img](../images/frontend.svg) 
 
The structure is quite straightforward:
 - We use jade, less and js to make up the webpages. The less and js files both have a base file which they are dependent on, then each page sub-levels down to have it’s unique properties which the rest of each pages sub-levels depend on.
 - Jquery v3 is used and is imported via the base class so this is standard across all pages.
 - For styling, LESS is used. Structurally we have a working files folder which divides the pages up and then it is imported into an `all.less` file which is converted into one CSS file.
 - There are two api files: `api.js` and `api-dev.js`. The `api-dev.js` file only changes the server to which the ajax queries from `api.js` are made.

### Tips for Frontend
 - It can be important to run `make clean` every now and then to be sure nothing is cached and the changes implemented are what you have made. **`make clean` is run automatically when running any make command.**
 - There are a few utilities which are used across some of the pages. They can be found in `src/js/util`. 
  - One example is el.js which is used to efficiently create elements. It is encouraged to use these utilities where possible to keep the coding consistent.
 - There is no need to (re)build after _modifying_ any files. Simply save the file and reload localhost website to see the changes. 
 - If you add new views, then add them to `app.js`.
 - If you add new LESS files, add them to `src/less/all.less`.


