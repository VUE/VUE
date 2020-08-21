# Visual Understanding Environment

Visual Understanding Environment (VUE) is a powerful mind-mapping, teaching, and presentation tool.

Note: **See "Changes" section for details about the rpavlik fork**

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
| `launch4j`| creates a Windows executable launcher |
| `clean`   | deletes all classes compiled earlier |
| `build`   | compiles the code and makes a jar as well as distributions |
| `tasks`   | list all available tasks |

## Changes

This package hasn't seen a lot of love from upstream recently, though it is fairly complete and usable as-is.
This is a modified version, assembled by Ryan Pavlik from a variety of forks on GitHub.
Ryan also adjusted it to build using Gradle instead of Ant.

### Apollia changes

A number of changes were sourced from github user biwin who had merged from user `apollia`.
While some commits were purely cosmetic (theming), the non-theming commits were broadly useful.
Apollia changelog entries for those commits picked onto this branch are below:

**Jan. 7, 2017**:  Here's what I managed to do so far:

* Made it so when running VUE on a Unix-like platform, the default Java look-and-feel is now GTK, not Metal.  (This is just to make VUE blend in better with my preferred dark GTK theme.)
* Switching to the GTK look-and-feel somehow made it impossible to get out of (or into) split-screen mode.  So, I got rid of the split-screen that appears at startup.  (Told the mViewerSplit object to remove its righthand panel.)

**Jan. 23, 2017**:

* A new submenu of the Edit menu called "Etc."  I moved the dangerous "Cut" and "Delete" menu items into it.
* A new command, "Insert Current Date/Time" (keyboard shortcut: F5).  It's in the new "Insert Bubble or Text" submenu of the Edit menu.
  It inserts dates in this format: 2017-01-23 14:55:49
  It works on bubbles or links, but for some reason, doesn't work on text blocks.  (Which I didn't fix, because I never use text blocks anyway.)
  If no bubble or link is selected, or if more than one is selected, a new bubble is created, containing the current date/time.
  If one bubble or link is selected, the current date/time is added to the label of the selected bubble or link, either at the end of the label, or wherever the caret currently is.
* Changed the original dangerous behavior of ESC while editing a bubble label or link label.
  Originally, ESC would delete all the changes to a bubble label or link label, and even Undo or Redo couldn't retrieve whatever was deleted.
  Now, ESC leaves the label text alone, and just deselects the bubble or link you were editing.
  (Which is how text blocks already reacted to ESC, so I didn't change anything related to text blocks.)
* Now, bubbles dragged into other bubbles won't be shrunk down and won't have their background colors darkened.
  And images dragged into bubbles also won't be shrunk down.

**Feb. 10, 2017**:

* Formerly, clicking a bubble to edit it would unnecessarily highlight all text.  Now, VUE automatically unhighlights that text, so fewer clicks are necessary to edit a bubble.
* A new Edit menu item - Deselect. (Keyboard shortcut: ESC)  Only works if only one item is selected.
* All text in both bubbles and links is now left-justified.
* Formerly, the text caret couldn't be seen on dark backgrounds.  To fix that, the text caret is given the same color as the current bubble or link's text color.

**Feb. 12, 2017**:

* Now you can quickly scroll through your map tabs using your mouse wheel! (**Note by rpavlik**: This code was disabled due to build errors, but marked with a TODO.)
  Uses almost unchanged code from here:
  http://stackoverflow.com/a/38463104
  http://stackoverflow.com/questions/38463047/use-mouse-to-scroll-through-tabs-in-jtabbedpane
* It's now possible to drag and drop tabs to reorder them!
  Uses code from here:
  http://stackoverflow.com/a/61982
  Which is unchanged except for replacing the convertTab method with an unchanged copy of the code from this post:
  http://stackoverflow.com/a/8610017
  Both of those links go to:
  http://stackoverflow.com/questions/60269/how-to-implement-draggable-tab-using-java-swing
* Possibly fixed the longstanding problem of mistaken-seeming "Failed to load map" dialog boxes sometimes popping up when loading a map (which always seemed mistaken because it always looked like the map loaded just fine).
  All I had to do was comment out two lines in MapTabbedPane.java.

**Feb. 16, 2017**:

* I commented out the code that caused VUE at startup to connect to vue.tufts.edu to look for an updated version of itself.
* I commented out and/or modified the worrying-looking code that seemed to be causing VUE to send every web URL the user added to a VUE map to the website open.thumbshots.org.

**Feb. 18, 2017**:

* Now Ctrl-T makes a new tab with a new map, instead of inserting a new textblock.  (Ctrl-Shift-N no longer creates a new map.)

**Feb. 21, 2017**:

* Now it's possible to rename layers while using VUE in Puppy Linux.
* Now, URLs you dragdrop into a VUE map are left alone and inserted verbatim.
  Newlines aren't inserted into them anymore, and they no longer get abbreviated at all.

**Feb. 22, 2017**:

* Now Ctrl-3 brings up the Panner (which I use a lot) instead of the Content panel, which I never use.

## Contact Information

For further information on VUE, visit [VUE's homepage](http://vue.tufts.edu/).
