VUE README 2003-03-17 Scott Fraize

VUE is currently built in Java 1.4, tho should run on Java 1.3.
It will currently work on either a Windows or a Macintosh OS.

To run the application:

        java tufts.vue.VUE

To test the map viewer:

        java tufts.vue.MapViewer

A few GUI tips:

  - Double-click opens the resource pointed to by a node
    in the default browser application for that type of
    resource on in the host OS

  - Ctrl-Drag from a node creates a new link
  - Ctrl-Drag with the spacebar down moves the whole map around
  
  - Ctrl 0 does zoom to fit, mostly
  - Ctrl 1 zooms to 100%
  - Ctrl + zooms bigger
  - Ctrl - zooms smaller
  (Zooming is incomplete: dragging the whole map
  while zoomed doesn't work yet)

  As an integration experiment, you can also drag a piece
  of artwork from Adobe Illustrator, and it will create
  an image node for it.

Cheers,
Scott
