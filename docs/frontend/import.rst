.. _import:

Import
======

Entries can be imported from a CSV or json file. On the submit page click on the button "Import" and select a file. A new collection will be made when importing the file and a name for that collection has to be specified.

CSV
---

In addition to choosing a collection name, whether Research or Challenge entries are to be imported has to be specified. Another thing that can be specified is what delimiter should be used for the CSV file. Default is comma, other choices are semi-colon, colon, tab, and many more. 

The first row in the CSV, meaning the headers, will be mapped to the following attributes:

 - Reference
 - DOI
 - Description
 - Contact
 - Date
 
 - Intervention
 - Solving
 - Adapting
 - Assessing
 - Improving
 - Planning
 - Design
 - Execution
 - Analysis
 - People
 - Information
 - Sut
 - Other

Firstly a button is pressed next to the attribute to specify that the attribute should be used. Then a dropdown menu and a "+" icon appears. The dropdown is used to specify which header to map to the current attribute. If the "+" is pressed, the "+" will be turned into a "-" and another dropdown menu and "+" icon will appear. This means that an attribute can be mapped to multiple columns in the CSV. For the attributes that are not part of the taxonomy, the string values in the different columns will be concatenated. If the attribute is taxonomy leaf, the string values in the different columns will be added to an array. The dropdown menus can be discarded by pressing the "-" next to a dropdown menu. 

If an attribute is selected but not mapped to any header in the CSV it will get the placeholder value "unspecified". If the required attributes (e.g. Reference for research entries and Description for challenge entries) are not entered they will still get the value "unspecified" so that the entries are able to be submitted from the queue (since those attributes can't be empty). They can be edited later. 
 
A json object will be made for every row in the CSV according to the selected mapping. Every column in each row (or, a combination of columns depending on how they were mapped) will correspond to a part of the json object. For example, if a CSV header called "Abstract" was mapped to "description", the "description" field of the json objects will get the value of the cell in the columns under the CSV header "Abstract".

The json objects will then be converted to entries and be put in the queue. 

JSON
----

Importing the json file is a lot simpler than importing the CSV file, only the collection name has to be specified. 

The file content has to be one list of json objects and in the same way as for the CSV, the json objects will then be converted to entries and queued. If there are invalid entries (e.g. no Reference for research entries or no Description for challenge entries), a message will pop up saying which entries were invalid (or how many, depending on how many there are) and the user will be asked if he/she wants to continue adding the valid entries or exiting. 
 
