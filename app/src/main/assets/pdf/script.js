// It expects PdfAndroidJavascriptBridge to be injected from the Android side
(function () {
  var pdfViewer;

  function initializePdfViewer() {
    PDFJS.disableAutoFetch = true;
    PDFJS.useOnlyCssZoom = true;
    PDFJS.maxCanvasPixels = 2097152;
    var container = document.getElementById('viewerContainer');

    // enable hyperlinks within PDF files.
    var pdfLinkService = new PDFJS.PDFLinkService();

    pdfViewer = new CustomPdfViewer({
      container: container,
      linkService: pdfLinkService,
    });
    pdfLinkService.setViewer(pdfViewer);

    // set proper scale to fit page width
    container.addEventListener("pagesinit", function (e) {
      pdfViewer.currentScaleValue = 2;
    });

    var fileSize = PdfAndroidJavascriptBridge.getSize();

    PDFJS.getDocument({
      length: fileSize,
      range: new RangeTransport(fileSize),
      rangeChunkSize: 262144,
    }).then(function (pdfDocument) {
      pdfViewer.setDocument(pdfDocument);
      pdfLinkService.setDocument(pdfDocument, null);
      PdfAndroidJavascriptBridge.onLoad();
    }).catch(function (e) {
      console.error(e);
      PdfAndroidJavascriptBridge.onFailure();
    });
  }

  // Defines the interface, which PDF.JS uses to fetch chunks of data it needs
  // for rendering a PDF doc.
  function RangeTransport(size) {
    this.__proto__ = new PDFJS.PDFDataRangeTransport();

    var self = this;
    this.length = size;

    this.requestDataRange = function (begin, end) {
      var base64string = PdfAndroidJavascriptBridge.getChunk(begin, end);
      var binaryString = atob(base64string);
      var byteArray = stringToBytes(binaryString)
      // Has to be async, otherwise PDF.js will fire an exception
      setTimeout(function () {
        self.onDataRange(begin, byteArray);
      }, 0);
    };
  };

  function stringToBytes(str) {
    var length = str.length;
    var bytes = new Uint8Array(length);
    for (var i = 0; i < length; ++i) {
      bytes[i] = str.charCodeAt(i) & 0xFF;
    }
    return bytes;
  }

  // Built-in PdfViewer uses `container`'s height to figure out what pages to render
  // We can't limit container's height because of how `WebFragment` works in non-fullscreen mode,
  // so we have to subclass existing PDFViewer and provide different logic for figuring out
  // what pages are visible - using `window.innerHeight` instead of `element.clientHeight`.
  // So, it's mostly the same code copy-pasted from pdf_viewer.js, with little changes when we calculate
  // the bounds of the viewport
  function CustomPdfViewer(opts) {
    this.__proto__ = new PDFJS.PDFViewer(opts); // inheriting from this "class"
    this.scroll = watchScroll(document, this._scrollUpdate.bind(this));
    this.renderingQueue.setViewer(this);

    this._getVisiblePages = function () {
      return getVisibleElements(this.container, this._pages, true);
    }
  }

  // Mostly copy-pasted from https://github.com/mozilla/pdf.js/blob/f3987bba237c814b9ed314b904263bc36f83eb5b/web/ui_utils.js#L319
  // with some changes in top/bottom variabbles
  function getVisibleElements(scrollEl, views) {
    var sortByVisibility = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : false;

    // Changes start here
    var top = window.scrollY,
        bottom = top + window.innerHeight;
    // Changes end here
    var left = scrollEl.scrollLeft,
        right = left + scrollEl.clientWidth;
    function isElementBottomBelowViewTop(view) {
      var element = view.div;
      var elementBottom = element.offsetTop + element.clientTop + element.clientHeight;
      return elementBottom > top;
    }
    var visible = [],
        view = void 0,
        element = void 0;
    var currentHeight = void 0,
        viewHeight = void 0,
        hiddenHeight = void 0,
        percentHeight = void 0;
    var currentWidth = void 0,
        viewWidth = void 0;
    var firstVisibleElementInd = views.length === 0 ? 0 : binarySearchFirstItem(views, isElementBottomBelowViewTop);
    for (var i = firstVisibleElementInd, ii = views.length; i < ii; i++) {
      view = views[i];
      element = view.div;
      currentHeight = element.offsetTop + element.clientTop;
      viewHeight = element.clientHeight;
      if (currentHeight > bottom) {
        break;
      }
      currentWidth = element.offsetLeft + element.clientLeft;
      viewWidth = element.clientWidth;
      if (currentWidth + viewWidth < left || currentWidth > right) {
        continue;
      }
      hiddenHeight = Math.max(0, top - currentHeight) + Math.max(0, currentHeight + viewHeight - bottom);
      percentHeight = (viewHeight - hiddenHeight) * 100 / viewHeight | 0;
      visible.push({
        id: view.id,
        x: currentWidth,
        y: currentHeight,
        view: view,
        percent: percentHeight
      });
    }
    var first = visible[0];
    var last = visible[visible.length - 1];
    if (sortByVisibility) {
      visible.sort(function (a, b) {
        var pc = a.percent - b.percent;
        if (Math.abs(pc) > 0.001) {
          return -pc;
        }
        return a.id - b.id;
      });
    }
    return {
      first: first,
      last: last,
      views: visible
    };
  }

  // Copy-pasted from https://github.com/mozilla/pdf.js/blob/f3987bba237c814b9ed314b904263bc36f83eb5b/web/ui_utils.js#L242
  function binarySearchFirstItem(items, condition) {
    var minIndex = 0;
    var maxIndex = items.length - 1;
    if (items.length === 0 || !condition(items[maxIndex])) {
      return items.length;
    }
    if (condition(items[minIndex])) {
      return minIndex;
    }
    while (minIndex < maxIndex) {
      var currentIndex = minIndex + maxIndex >> 1;
      var currentItem = items[currentIndex];
      if (condition(currentItem)) {
        maxIndex = currentIndex;
      } else {
        minIndex = currentIndex + 1;
      }
    }
    return minIndex;
  }

  // Copy-pasted from https://github.com/mozilla/pdf.js/blob/f3987bba237c814b9ed314b904263bc36f83eb5b/web/ui_utils.js#L188
  function watchScroll(viewAreaElement, callback) {
    var debounceScroll = function debounceScroll(evt) {
      if (rAF) {
        return;
      }
      rAF = window.requestAnimationFrame(function viewAreaElementScrolled() {
        rAF = null;
        var currentY = viewAreaElement.scrollTop;
        var lastY = state.lastY;
        if (currentY !== lastY) {
          state.down = currentY > lastY;
        }
        state.lastY = currentY;
        callback(state);
      });
    };
    var state = {
      down: true,
      lastY: viewAreaElement.scrollTop,
      _eventHandler: debounceScroll
    };
    var rAF = null;
    viewAreaElement.addEventListener('scroll', debounceScroll, true);
    return state;
  }

  initializePdfViewer();
}());
