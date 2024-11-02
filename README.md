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
* [ ] select pieces
  * [ ] shilft draws selection rectangle
  * [ ] crtl selects single piece
  * [ ] collect selected piece to one big pile
  * [ ] disperse selected pieces so there is no overlap
* [ ] finish scene
* [x] small window with preview of the final image
  * [ ] resizeable
  * [x] movable
  * [x] minimizable
* [ ] small window with preview of the pieces workspace
  * [ ] resizeable, movable, minimizable
  * [ ] current view is shown by a rectangle
  * [ ] workspace is movable by clicking in the small window
* [ ] configuration scene
  * [ ] image panel (image preview, piece preview)
  * [ ] text field (file name)
  * [ ] dropdown field (cutter)
  * [ ] slider (piece count)
  * [ ] select image dialog
* [ ] load/save
* [ ] Bugfix: set shader values: which edge should be drawn highlighted or darkened
  * [ ] option to "don't draw piece highlight on combined edges"
* [ ] show (outline) hidden pieces
* [ ] addiitional cutters
  * euler tiles
  * triangles which can be rotated 120 degrees?
    * https://www.anitasfeast.com/blog/2013/09/the-hagues-escher-in-het-palais/
    * https://www.researchgate.net/figure/Artistic-tiling-Escher-55_fig5_328137631
    * http://artwithmrsseitz.blogspot.com/2015/04/mc-escher-tessellations.html



TODO:
* move panel with click and drag
* select multiple pieces and move them
* move piece(s) to another panel.
* better file chooser
* first dialog should show preview of selected image
* crop selected image
* hide cutting in finished pieces

Reorder pieces:
* stack selected pieces
* unstack selected pieces
* order by hue
* order by number of knobs
* 