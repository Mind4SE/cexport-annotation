cexport-annotation
==================

Mind annotation plugin to export chosen interfaces as friendly C symbols.

User info
---------

@CExport use examples can be found in src/assemble/resources/examples, ready to use into the MindEd Eclipse plugin (Import -> General -> Existing projects into workspace).

To generate HTML and PDF documentation from this repository, use Maven as follows:
mvn -N docbkx:generate-html docbkx:generate-pdf

Build info
----------

To generate the plugin, just run 'mvn install' from the root folder.
To use it, copy the target jar file to the mind-compiler 'ext' folder. 
