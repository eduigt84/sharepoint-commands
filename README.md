# IGT Sharepoint Commands

## Description

Similar to Jira and Wiki commands, we created this sharepoint interface to be able to communicate with it and be able to take basic actions such as:
- Upload files,
- Create web pages,
- Get list of items,
- Etc

## Getting Started

### Dependencies

* Java IBM JDK1.8
* Sharpoint credentials (please ask our sharepoint admins for those)
* A site to be able to work with given the above credentials

### Installing

* For development purposes just download the code using git clone command
* For regular usage in your custom java project just use a maven reference like this (using xxmaven repository):
```
  <dependency>
      <groupId>com.igt.tools</groupId>
      <artifactId>sharepoint-commands</artifactId>
      <version>1.0</version>
  </dependency>
```
For examples on how to use it with parameters plese refer to the next section.

### Executing program

* Just run the main program with the required parameters, all commands are described in that test class
```
sharepoint/igt/com/Main.java
```

## Authors

- Eduardo Saavedra

## Version History

* 1.0
    * Initial Release, client/secret id authentications, basic commands: 
      * getAllSiteLists
      * getListItemsByTitle
      * getPagesListByRelativePath
      * folderExists
      * createFolderRecursively
      * createFolder
      * uploadFile
      * createNewPageFromTemplate
      * getPage
      * checkOutPage
      * savePage
      * publishPage
      * getUser
      * ensureUser
      * shareSite

* 1.2
    * Second Release, added one more method
        * getUsers    

* 1.3
    * Third Release, added one more method
        * downloadFile

* 1.4
    * Additional parameter for "createNewPageFromTemplate", so we can define a path for sharepoint base template aspx.