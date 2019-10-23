function initInputBox($cont, getHintsFun, getEntriesFun, runQueryFun) {
  var $inputbox = $cont.children('.tokenbox-editbox').first()
  var $hintbox = $cont.children('.tokenbox-hints').first()
  var getHints = getHintsFun
  var getEntries = getEntriesFun
  var runQuery = runQueryFun

  const timeThrottlingMs = 0
  const TWORD = "word"
  const TKEYWORD = "keyword"

  var hintSelected = -1
  var hintInBox = null
  var hints = []

  // tips - todo - refactor, shall not depend on #intro
  $('#intro').find('a').click(function (e) {
    e.preventDefault()
    $inputbox.text(e.target.attributes['data-query'].value)
    focusBox()
  })

  // todo - this must live outside!
  function setupReports() {
    $('a.report').click(function(e) {
      var q = $(this).data('query')
      hintInBox = {data:-1} // hack
      $inputbox.text(q)
      e.preventDefault()
      tryRunQuery()
    })
  }
  setupReports()

  $inputbox.on('keydown', function(e) {
    switch(e.which) {
      case 38: // up
      console.info('up')
      if (hints.length && hintSelected > 0) {
        hintSelected = hintSelected - 1
        highlightHint()
        hintToBox(hints[hintSelected])
        focusBox()
      }
      e.preventDefault()
      break;

      case 40: // down
      console.info('down')
      if (hints.length && hintSelected < hints.length-1) {
        hintSelected = hintSelected + 1
        highlightHint()
        hintToBox(hints[hintSelected])
        focusBox()
      }
      e.preventDefault()
      break;

      case 13: // enter
      console.info('enter')
      e.stopPropagation()
      e.preventDefault()
      if (hintSelected >= 0)  {
        hintToBox(hints[hintSelected])
        hideHints()
        focusBox() // it must be already focused, but this way we move caret to end
      }
      tryRunQuery()
      break;

      case 27: // esc
      console.info('esc')
      hideHints()
      break;
    }
  })

  // need to sanitize input. Util done, better prevent
  $inputbox.on('paste', function(event) {
    event.stopPropagation()
    event.preventDefault()
  })

  $inputbox.on('input', function(a) {
    hintInBox = null // we break link to hint when editing
    tryShowHints(true)
  })

  $inputbox.on('focus', function(a) {
    tryShowHints()
  })

  var hintRequestTimeout = null
  function tryShowHints(isUserTextInput) {
    clearTimeout(hintRequestTimeout)
    // only direct user input (key presses) are throttled - other cases (like focus) are not
    if (isUserTextInput)
      hintRequestTimeout = setTimeout(function() { tryShowHintsImpl(isUserTextInput) }, timeThrottlingMs)
    else
      tryShowHintsImpl(isUserTextInput)
  }

  function tryShowHintsImpl(isUserTextInput) {
    var tokensAndEditedToken = getTokens($inputbox)
    var tokens = tokensAndEditedToken.tokens
    var editedToken = isUserTextInput ? tokensAndEditedToken.editedToken : -1 // if not user input, we don't consider token to be edited even if caret is on the token
    console.info("tokens (edited: " + editedToken + "):")
    console.info(tokens)
    var d = hintInBox ? hintInBox.data : -1; // server wants to know if we try to extend some known hint
    getHints({tokens:tokens, data:d, editedToken:editedToken}, function(h) { // todo -1
      console.info('hints:')
      console.info(h)
      showHints(h)
    })
  }

  $inputbox.on('mousedown', '.tokenedit-token-placeholder', function(event) {
    // to prevent from edit box focusing (and firing events) when clicking on placeholders
    event.preventDefault();
  });

  $inputbox.on('click', '.tokenedit-token-placeholder', function(e) {
    hideHints()
    token = $(this).data('token')
    setTimeout(function() { // to prevent click event interfering with further processing
      editToken(token)
    }, 0);
    e.stopPropagation() // to not fire focus event on text box
  })

  function highlightHint() {
    $hintbox.children('div').removeClass('selected')
    $hintbox.children('div').eq(hintSelected).addClass('selected')
  }

  function hintToBox(hint) {
    var txt = makeInputFromHint(hint)
    $inputbox.html(txt)
    hintInBox = hint
  }
  
  function showHints(hints_) {
    hints = hints_
    hintSelected = -1
    $hintbox.empty()
    var hintNo = 0
    hints.forEach(function(h) {
      $hintDiv = makeHintDiv(h)
      $hintDiv.mouseover(hintNo, function(event) { // passing hintNo to have modified value inside closure
        hintSelected = event.data // hintNo comes here
        highlightHint()
      })
      $hintDiv.click(function() {
          setTimeout(function() { // to prevent click event interfering with further processing
            hintToBox(h)
            hideHints()
            tryRunQuery()
          }, 0);
      })
      $hintbox.append($hintDiv)
      hintNo++
    })
    if (hints.length > 0) {
      hintSelected = 0
      highlightHint()
      $hintbox.css('display', 'block')
    }
  }

  function hideHints() { 
    hints = []
    hintSelected = -1
    $hintbox.empty()
  }

  function tryRunQuery() {
    var tokens = getTokens($inputbox).tokens
    var placeholders = []
    tokens.forEach(function(t) {
      if (t.type == 'PLACEHOLDER')
        placeholders.push(t)
    })
    if (placeholders.length > 0) {
      editToken(placeholders[0], function() {setTimeout(function() {tryRunQuery()}, 0)})
    }
    else if (hintInBox) {
      runQueryFun({tokens:tokens, data:hintInBox.data})
    }
    else {
      tryShowHints()
    }
  }

  function editToken(token, doneFun) {
    var $bx = $cont.children('.tokenbox-edittoken-cont').first()
    var $table = $bx.children('.tokenbox-edittoken-list').first()
    var id = uniq()

    // align arrow
    var $arrow = $bx.children('.tokenbox-edittoken-arrow').first()
    var $t = findTokenEl(token)
    $arrow.css({left:($t.position().left-40)+'px'}) //todo -40

    $table.text("Loading...")
    $bx.show();

    // hide dropdown on click outside
    $(document).on('click.'+id + ' keydown.'+id, function() {
      hideBox()
    })

    getEntries(token, function(ts) {
      $table.empty()
      ts.forEach(function(t) {
        var $row = $('<div class="tokenbox-edittoken-row">' + t.text + '</div>')
        $row.click(function() {
          replaceToken(token, t)
          hideBox()
          if (doneFun)
            doneFun()
          else
            tryShowHints()
        })
        $table.append($row)
      })
    })

    function hideBox() {
      $bx.hide()
      $(document).off('.'+id)
    }

    function findTokenEl(token) {
      var $res
      $inputbox.contents().each(function() {
        var $t = $(this)
        if ($t.data && $t.data('token') === token) {
          $res = $t
        }
      })
      return $res
    }

    function replaceToken(token, newToken) {
      var $t = findTokenEl(token)
      if ($t) $t.replaceWith(makeTokenElForInput(newToken))
    }
  }

  function makeHintDiv(hint) {
    var $content = $(document.createDocumentFragment())
    hint.tokens.forEach(function(token) {
      $content.append(makeTokenElForDiv(token))
      $content.append(" ")
    })
    return $('<div class="tokenedit-hint">').append($content)
  }

  function makeInputFromHint(hint) {
    var $content = $(document.createDocumentFragment())
    hint.tokens.forEach(function(token) {
      $content.append(makeTokenElForDiv(token))
      $content.append(" ")
    })
    return $content
  }

  function makeTokenElForDiv(token) {
      if (token.type == TKEYWORD)
        return token.text

      var $el
      $el = $('<div contenteditable=false class="tokenedit-token">&nbsp;' + token.text + '&nbsp;</div>')
      $el.data("token", token)
      return $el
  }

  function focusBox() {
    placeCaretAtEnd($inputbox.get(0))
  }

  $(document).click(function(event){
    if (!event) { var event = window.event; }
    var s = event.srcElement ? event.srcElement : event.target;
    if (!$(s).is($inputbox))
      hideHints()
  })

  /** @returns {{tokens:Token[], editedToken:number}} */
  function getTokens($el) {
      var tokens = []
      var editedToken = -1
      var tokenNum = -1
      var sel = window.getSelection()
      $el.contents().each(function() {
        var t = this
        if (t.nodeType == 3) { // text
          var pos = 0 // position within the split string (used to find word with caret)
          // todo: this string-splitting shall probably be done on the server?
          var split = t.nodeValue.match( /[^\s=<>\(\)]+|[=<>\(\)]/g )
          if (split) {
            split.forEach(function(w) {
              if (w != '') {
                tokenNum++
                tokens.push({'type':TWORD,'text':w})
                pos += w.length // move to next word
                // if caret is within this node, check if it was this node
                // this returns true is caret is in the word or right after the last char of the word
                if (t == sel.anchorNode && editedToken < 0 && sel.extentOffset <= pos) {
                  editedToken = tokenNum
                }
              }
              pos += 1 // end of any word means increasing next token start position
            })
          }
        }
        else if (t.nodeType == 1) { // element (must be div and must contain token in data attribute)
          tokenNum++
          tokens.push($(t).data('token'))
        }
      })
      return {tokens:tokens, editedToken:editedToken}
  }

  setTimeout(function() {
    $inputbox.focus() // doesnt work without timeout for contenteditable divs
  }, 0);

  return {
    setText(txt) {
      $inputbox.text(txt)
      $inputbox.get(0).blur() // if was focused, we want to trigger focus event to uniformly show hints
      focusBox()
    }
  }
}


// https://stackoverflow.com/questions/4233265/contenteditable-set-caret-at-the-end-of-the-text-cross-browser/4238971#4238971
function placeCaretAtEnd(el) {
    el.focus();
    if (typeof window.getSelection != "undefined"
            && typeof document.createRange != "undefined") {
        var range = document.createRange();
        range.selectNodeContents(el);
        range.collapse(false);
        var sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
    } else if (typeof document.body.createTextRange != "undefined") {
        var textRange = document.body.createTextRange();
        textRange.moveToElementText(el);
        textRange.collapse(false);
        textRange.select();
    }
}

function uniq() {
  return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15)
}