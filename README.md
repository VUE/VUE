# Visual Understanding Environment

Visual Understanding Environment (VUE) is a powerful mind-mapping, teaching, and presentation tool.

## Code Organization

The code is organized in following folders under `VUE2`,
though it is in the process of being re-organized to suit Gradle/Maven.

| Folder name | Folder content |
| ----------- | -------------- |
| src         | the source files |
| test        | automatic JUnit tests |
| lib         | third party libraries used in VUE |
| linux       | code specific to Linux operating systems |

## Compiling and Running

The easiest method to compile and run VUE code is using Gradle, and the gradle wrapper is included.
Just run `./gradlew` or `gradlew` depending on your platform (non-Windows vs Windows).
The following useful tasks might be interesting:

| Task name | Action |
| --------- | ------ |
| `run`     | runs VUE |
| `clean`   | deletes all classes compiled earlier |
| `build`   | compiles the code and makes a jar as well as distributions |
| `tasks`   | list all available tasks |


## Contact Information
For further information on VUE, visit [VUE's homepage](http://vue.tufts.edu/).
