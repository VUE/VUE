# Visual Understanding Environment

Visual Understanding Environment (VUE) is a powerful teaching and presentation tool.

## Code Organization

The code is organized in following folders
   
  | Folder name | Folder content |
  | ----------- | -------------- |
  | src         | the source files |
  | test        | automatic JUnit tests |
  | lib         | third party libraries used in VUE |
  | linux       | code specific to Linux operating systems |

## Compiling and Running
The easiest method to compile and run VUE code is using ant version 1.6 or higher. 
`build.xml` file in _src_ folder contains many tasks to make clean builds of complete
VUE application on Linux, Mac and Windows platforms. Here is a list of few useful tasks

  | Task name | Action |
  | --------- | ------ |
  | `default` | runs VUE (need to run `compile` task for this to work) |
  | `clean`   | deletes all classes compiled earlier |
  | `compile` | compiles the code |
  | `jar`     | creates VUE.jar which contains all the required classes and libraries to run VUE |

## Contact Information
For further information on VUE, visit [VUE's homepage](http://vue.tufts.edu/).
