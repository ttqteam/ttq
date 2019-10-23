var box = initInputBox($('#tokenbox-cont'), loadHints, loadEntries, hintSelected)

// maybe hide global vars?
var activeHint = null
var activeModifiers = []
var chart = null
var sessionId = generateUID()
var currentRequest = null

// take query from URL
$(window).on('popstate', processUrl)
processUrl()

function processUrl() {
  var params = {}
  document.location.search.substr(1).split('&').forEach(function(q) {
    var r = q.split('=')
    if (r[0] && r[1])
      params[r[0].toString()] = decodeURIComponent(r[1].toString().replace(/\+/g, '%20'))
  })
  if (params['q'])
    box.setText(params['q'])
}

function hintSelected(hint) {
  // show query in URL
  var txt = encodeURIComponent(hint.tokens.map(function(t) { return t.text }).join(' ')).replace(/%20/g, '+')
  history.pushState(null, txt, "/?q="+txt)

  activeHint = hint
  runQuery()
}

function modifierSelected(modifier) {
  activeModifiers = [modifier]
  runQuery()
}

// todo - handle multiple queries running in parallel
function runQuery() {
  loadData(function(result) {
    showModifiers(result.modifiers)
    drawChart(result.title, result.series, result.values, result.chartType)
    // remember active modifiers so that when new hint is selected we send modifiers which were active last time
    activeModifiers = result.modifiers.filter(function(m) { return m.active }).map(function(m) { return m.id })
  },
  activeHint, activeModifiers)
}

function loadHints(hint, success) {
    if (currentRequest)
        currentRequest.abort()
    currentRequest = $.ajax({
        url: "/hint",
        type: 'post',
        data: JSON.stringify(convertToServer(hint)),
        dataType: 'json',
        contentType: 'application/json',
        success: function(hints) {
          // server returns 204 == 'nocontent' if request was cancelled, and 'success' callback will not be called
          success(convertFromServer(hints))
        }
    })
}

function loadEntries(placeholderToken, success) {
    return $.ajax({
        url: "/placeholder?param=" + placeholderToken.word,
        type: 'get',
        dataType: 'json',
        contentType: 'application/json',
        success: function(entries) { success(convertTokenListFromServer(entries)) }
    })
}

function loadData(success, hint, modifiers) {
//    var results = {
//      data: {
//        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug'],
//        data: [23,98,1,543,32,332,98,124]
//      }
//    }
//    success(results)
    $.ajax({
        url: "/hint/query",
        type: 'post',
        data: JSON.stringify({tokens: hint.tokens, modifiers: modifiers, sessionId:sessionId}),
        dataType: 'json',
        contentType: 'application/json',
        success: function(res) { success(res) }
    })
}

function convertToServer(hint) {
  return {tokens: hint.tokens, editedToken:hint.editedToken, sessionId:sessionId}
}

function convertFromServer(hints) {
  return _.map(hints, function(hint) {
    return {tokens: hint.tokens, data: null}
  })
}

function showModifiers(modifiers) {
  var $modifiers = $('#modifiers')
  $modifiers.empty()
  modifiers.forEach(function(m) {
    if (!m.active) {
      var $a = $("<a href=''>" + m.label + "</a>").click(function() { modifierSelected(m.id); return false })
      var $m = $("<span class='modifier'></span>")
      $m.append($a)
    }
    else {
      var $m = $("<span class='modifier'>" + m.label + "</span>")
    }
    $modifiers.append($m)
  })
}

function drawChart(title, series, values, chartType) {
    if (chart) chart.destroy() // dispose old chart
    var $results = $('#results')
    $results.empty()

    $results.append($('<div class="chartTitle">' + title + '</div>'))

    if (chartType == 'scalar') {
      var units = series[0].units
      var $scalar = $('<div class="scalar">' + values[0][0] + (units ? '&nbsp;' + units : '') + '</div>')
      $results.append($scalar)
    }
    else if (chartType == 'hbc' || chartType == 'vbc') {
      var $canvas = $('<canvas id="myChart" width="800" height="400"/>')
      $results.append($canvas)

      var chart = new Chart($canvas, {
        type: chartType == 'hbc' ? 'horizontalBar' : 'bar',
        data: {
          labels: values.map(function(vs) { return vs[0] }),
          datasets: [{
            data: values.map(function(vs) { return vs[1] }),
            backgroundColor: 'rgba(75, 192, 192, 0.4)',
            borderWidth: 1
          }]
        },
        options: {
          legend: {
            display: false
          },
          scales: {
            yAxes: [{
              ticks: {
                  beginAtZero:true
              }
            }]
          }
        }
      });
    }
    else if (chartType == 'table') {
      var ary = [], o = 0
      ary[o++] = '<table class="table table-borderless table-sm"><thead><tr>'
      for (var i=0; i < series.length; i++) {
          ary[o++] = '<th>'
          ary[o++] = series[i].title
          ary[o++] = '</th>'
      }
      ary[o++] = '</tr></thead><tbody>'

      for (var i=0; i<values.length; i++) {
        ary[o++] = '<tr>'
        for (var j=0; j < values[i].length; j++) {
          ary[o++] = '<td>'
          ary[o++] = values[i][j]
          ary[o++] = '</td>'
        }
        ary[o++] = '</tr>'
      }
      ary[o++] = '</tbody></table>'

      $results.html(ary.join(''));
    }
}

function generateUID() {
    var firstPart = (Math.random() * 46656) | 0;
    var secondPart = (Math.random() * 46656) | 0;
    firstPart = ("000" + firstPart.toString(36)).slice(-3);
    secondPart = ("000" + secondPart.toString(36)).slice(-3);
    return firstPart + secondPart;
}

