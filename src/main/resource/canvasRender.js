const mapCanvas = document.getElementById("mapCanvas");
const canvasInputBox = document.getElementById("canvasInputBox");
const canvasInput = document.getElementById("canvasInputBoxInput");
const tempLineCanvas = document.getElementById("tempLineCanvas");
const canvasA = mapCanvas.getContext("2d");

var chosenEdge = -1;
var listenerLoaded = false;
var inputData = { };

function loadJSON(data_uri, zoom)
{
	var xHttpRequest = new XMLHttpRequest();
	var jsonObj;

	if(!listenerLoaded)
	{
		mapCanvas.addEventListener('click', (e) => {
			const mousePos = {
				x: e.clientX - mapCanvas.offsetTop,
				y: e.clientY - mapCanvas.offsetLeft - 50 // -50 is hacky!  Need to factor in screen position as well!!
			};
			var zm = Math.pow(2, zoom);
			
			if(chosenEdge > 0)
			{
				if(canvasInput.value > 0)
				{
					inputData[chosenEdge] = canvasInput.value;
				}
				else
				{
					delete inputData[chosenEdge];
				}
				
				canvasInput.value = "";
			}
			
			var distance = 9999;
			for(edge in jsonObj["mapEdges"])
			{
				var vertexA = jsonObj["mapVertices"][jsonObj["mapEdges"][edge]["vertices"][0]];
				var vertexB = jsonObj["mapVertices"][jsonObj["mapEdges"][edge]["vertices"][1]];
				
				if(vertexA == null || vertexB == null) // Stupid catch-all because my server code sucks
				{
					// console.log("Skipped a Vertex on: " + edge);
					continue;
				}
				
				var coordinatesA = getBearing(ref_lon, ref_lat, vertexA["longitude"], vertexA["latitude"], zm, 300, 500);
				var coordinatesB = getBearing(ref_lon, ref_lat, vertexB["longitude"], vertexB["latitude"], zm, 300, 500);

				var min, max, minX, maxX, minY, maxY, mPos;
				if (coordinatesA['x']<coordinatesB['x'])
				{
					minX = coordinatesA['x'];
					maxX = coordinatesB['x'];
					minY = coordinatesA['y'];
					maxY = coordinatesB['y'];
					mPos = mousePos.x;
				}
				else if (coordinatesA['x']<coordinatesB['x'])
				{
					minX = coordinatesB['x'];
					maxX = coordinatesA['x'];
					minY = coordinatesB['y'];
					maxY = coordinatesA['y'];
					mPos = mousePos.x;
				} else if (coordinatesA['y']<coordinatesB['y'])
				{
					minX = coordinatesA['x'];
					maxX = coordinatesB['x'];
					minY = coordinatesA['y'];
					maxY = coordinatesB['y'];
					mPos = mousePos.y;
				}
				else
				{
					minX = coordinatesB['x'];
					maxX = coordinatesA['x'];
					minY = coordinatesB['y'];
					maxY = coordinatesA['y'];
					mPos = mousePos.y;
				}
				
				if(minX == maxX)
				{
					min = minY;
					max = maxY;
				}
				else
				{
					min = minX;
					max = maxX;
				}
				
				// CrossPoint - the X-Value position on the line (only works for straight lines.)
				var crossPoint = (max - mPos) / (max - min);
				var xRange = Math.abs(maxX - minX);
				var yRange = Math.abs(maxY - minY);
				
				if(crossPoint >= 0 && crossPoint <= 1)
				{
					// Need stricter box testing
					var xPt = (xRange * crossPoint) + minX;
					var yPt = (yRange * crossPoint) + minY;
					
					var xDst = xPt - mousePos.x;
					var yDst = yPt - mousePos.y;
					
					var finalDst = Math.sqrt(Math.pow(xDst, 2) + Math.pow(yDst, 2));
					// range between xPt; mosue
					
					var nearestPoint = { x: xPt, y: yPt};

					if(distance > finalDst)
					{
						chosenEdge = edge;
						distance = finalDst;
					}
					
					var chosenEdgeVertexA = jsonObj["mapVertices"][jsonObj["mapEdges"][chosenEdge]["vertices"][0]];
					var chosenEdgeVertexB = jsonObj["mapVertices"][jsonObj["mapEdges"][chosenEdge]["vertices"][1]];
					
					var coordinatesA = getBearing(ref_lon, ref_lat, chosenEdgeVertexA["longitude"], chosenEdgeVertexA["latitude"], zm, 300, 500);
					var coordinatesB = getBearing(ref_lon, ref_lat, chosenEdgeVertexB["longitude"], chosenEdgeVertexB["latitude"], zm, 300, 500);

					// console.log("Mousey: " + mousePos.x + ", " + mousePos.y + " : " + distance);
					// console.log("ToDraw: " + coordinatesA['x'] + ":" + coordinatesA['y'])
				}
			}

			const selectedRoad = document.getElementById("selectedRoadType");
			selectedRoad.innerHTML = "[" + chosenEdge + "] " + jsonObj["mapEdges"][chosenEdge]["mode"] + " w:" + jsonObj["mapEdges"][chosenEdge]["weight"] + " c:" + jsonObj["mapEdges"][chosenEdge]["confidence"];		
			if(typeof inputData[chosenEdge] != 'undefined')
			{
				canvasInput.value = inputData[chosenEdge];
			}
			else
			{
				canvasInput.value = "";
			}
			
			// FOR LOOP HERE
			var length = jsonObj["mapEdges"][chosenEdge]["vertices"].length;
			for(var i = 0; i < length-1; i++)
			{
				var chosenEdgeVertexA = jsonObj["mapVertices"][jsonObj["mapEdges"][chosenEdge]["vertices"][i]];
				var chosenEdgeVertexB = jsonObj["mapVertices"][jsonObj["mapEdges"][chosenEdge]["vertices"][i+1]];
				
				var coordinatesA = getBearing(ref_lon, ref_lat, chosenEdgeVertexA["longitude"], chosenEdgeVertexA["latitude"], zm, 300, 500);
				var coordinatesB = getBearing(ref_lon, ref_lat, chosenEdgeVertexB["longitude"], chosenEdgeVertexB["latitude"], zm, 300, 500);
				
				var lineWidth = 12;
				var dx = coordinatesB['x'] - coordinatesA['x'];
				var dy = coordinatesB['y'] - coordinatesA['y'];
				var rotation = Math.atan2(dy, dx);
				var lineLength = Math.sqrt(dx * dx + dy * dy);
				
				var xPos = (coordinatesA['x']+coordinatesB['x'])/2;
				var yPos = (coordinatesA['y']+coordinatesB['y'])/2;
				
				canvasInputBox.style.left = xPos;
				canvasInputBox.style.top = yPos;
				
				// canvasInput.style.left = xRange + xPos;
				// canvasInput.style.top = yRange + yPos;
				
				tempLineCanvas.getContext("2d").clearRect(0, 0, tempLineCanvas.width, tempLineCanvas.height);
				
				tempLineCanvasContext = tempLineCanvas.getContext("2d");
				tempLineCanvasContext.beginPath();
				tempLineCanvasContext.globalAlpha = 1;
				tempLineCanvasContext.translate(coordinatesA['x'], coordinatesA['y']);
				tempLineCanvasContext.rotate(rotation);
				tempLineCanvasContext.rect(0, -lineWidth / 2, lineLength, lineWidth);
				tempLineCanvasContext.translate(-coordinatesA['x'], -coordinatesA['y']);
				tempLineCanvasContext.fillStyle = "#FF00FF";
				tempLineCanvasContext.strokeStyle = "#0000FF";
				tempLineCanvasContext.fill();
				tempLineCanvasContext.stroke();
				tempLineCanvasContext.setTransform(1, 0, 0, 1, 0, 0);
				tempLineCanvasContext.closePath();
			}
		});
		listenerLoaded = true;
	}
	
	var drawn = 0;
	var notdrawn = 0; 
	
	xHttpRequest.onreadystatechange = function()
	{
		if (xHttpRequest.readyState == 4  )
		{
			// Javascript function JSON.parse to parse JSON data
			try
			{
				jsonObj = JSON.parse(xHttpRequest.responseText);
			}
			catch(e)
			{
				document.getElementById("mapCanvas").write(e);
			}

			ref_lon = jsonObj["focus"]["longitude"];
			ref_lat = jsonObj["focus"]["latitude"];
			
			document.getElementById("mapCanvas").getContext("2d").clearRect(0, 0, mapCanvas.width, mapCanvas.height);
			
			var zm = Math.pow(2, zoom);
			for(edge in jsonObj["mapEdges"])
			{
				var length = jsonObj["mapEdges"][edge]["vertices"].length;
				for(var i = 0; i < length-1; i++)
				{
					var vertexAId = jsonObj["mapEdges"][edge]["vertices"][i];
					var vertexBId = jsonObj["mapEdges"][edge]["vertices"][i+1];
					
					var edgeObj = jsonObj["mapEdges"][edge];
					
					var vertexA = jsonObj["mapVertices"][vertexAId];
					var vertexB = jsonObj["mapVertices"][vertexBId];
					
					if(vertexA == null || vertexB == null) // Stupid catch-all because my server code sucks
					{
						console.log("Skipped a Vertex on: " + edgeObj);
						continue;
					}
				
					var coordinatesA = getBearing(ref_lon, ref_lat, vertexA["longitude"], vertexA["latitude"], zm, 300, 500);
					var coordinatesB = getBearing(ref_lon, ref_lat, vertexB["longitude"], vertexB["latitude"], zm, 300, 500);
					
					// console.log(coordinatesA['x'] + " -- " + coordinatesA['y'] + " :: " + coordinatesB['x'] + " -- " + coordinatesB['y']);
				
					if((coordinatesA['x'] > mapCanvas.width && coordinatesB['x'] > mapCanvas.width)
					|| (coordinatesA['x'] < 0 && coordinatesB['x'] < 0)
					|| (coordinatesA['y'] > mapCanvas.height && coordinatesB['y'] > mapCanvas.height)
					|| (coordinatesA['y'] < 0 && coordinatesB['y'] < 0))
					{
						notdrawn += 1;
						continue;
						// remove element if not drawn
					}
	
					var dx = coordinatesB['x'] - coordinatesA['x'];
					var dy = coordinatesB['y'] - coordinatesA['y'];
					var rotation = Math.atan2(dy, dx);
					var lineLength = Math.sqrt(dx * dx + dy * dy);

					drawn += 1;
					// canvasA.moveTo(coordinatesA['x'], coordinatesA['y']);
					// canvasA.lineTo(coordinatesB['x'], coordinatesB['y']);
					// canvasA.lineWidth = 5;
					
					canvasA.save();
					canvasA.beginPath();
	
					canvasA.globalAlpha = 1;

					weightScore = Math.round(edgeObj.weight * 25);
					confidenceScore = Math.round(Math.abs(edgeObj.confidence-1) * 255);
					if(confidenceScore <= 0)
					{
						confidenceScore = 0;
					} else if (confidenceScore > 255)
					{
						confidenceScore = 255;
					}
					if(weightScore < confidenceScore)
					{
						// differential = (255-confidenceScore) * confidenceScore;
						// weightScore = confidenceScore + differential;
						if(weightScore < confidenceScore) weightScore = confidenceScore;
						else if(weightScore > 255) weightScore = 255;
					}
					weightHex = ("00" + weightScore.toString(16)).substr(-2);
					confidenceHex = ("00" + confidenceScore.toString(16)).substr(-2);
					if(jsonObj["mapEdges"][edge]["marked"] == true)
					{
						hxResultStroke = "FF";
					}
					else
					{
						hxResultStroke = "00";
					}
					colorHex = weightHex + confidenceHex + "00";
					var lineWidth = 8;
					var strokeAlpha = "80";
					if(edgeObj["mode"] == "primary" || edgeObj["mode"] == "secondary" || edgeObj["mode"] == "rail")
					{
						lineWidth = 12;
						strokeAlpha = "FF";
					}

					canvasA.translate(coordinatesA['x'], coordinatesA['y']);
					canvasA.rotate(rotation);
					canvasA.rect(0, -lineWidth / 2, lineLength, lineWidth);
					canvasA.translate(-coordinatesA['x'], -coordinatesA['y']);
					canvasA.fillStyle = "#" + colorHex;
					canvasA.strokeStyle = "#" + hxResultStroke + hxResultStroke + "00" + strokeAlpha;
	
					canvasA.fill();
					canvasA.stroke();
					canvasA.restore();
					canvasA.closePath();
				}
			} 
			
			canvasA.beginPath();
			canvasA.fillText("Drawn: " + drawn, 20, 20);
			canvasA.fillText("Not Drawn: " + notdrawn, 20, 40);
			canvasA.closePath();
		}
	}

	xHttpRequest.open("GET", data_uri, true);
	xHttpRequest.send();
}

function submitDataForm(data_uri)
{
	if(chosenEdge > 0)
	{
		if(canvasInput.value > 0)
		{
			inputData[chosenEdge] = canvasInput.value;
		}
		else
		{
			delete inputData[chosenEdge];
		}
	}
	
	var request = new XMLHttpRequest();
	
	request.open("POST", data_uri, false);
	var rawData = "";
	for(item in inputData)
	{
		rawData += item + "=" + inputData[item] + "&";
	}
	console.log(rawData);
	request.send(rawData);
	inputData = { };
	canvasInput.value = "";
	canvasInputBox.style.left = "-100pt";
	canvasInputBox.style.top = "-100pt";
	tempLineCanvas.getContext("2d").clearRect(0, 0, tempLineCanvas.width, tempLineCanvas.height);
	refresh();
}

function getBearing(rlon, rlat, lon, lat, zoom, xOffset, yOffset)
{
	var coords = new Object();
		coords['x'] = ((((lon - rlon)*zoom)+xOffset)*0.725)+155;
		coords['y'] = (((rlat - lat)*zoom)+yOffset)*0.9;
		
	return coords;
}