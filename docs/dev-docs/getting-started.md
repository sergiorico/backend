Getting Started
===============

## Backend

### Preparing your workstation

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

### Workflow
 - Find issue on github. 
 - Pull latest updates from master.
 - Create a new branch with a sensible name and switch to it.
 - Implement changes in your favorite IDE, Eclipse is recommended for backend.
 - Write test case for backend changes if relevant.
 - Document changes.
 - Commit the changes with a sensible comment no longer than 50 characters.
 - Pull latest changes.
 - Push the changes and create a pull request.

## Tips for backend
 - A graph-database called Neo4j is used as our database. Quickest way to get up to speed of what it is and how it works is to visit their website. Neo4j have a graphical interface which is located at
`http://localhost:7474/browser/` when it's started. This can be helpful in the beginning to try commands
and later to see if correct connections and data was added to the database.
 - jcypher is used for querying the database from the backend much like SQL but with different syntax.
There are somewhat limited documentation of jcypher but there are some examples on their github wiki.
Easiest is probably to look at how things are done in the connect.routes package. Note that jcypher can't
always translate a Neo4j query directly. But everything that can be achieved by Neo4j querying can be achieved
in someway with jcypher. 
 - The application.properties file has to be configurated with the right settings in order to run the database and other applications.

### Eclipse
To use Eclipse, simply import the backend files as a github repository. We recommend using a maven plugin to facilitate running the server. 

## Frontend

Here is an overview of the different components that make up the frontend. 
![img](../images/frontend.svg) 
 
The structure is quite straightforward:
 - We use jade, less and js to make up the webpages. The less and js files both have a base file which they are dependent on, then each page sub-levels down to have it’s unique properties which the rest of each pages sub-levels depend on.
 - Jquery v1.8 is used and is imported via the base class so this is standard across all pages.
 - For CSS, Less is used. Structurally we have a working files folder which divides the pages up and then it is imported into an all.less file which is converted into one CSS file, this is so the website only has to load this page once.
 - There are many scripts set up to make development of the site easier, these scripts search for updates in the Less, Jade and js files and as the contributor make these changes they will update automatically with immediate effect provided there are no errors for the compiler.
 - There is also two api end points, one for the live working version of the website and the other for when in development mode.

### Workflow
 - Find issue on github.
 - Pull latest updates from master.
 - Create a new branch with a sensible name and switch to it.
 - Make sure neo4j and backend servers are running. 
   backend: cd connect && mvn compile exec:java
 - Run local web server @ port 8181 with XHR/AJAX against local 8080
 - Use make files to have development scripts runnning to update changes on the fly.
 - Implement changes in your favorite IDE, Atom is recommended for frontend.
 - Recommended browsers to test on are Chrome, Firefox & Safari.
 - Write test case for frontend changes if relevant.
 - Document changes.
 - Commit the changes with a sensible comment no longer than 50 characters.
 - Pull latest changes.
 - Push the changes and create a pull request.

## Tips for Frontend
 - Node.js is used so it is recommended that you use versions 5 or 6 which both work with the current version of the site.
 - It can be important to use the make clean file every now and then to be sure nothing is cached and the changes implemented are what you have made.
 - There are a few utilities which are used across some of the pages. They can be found in src/js/util. One example is el.js which is used to efficiently create elements. It Is encouraged to use these utilities where possible to keep the coding consistent.
 - There is no need to compile after changes have been made to see effect. Simply save the file and update the localhost website to see the changes. 


