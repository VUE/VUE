VUE README 2003-03-19 Scott Fraize

VUE is currently built in Java 1.4, and should run on Java 1.3, tho
the Mac version is having rendering problems at the moment.
Generally, it currently should work on either a Windows or a Macintosh
OS, and has been tested on Windows 2000 and Mac OSX 10.2.4.

To run the application:

        java tufts.vue.VUE

To test just map viewer:

        java tufts.vue.MapViewer

A few GUI tips:

  - Double-click opens the resource pointed to by a node
    in the default browser application for that type of
    resource on in the host OS

  - Ctrl-Drag from a node creates a new link
  - Ctrl-Drag with the spacebar down moves the whole map around
  - Ctrl 0 does zoom to fit
  - Ctrl 1 zooms to 100%
  - Ctrl + zooms bigger
  - Ctrl - zooms smaller
  - 'z' zooms in at point
  - 'Z' zooms out at point

  - Right-click pops a test context menu, but they
    don't do anything yet.

  - As an integration experiment, you can also drag a piece
    of artwork from Adobe Illustrator, and it will create
    an image node for it.

Cheers,
Scott
