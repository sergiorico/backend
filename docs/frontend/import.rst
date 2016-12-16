.. _import:

Import
======

It is possible to import entries from a CSV or json file. On the submit page click on the button "Import" and select a file. A new collection will be made when importing the file and a name for that collection has to be specified.

CSV
-----

One of the two following entry types, and their corresponding fields that they must have, has to be selected. 

 - research (reference, doi)
 - challenge (description)
  
The user gets help with selecting these by having the required fields in the color red.

The first row in the CSV, meaning the headers, will by the user be mapped to the following attributes with the help of dropdown menus:

 - reference
 - doi
 - description
 - contact
 - date
 
 - intervention
 - solving
 - adapting
 - assessing
 - improving
 - planning
 - design
 - execution
 - analysis
 - people
 - information
 - sut
 - other
 
A json object will be made for every row in the excel according to the selected mapping. Every column in each row will correspond to a part of the json object. For example, if a CSV header called "Abstract" was mapped to "description", the "description" field of the json objects will get the value of the cell in the columns under the CSV header "Abstract".

The json objects will then be converted to entries. The entries that are valid will be put into the submit queue. If some entries in the excel does not have a value for a required field, they will not be queued. A message will pop up saying which entries were invalid (or how many, depending on how many there are). 

JSON
-----

Importing the json file is a lot simpler than importing the CSV file. The file content has to be one list of json objects and in the same way as for the CSV, the json objects will then be converted to entries and queued if they are valid. The user does not get any help with the json import, so json objects that are not valid will simply not be queued. Also as for the CSV, a message will pop up saying which entries were invalid (or how many, depending on how many there are). 
 
