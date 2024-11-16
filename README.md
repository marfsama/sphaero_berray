This is sphaero2, a derivative of the original sphaero Jigsaw Puzzle
application by Paul Brinkley:

http://sphaero.sourceforge.net/

I've kept the heart largely unchanged - the general cutting of pieces,
the main puzzle class, and the basic image handling and display. It's
been refactored a bit, modernized (so it needs Java 5 or later),
cleaned up, and the user interface has been tarted up significantly. To
be honest, the basic core was pretty darned good already, the interface
just deserved a little extra TLC.

The name I've used is sphaero2, to make it clear that it's a derivative
of the original sphaero, but isn't quite the same.

There is no explicit license on the files, but the sourceforge project
page says GPL, so I'm assuming that's what the license is. (Except for
ImagePreview.java, which is copyrighted by Oracle.)


Berray TODO:
* [x] panel should extend in negative directions so the mouse events work
* [x] zoom with scroll wheel
  * [x] show zoom as label
  * [x] set initial zoom so the entire image can be visible
  * [x] zoom to mouse cursor
* [x] fix bug: join pieces and press rotate. The last piece is rotated instead of the combined piece 
* [x] select pieces
  * [x] shilft draws selection rectangle
  * [x] crtl selects single piece
  * [x] collect selected piece to one big pile
  * [x] disperse selected pieces so there is no overlap
* [x] finish scene
* [x] small window with preview of the final image
  * [x] movable
  * [x] minimizable
* [x] small window with preview of the pieces workspace
  * [x] movable, minimizable
  * [x] current view is shown by a rectangle
  * [ ] Bugfix: current view rectangle should not be bigger than window 
  * [ ] workspace is movable by clicking in the small window
* [ ] change background
* [ ] change selection color
* [ ] configuration scene
  * [ ] image panel (image preview, piece preview)
  * [ ] text field (file name)
  * [ ] dropdown field (cutter)
  * [ ] slider (piece count)
  * [ ] select image dialog
* [ ] load/save
* [ ] Bugfix: set shader values: which edge should be drawn highlighted or darkened
  * [ ] or simply add an outline to selected pieces? 
  * [ ] option to "don't draw piece highlight on combined edges"
    * maybe don't use shader to draw highlights but create additional textures 
* [ ] show (outline of) hidden pieces
* [ ] additional cutters
  * euler tiles
    * [x] hexagons
    * [ ] rotate other than 90°
    * rows shifted each 2nd row
  * triangles which can be rotated 120 degrees?
    * https://www.anitasfeast.com/blog/2013/09/the-hagues-escher-in-het-palais/
    * https://www.researchgate.net/figure/Artistic-tiling-Escher-55_fig5_328137631
    * http://artwithmrsseitz.blogspot.com/2015/04/mc-escher-tessellations.html
    * http://en.tessellations-nicolas.com/method.php

* order by hue
* order by number of knobs