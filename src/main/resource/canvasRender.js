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
				x: e.clientX - mapCanvas.offsetLeft,
				y: e.clientY - mapCanvas.offsetTop
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
			
			var coordFinal = getBearingInverse(ref_lon, ref_lat, mousePos.x, mousePos.y, zm, 300, 500);
			
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
				
				aX = coordinatesA['x'] - mousePos.x;
				bX = coordinatesB['x'] - mousePos.x;
				aY = coordinatesA['y'] - mousePos.y;
				bY = coordinatesB['y'] - mousePos.y;
				
				console.log("# [" + vertexA['longitude'] + " : " + vertexA['latitude']);
				console.log("C [" + coordFinal['lon'] + " : " + coordFinal['lat']);
				console.log("Z [" + (vertexA['longitude'] - coordFinal['lon']) + " : " + (vertexA['latitude'] - coordFinal['lat']));
				
				lineAX = bX - aX;
				lineAY = bY - aY;
				
				slopeA = lineAX / lineAY;
				offsetA = slopeA * aX;
				slopeB = 1 / slopeA;
				offsetB = 0;
				
				xSect = (offsetB - offsetA) / (slopeA - slopeB);
				
				mDistance = Math.abs(xSect) ; // Not Entirely Accurate!!
				
				if(distance > mDistance)
				{
					// Need to pad in mDistance??  Or, use else statements for out of bounds values
					if((aX >= 0 && 0 >= bX) || (bX >= 0 && 0 >= aX)) 
					{
						chosenEdge = edge;
						distance = mDistance;
						// console.log("  [" + edge + "] " + coordinatesA['x'] + " :: " + coordinatesB['x'] + " - " + coordinatesA['y'] + " : " + coordinatesB['y'] + " : " + mousePos.x + ", " + mousePos.y);
					}
					else
					{
						dstA = Math.sqrt(Math.pow(aX, 2) + Math.pow(aY, 2));
						dstB = Math.sqrt(Math.pow(bX, 2) + Math.pow(bY, 2));
						mDst = (dstA<dstB)?dstA:dstB;
						if(distance < mDst)
						{
							chosenEdge = edge;
							distance = mDst;
							// console.log("# [" + edge + "] " + coordinatesA['x'] + " :: " + coordinatesB['x'] + " - " + coordinatesA['y'] + " : " + coordinatesB['y'] + " : " + mousePos.x + ", " + mousePos.y);
						}
					}
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

					var weightRange = edgeObj.weight/10;
					var confidenceRange = edgeObj.confidence;
					weightScore = Math.round(weightRange * 254);
					confidenceScore = Math.round(Math.abs(confidenceRange-1) * 254);
					if(confidenceScore <= 0)
					{
						confidenceScore = 0;
					} else if (confidenceScore > 254)
					{
						confidenceScore = 254;
					}
					if(weightScore < confidenceScore)
					{
						weightScore = confidenceScore;
					}
					gg = confidenceScore - Math.round((weightRange * confidenceScore) / 2);
					ggHex = ("00" + gg.toString(16)).substr(-2);
					
					rr = (confidenceScore>weightScore)?confidenceScore:weightScore;
					rrHex = ("00" + rr.toString(16)).substr(-2);
					
					confidenceHex = ("00" + confidenceScore.toString(16)).substr(-2);
					if(jsonObj["mapEdges"][edge]["marked"] == true)
					{
						hxResultStroke = "FF";
					}
					else
					{
						hxResultStroke = "00";
					}
					colorHex = rrHex + ggHex + "00ff";
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

function getBearingInverse(rlon, rlat, x, y, zoom, xOffset, yOffset)
{
	var coords = new Object();
		coords['lon'] = ((((x - 155)/0.725)-xOffset)/zoom) + rlon;
		coords['lat'] = -((((y/0.9)-yOffset)/zoom)-rlat); 
		
	return coords;
}