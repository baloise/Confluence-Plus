// modified version of https://github.com/codazoda/mouse-highlighter/blob/master/highlight.js
var mousePointerid = 'mousePointer';

function mousePointer() {
  var existingPointer = document.getElementById(mousePointerid);
  if(existingPointer) {
      var style = existingPointer.style.display == "none" ? "" : "none";
    	existingPointer.style.display = style;
      localStorage.removeItem(mousePointerid)
  } else {
     localStorage.setItem(mousePointerid, true)
  /* Draw a div */
  var mhNewPointer = document.createElement('div');
  mhNewPointer.id = mousePointerid;
  mhStyleMouseHighlight();
  document.getElementsByTagName('body')[0].appendChild(mhNewPointer);

  /* Move the div to the mouse */
  function mhHandleMouseMove(event) {
      newX = event.pageX - 25;
      newY = event.pageY - 25;
      mhNewPointer.style.left = newX + 'px';
      mhNewPointer.style.top = newY + 'px';
  }

  /* Style the mouse div */
  function mhStyleMouseHighlight() {
      mhNewPointer.style.position = 'absolute';
      mhNewPointer.style.backgroundColor = '#FFC300';
      mhNewPointer.style.width = '50px';
      mhNewPointer.style.height = '50px';
      mhNewPointer.style.opacity = 0.25;
      mhNewPointer.style.borderRadius = '25px';
      mhNewPointer.style.zIndex = '2147483647';
      mhNewPointer.style.pointerEvents = 'none';
  }
  document.onmousemove = mhHandleMouseMove;  
  }
}

if(localStorage.getItem(mousePointerid))  mousePointer()
