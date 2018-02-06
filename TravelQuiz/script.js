var ANIMATION_DURATION = 500;
var RADIUS = 150;
var LIMIT = 80;
var PROPERTY_BLACKLIST = ["P625", "P791", "P1705", "P1813", "P131", "P373",
                          "P227", "P214", "P421", "P2427", "P213", "P1281",
                          "P2503", "P439", "P2581", "P3417", "P982", "P814",
                          "P646", "P1566", "P809", "P910", "P402", "P935",
                          "P856", "P31", "P1612", "P973", "P1183", "P242",
                          "P361"
                         ];
var IMAGE_TYPES = ["Bild", "Logo", "Wappen"];

var lastAction = new Date().getTime();

function searchGps() {
    $("#searching").show(ANIMATION_DURATION);
    $("#loading").hide(ANIMATION_DURATION);
    $("#question").hide(ANIMATION_DURATION);
    $("#error").hide(ANIMATION_DURATION);

    lastAction = new Date().getTime();

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(gpsFound, showGpsError);
    } else {
        errorMessage("Kein GPS unterstützt");
    }
}

function showGpsError(error) {
    switch(error.code) {
        case error.PERMISSION_DENIED:
            errorMessage("GPS: Keine Berechtigung");
            break;
        case error.POSITION_UNAVAILABLE:
            errorMessage("GPS: Keine Position gefunden");
            break;
        case error.TIMEOUT:
            errorMessage("GPS: Timeout");
            break;
        case error.UNKNOWN_ERROR:
            errorMessage("GPS: Unbekannter Fehler");
            break;
    }
}

var position;
function gpsFound(_position) {
    position = _position;
    window.setTimeout(loadQuestion, lastAction + 1000 - new Date().getTime());
}

function loadQuestion() {
    lastAction = new Date().getTime();

    $("#searching").hide(ANIMATION_DURATION);
    $("#loading").show(ANIMATION_DURATION);
    $("#question").hide(ANIMATION_DURATION);
    $("#error").hide(ANIMATION_DURATION);
    point = position.coords.longitude + " " + position.coords.latitude;
    query = `
    SELECT DISTINCT ?place ?location WHERE {
      SERVICE wikibase:around {
        ?place wdt:P625 ?location .
        bd:serviceParam wikibase:center "Point(` + point + `)"^^geo:wktLiteral.
        bd:serviceParam wikibase:radius "`+RADIUS+`".
        bd:serviceParam wikibase:distance ?dist.
      }.
    }
    ORDER BY ?dist
    LIMIT `+LIMIT+`
    `;
    $.ajax({
        url: "https://query.wikidata.org/sparql?format=json&query=" + encodeURI(query),
        success: function(itemsInRange) {
            console.log(itemsInRange);
            loadProperties(itemsInRange);
        },
        error: function(xhr, status, error) {
            errorMessage("Network error");
        }
    });
}

function loadProperties(itemsInRange) {
    items = itemsInRange.results.bindings;
    itemString = "";
    for (i = 0; i < items.length; i++) {
        itemString += items[i].place.value.replace("http://www.wikidata.org/entity/", "wd:") + " ";
    }
    query = `
    SELECT ?property (COUNT(?property) AS ?count) WHERE {
      {
        SELECT (SAMPLE(?item) AS ?item) (SAMPLE(?property) AS ?property) WHERE {
          VALUES ?item {
            ` + itemString + `
          }
          ?item ?p ?statement.
          ?property wikibase:claim ?p.
          MINUS { ?property wikibase:propertyType wikibase:ExternalId. }
        }
        GROUP BY ?item ?property
      }
    }
    GROUP BY ?property ?propertyLabel
    HAVING(?count > 1)
    ORDER BY DESC(?count)
    `;
    $.ajax({
        url: "https://query.wikidata.org/sparql?format=json&query=" + encodeURI(query),
        success: function(mostUsedProperties) {
            console.log(mostUsedProperties);
            loadItemsWithProperties(mostUsedProperties, itemString);
        },
        error: function(xhr, status, error) {
            errorMessage("Network error");
        }
    });
}

function loadItemsWithProperties(mostUsedProperties, itemString) {
    properties = mostUsedProperties.results.bindings;
    propertyString = "";
    for (i = 0; i < properties.length; i++) {
        if (properties[i].count.value <= 1) {
            continue;
        } else if (PROPERTY_BLACKLIST.includes(properties[i].property.value.replace("http://www.wikidata.org/entity/", ""))) {
            continue;
        }
        propertyString += properties[i].property.value.replace("http://www.wikidata.org/entity/", "wdt:") + " ";
    }

    query = `
    SELECT ?itemLabel ?pLabel ?statementLabel WHERE {
        VALUES ?item {
            ` + itemString + `
        }
        VALUES ?property {
            ` + propertyString + `
        }
        ?item ?property ?statement.
        ?p wikibase:directClaim ?property.
        SERVICE wikibase:label { bd:serviceParam wikibase:language "de,en,fr". }
    }
    `;
    $.ajax({
        url: "https://query.wikidata.org/sparql?format=json&query=" + encodeURI(query),
        success: function(questionItems) {
            console.log(questionItems);

            window.setTimeout(function() {
                displayQuestion(questionItems);
            }, lastAction + 1000 - new Date().getTime());
        },
        error: function(xhr, status, error) {
            errorMessage("Network error");
        }
    });
}

function shuffle(a) {
    var j, x, i;
    for (i = a.length - 1; i > 0; i--) {
        j = Math.floor(Math.random() * (i + 1));
        x = a[i];
        a[i] = a[j];
        a[j] = x;
    }
}

var answerIndex = -1;
var questionIndex = 0;
var possibleQuestions;

function displayQuestion(questionItems) {
      $("#searching").hide(ANIMATION_DURATION);
      $("#loading").hide(ANIMATION_DURATION);
      $("#question").hide(ANIMATION_DURATION);
      $("#error").hide(ANIMATION_DURATION);
      items = questionItems.results.bindings;
      shuffle(items);

      possibleQuestions = [];
      answerIndex = -1;
      questionIndex = 0;
      for (i = 0; i < items.length; i++) {
          itm = items[i];
          index = -1;
          for (k = 0; k < possibleQuestions.length; k++) {
              if (possibleQuestions[k].name == itm.pLabel.value) {
                  index = k;
              }
          }
          if (index == -1) {
              possibleQuestions.push({name: itm.pLabel.value, items: [], statements: [], questions: []});
              index = possibleQuestions.length - 1;
          }
          if (!possibleQuestions[index].items.includes(itm.itemLabel.value)
              && !possibleQuestions[index].statements.includes(itm.statementLabel.value)
              && possibleQuestions[index].questions.length < 5) {
                  question = {item: itm.itemLabel.value, statement: itm.statementLabel.value};
                  possibleQuestions[index].items.push(question.item);
                  possibleQuestions[index].statements.push(question.statement);
                  possibleQuestions[index].questions.push(question);
          }
      }
      possibleQuestions.sort(function(a, b){ return b.questions.length - a.questions.length});
      console.log(possibleQuestions);
      nextQuestion();
}

function nextQuestion() {
    $("#question").hide(ANIMATION_DURATION/2);
    window.setTimeout(nextQuestionLoader, ANIMATION_DURATION/2);
}

function nextQuestionLoader() {
    if (questionIndex >= possibleQuestions.length
        || possibleQuestions[questionIndex].questions.length <= 1) {
        errorMessage("Keine Fragen mehr übrig");
        return;
    }

    question = possibleQuestions[questionIndex];
    questionIndex++;

    i = 0;
    do {
      answerIndex = Math.floor(Math.random() * question.questions.length);
      statement = question.questions[answerIndex].statement;

      i++;
      if (i == 10) {
        break;
      }
    } while (question.questions[answerIndex].item.includes(statement)
              || statement.includes(question.questions[answerIndex].item));


    if (IMAGE_TYPES.includes(question.name)) {
        if (!statement.endsWith(".svg")) {
            console.log(statement);
            thumbnail = decodeURI(statement).replace("http://commons.wikimedia.org/wiki/Special:FilePath/", "");
            thumbnail = thumbnail.replace(/ /g, "_");
            thumbnail = encodeURI(thumbnail);
            console.log(thumbnail);
            thumbnail = "https://upload.wikimedia.org/wikipedia/commons/thumb/"
                        + md5(thumbnail)[0] + "/"+ md5(thumbnail)[0] + md5(thumbnail)[1] + "/" + thumbnail
                        + "/200px-" + thumbnail;
            console.log(statement);
            statement = "<img src=\"" + thumbnail + "\" onerror=\"this.src='" + statement + "';\" />";
        } else {
            statement = "<img src='" + statement + "' />";
        }
    } else if(/\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\dZ/.test(statement)) {
        statement = statement.substring(0, 4);
    }
    $("#question-property").html(question.name.replace("<", "&lt;") + ":<br />" + statement);
    $("#answers").text("");
    for (i = 0; i < question.questions.length; i++) {
        q = $('<a id="answer' + i + '" href="javascript:answer(' + i + ')">'
              + question.questions[i].item + '</a>');
        $("#answers").append(q);
    }
    $("#question").show(ANIMATION_DURATION/2);
}

function errorMessage(message) {
      $("#searching").hide(ANIMATION_DURATION);
      $("#loading").hide(ANIMATION_DURATION);
      $("#question").hide(ANIMATION_DURATION);
      $("#error").show(ANIMATION_DURATION);
      $("#error span").text(message);
}

function answer(num) {
    if (num == answerIndex) {
        $("#answer"+num).addClass("right");
        window.setTimeout(nextQuestion, 300);
    } else {
        $("#answer"+num).addClass("wrong");
    }
}
