const mapCanvas = document.getElementById("mapCanvas");
const canvasInputBox = document.getElementById("canvasInputBox");
const canvasInput = document.getElementById("canvasInputBoxInput");
const tempLineCanvas = document.getElementById("tempLineCanvas");
const tempLineCanvasContext = tempLineCanvas.getContext("2d");
const canvasA = mapCanvas.getContext("2d");
const miniMapSize = 48;
const miniMapSegment = miniMapSize/3;
const debugAlpha = "CC";
const canvasInputPosition = {"x": 100, "y": 40};
var debug = true;

var chosenEdge = -1;
var listenerLoaded = false;
var inputData = { };
var offsetMousePosX = 0;
var offsetMousePosY = 0;

function loadJSON(data_uri, zoom)
{
	var xHttpRequest = new XMLHttpRequest();
	var jsonObj;

	if(!listenerLoaded)
	{
		mapCanvas.addEventListener('click', (e) => {
			const mousePos = {
				x: e.clientX - mapCanvas.offsetLeft + window.scrollX,
				y: e.clientY - mapCanvas.offsetTop + window.scrollY
			};
			offsetMousePosX = mousePos.x;
			offsetMousePosY = mousePos.y;
			var maxEdgeLength = 0;

			var zm = Math.pow(2, zoom);

			var coordFinal = getBearingInverse(ref_lon, ref_lat, offsetMousePosX, offsetMousePosY, zm, 300, 500);
			
			var distance = 9999;
			var finalX = 0;
			var finalY = 0;
			
			tempLineCanvas.getContext("2d").clearRect(0, 0, tempLineCanvas.width, tempLineCanvas.height);
			edgeCollection = { "primary": {}, "secondary": {}, "tertiary": {}, "residential": {}};
			
			hxVal = 0;
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
				
				var mouseCoord = getBearingInverse(ref_lon, ref_lat, offsetMousePosX, offsetMousePosY, zm, 300, 500);
				roadSlope = (vertexB["latitude"] - vertexA["latitude"])/(vertexB["longitude"] - vertexA["longitude"])
				inverseRoadSlope = -1/roadSlope;
				
				// Intersect the slopes.
				roadZeroPointOffset = (roadSlope * vertexB["latitude"]) - vertexA["latitude"];
				mouseInversePointOffset = (inverseRoadSlope * mouseCoord["lon"]) - vertexA["latitude"];
				
				lonSect = ((mouseInversePointOffset - mouseCoord["lat"]) / (roadSlope - inverseRoadSlope)) + vertexA["latitude"];
				latSect = inverseRoadSlope * lonSect;
				intersectCoords = getBearing(ref_lon, ref_lat, lonSect, latSect, zm, 300, 500);
				
				aX = -(coordinatesA['x'] - offsetMousePosX);
				aY = -(coordinatesA['y'] - offsetMousePosY);
				bX = -(coordinatesB['x'] - offsetMousePosX);
				bY = -(coordinatesB['y'] - offsetMousePosY);
				cX = coordinatesB['x'] - coordinatesA['x'];
				cY = coordinatesB['y'] - coordinatesA['y'];
				dX = coordinatesB['x'] - offsetMousePosX;
				dY = coordinatesB['y'] - offsetMousePosY;
				
				lineAX = bX - aX;
				lineAY = bY - aY;
				
				directionRad = Math.atan(lineAX / lineAY);
				crossPoint = (Math.PI/2) + directionRad;

				ySect = 50 * Math.cos(crossPoint);
				xSect = 50 * Math.sin(crossPoint);

				xSect = xSect + coordinatesA['x'];
				ySect = ySect + coordinatesA['y'];
				
				lineANormalized = normalize(cX, cY, 1);
				lineBNormalized = normalize(dX, dY, 1);
				dotProduct = vDotProduct(lineANormalized['x'], lineANormalized['y'], lineBNormalized['x'], lineBNormalized['y']);
				vLenA = vLength(cX, cY);
				vLenB = vLength(dX, dY) * dotProduct;
				
				ratio = vLenB / vLenA;
				xSect = coordinatesB['x'] - (cX * ratio);
				ySect = coordinatesB['y'] - (cY * ratio);

				mDistance = Math.sqrt(Math.pow(xSect - mousePos['x'], 2) + Math.pow(ySect - mousePos['y'], 2));

				if(ratio <= 1 && ratio >= 0)
				{
					if(mDistance < 20)
					{
						if(edgeCollection[jsonObj["mapEdges"][edge]["mode"]] == null)
						{
							edgeCollection[jsonObj["mapEdges"][edge]["mode"]] = {}; 
						}
						edgeCollection[jsonObj["mapEdges"][edge]["mode"]][edge] = true;
						maxEdgeLength = (jsonObj["mapEdges"][edge]["distance"]>maxEdgeLength)? jsonObj["mapEdges"][edge]["distance"]:maxEdgeLength;
					}

					if(debug == true)
					{
						hxVal = hxVal + 50;
						hxC = ("00" + hxVal.toString(16)).substr(-2);
						
						xPos = ((coordinatesA['x']+coordinatesB['x'])/2)+10;
						yPos = ((coordinatesA['y']+coordinatesB['y'])/2);
						
						tempLineCanvasContext.beginPath();
						tempLineCanvasContext.moveTo(coordinatesA['x'], coordinatesA['y']);
						tempLineCanvasContext.lineTo(coordinatesB['x'], coordinatesB['y']);
						tempLineCanvasContext.strokeStyle = "#00" + hxC + hxC + debugAlpha;
						tempLineCanvasContext.lineWidth = 5;
						tempLineCanvasContext.stroke();
						tempLineCanvasContext.closePath();
						
						tempLineCanvasContext.beginPath();
						tempLineCanvasContext.moveTo(mousePos['x'], mousePos['y']);
						tempLineCanvasContext.lineTo(xSect, ySect);
						tempLineCanvasContext.strokeStyle = "#" + hxC + "0000" + debugAlpha;
						tempLineCanvasContext.lineWidth = 1;
						
						tempLineCanvasContext.fillStyle = "#FF0000" + debugAlpha;
						tempLineCanvasContext.fillText(Math.round(mDistance*100)/100, xSect, ySect);
						tempLineCanvasContext.stroke();
						tempLineCanvasContext.closePath();
						
						tempLineCanvasContext.beginPath();
						tempLineCanvasContext.fillStyle = "#8000FF" + debugAlpha;
						tempLineCanvasContext.fillText(edge, xPos, yPos);
						tempLineCanvasContext.stroke();
						tempLineCanvasContext.closePath();
					}
					
					if(distance > mDistance)
					{
						chosenEdge = edge;
						distance = mDistance;
						finalX = xSect;
						finalY = ySect;
					}
				}

				var confidenceRange = jsonObj["mapEdges"][edge].confidence;
				if(confidenceRange < 0.5)
				{
					var mPosX = ((coordinatesA['x'] + coordinatesB['x']) / 2) - 8;
					var mPosY = ((coordinatesA['y'] + coordinatesB['y']) / 2) - 8;
					var cautionImg = document.getElementById("lowConfidence");
					tempLineCanvasContext.drawImage(cautionImg, mPosX, mPosY, 16, 16);
				}
			}
			
			if(chosenEdge > 0) 
			{
				if(edgeCollection[jsonObj["mapEdges"][chosenEdge]["mode"]] == null)
				{
					edgeCollection[jsonObj["mapEdges"][chosenEdge]["mode"]] = {}; 
				}
				edgeCollection[jsonObj["mapEdges"][chosenEdge]["mode"]][chosenEdge] = true;
				maxEdgeLength = (jsonObj["mapEdges"][chosenEdge]["distance"]>maxEdgeLength)? jsonObj["mapEdges"][chosenEdge]["distance"]:maxEdgeLength;
			}

			const selectedRoad = document.getElementById("selectedRoadType");
			
			strTable = "<table><tr><td colspan=\"1\"></td><td><img width=\"320\" src=\"/resource/RoadPictogram.png\"/></td></tr>";
			for(mode in edgeCollection)
			{
				if(Object.keys(edgeCollection[mode]).length == 0) continue;
				strTable += "<tr><td colspan=\"2\" style=\"background-color: #AAAAAA; text-align: center; font-weight: bold\">" + mode.charAt(0).toUpperCase() + mode.slice(1) + " road type</td></tr>";
				for(edge in edgeCollection[mode])
				{
					var centerX = jsonObj["mapEdges"][edge]["longitude"];
					var centerY = jsonObj["mapEdges"][edge]["latitude"];
					
					var edgeObject = jsonObj["mapEdges"][edge];
					var edgesA = jsonObj["mapVertices"][jsonObj["mapEdges"][edge]["vertices"][0]]["edges"];
					var edgesB = jsonObj["mapVertices"][jsonObj["mapEdges"][edge]["vertices"][1]]["edges"];
					
					var xMag = -(centerX - jsonObj["mapVertices"][jsonObj["mapEdges"][edge]["vertices"][0]]["longitude"]);
					var yMag = centerY - jsonObj["mapVertices"][jsonObj["mapEdges"][edge]["vertices"][0]]["latitude"];
					
					var centerMag = normalize(xMag, yMag, (jsonObj["mapEdges"][edge]["distance"]/maxEdgeLength));
					var scale = maxEdgeLength / 2;  // vLength(xMag, yMag);
					
					var cx1 = (miniMapSize/2) + (centerMag["x"] * miniMapSegment);
					var cy1 = (miniMapSize/2) + (centerMag["y"] * miniMapSegment);
					var cx2 = (miniMapSize/2) - (centerMag["x"] * miniMapSegment);
					var cy2 = (miniMapSize/2) - (centerMag["y"] * miniMapSegment);
					
					sideLines = "";
					for(edgeItem in edgesA)
					{
						if(edge == edgesA[edgeItem] || typeof jsonObj["mapEdges"][edgesA[edgeItem]] == 'undefined') continue;
						currentSelectedEdge = jsonObj["mapEdges"][edgesA[edgeItem]];
						
						var x1 = (miniMapSize/2) + (-(centerX - jsonObj["mapVertices"][jsonObj["mapEdges"][edgesA[edgeItem]]["vertices"][0]]["longitude"]) / scale * miniMapSegment);
						var y1 = (miniMapSize/2) + ((centerY - jsonObj["mapVertices"][jsonObj["mapEdges"][edgesA[edgeItem]]["vertices"][0]]["latitude"]) / scale * miniMapSegment);
						var x2 = (miniMapSize/2) + (-(centerX - jsonObj["mapVertices"][jsonObj["mapEdges"][edgesA[edgeItem]]["vertices"][1]]["longitude"]) / scale * miniMapSegment);
						var y2 = (miniMapSize/2) + ((centerY - jsonObj["mapVertices"][jsonObj["mapEdges"][edgesA[edgeItem]]["vertices"][1]]["latitude"]) / scale * miniMapSegment);
						
						sideLines += "<line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" style=\"stroke:rgb(0,0,0);stroke-width:2\"/>";
					}
					for(edgeItem in edgesB)
					{
						if(edge == edgesB[edgeItem] || typeof jsonObj["mapEdges"][edgesB[edgeItem]] == 'undefined') continue;
						currentSelectedEdge = jsonObj["mapEdges"][edgesB[edgeItem]];
						
						var x1 = (miniMapSize/2) + (-(centerX - jsonObj["mapVertices"][jsonObj["mapEdges"][edgesB[edgeItem]]["vertices"][0]]["longitude"]) / scale * miniMapSegment);
						var y1 = (miniMapSize/2) + ((centerY - jsonObj["mapVertices"][jsonObj["mapEdges"][edgesB[edgeItem]]["vertices"][0]]["latitude"]) / scale * miniMapSegment);
						var x2 = (miniMapSize/2) + (-(centerX - jsonObj["mapVertices"][jsonObj["mapEdges"][edgesB[edgeItem]]["vertices"][1]]["longitude"]) / scale * miniMapSegment);
						var y2 = (miniMapSize/2) + ((centerY - jsonObj["mapVertices"][jsonObj["mapEdges"][edgesB[edgeItem]]["vertices"][1]]["latitude"]) / scale * miniMapSegment);
						
						sideLines += "<line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" style=\"stroke:rgb(0,0,0);stroke-width:2\"/>";
					}
					
					var edgeVertexA = jsonObj["mapVertices"][jsonObj["mapEdges"][edge]["vertices"][0]];
					var edgeVertexB = jsonObj["mapVertices"][jsonObj["mapEdges"][edge]["vertices"][1]];
					
					var subCoordinatesA = getBearing(ref_lon, ref_lat, edgeVertexA["longitude"], edgeVertexA["latitude"], zm, 300, 500);
					var subCoordinatesB = getBearing(ref_lon, ref_lat, edgeVertexB["longitude"], edgeVertexB["latitude"], zm, 300, 500);
					
					var defaultvalue = (inputData[edge]==null)?0:inputData[edge];
					var highlightHex = (edge == chosenEdge)?"FF":"00";
					
					svgLines = "";
					svgLines += "<line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" style=\"stroke:rgb(0,0,0);stroke-width:2\"/>";
					
					var lnRotate = Math.atan2(subCoordinatesB['y'] - subCoordinatesA['y'], subCoordinatesB['x'] - subCoordinatesA['x']);
					var lnLen = vLength(subCoordinatesB['x'] - subCoordinatesA['x'], subCoordinatesB['y'] - subCoordinatesA['y']);
					
					// HERE
					strTable += "<tr onClick=\"clearAndSelectEdge(1, " + subCoordinatesA['x'] + ", " + subCoordinatesA['y'] + ", " + lnLen + ", 12, " + lnRotate + ", tempLineCanvas)\" style=\"background-color: #80FFBB" + highlightHex + "\"><td><svg id=\"canvasEdge_" + edge + "\" width=\"" + miniMapSize + "\" height=\"" + miniMapSize + "\">"
					+ sideLines
					+ "<line x1=\"" + cx1 + "\" y1=\"" + cy1 + "\" x2=\"" + cx2 + "\" y2=\"" + cy2 + "\" style=\"stroke:rgb(255,0,0);stroke-width:6\"/>"
					+ "<line x1=\"" + cx1 + "\" y1=\"" + cy1 + "\" x2=\"" + cx2 + "\" y2=\"" + cy2 + "\" style=\"stroke:rgb(0,255,255);stroke-width:4\"/></svg></td>"
					+ "<td><input type=\"range\" min=\"0\" max=\"10\" value=\"" + defaultvalue + "\" class=\"slider\" id=\"roadInput_" + edge + "\" name=\"entry\" size=\"3\" style=\"width: 290px; background-color: #AAAAAA;\" onchange=\"updateEntry(" + edge + ", this.value)\" /></td></tr>";
				}
			}
			strTable += "</table>";
			selectedRoad.innerHTML = strTable;		
			
			var length = jsonObj["mapEdges"][chosenEdge]["vertices"].length;

			var chosenEdgeVertexA = jsonObj["mapVertices"][jsonObj["mapEdges"][chosenEdge]["vertices"][0]];
			var chosenEdgeVertexB = jsonObj["mapVertices"][jsonObj["mapEdges"][chosenEdge]["vertices"][1]];
			
			var coordinatesA = getBearing(ref_lon, ref_lat, chosenEdgeVertexA["longitude"], chosenEdgeVertexA["latitude"], zm, 300, 500);
			var coordinatesB = getBearing(ref_lon, ref_lat, chosenEdgeVertexB["longitude"], chosenEdgeVertexB["latitude"], zm, 300, 500);
			
			var lineWidth = 12;
			var eX = coordinatesB['x'] - coordinatesA['x'];
			var eY = coordinatesB['y'] - coordinatesA['y'];
			var rotation = Math.atan2(eY, eX);
			var lineLength = vLength(eX, eY);
			
			var xPos = (coordinatesA['x']+coordinatesB['x'])/2;
			var yPos = (coordinatesA['y']+coordinatesB['y'])/2;
			
			canvasInputBox.style.left = canvasInputPosition['x'];
			canvasInputBox.style.top = canvasInputPosition['y'];
			
			selectEdge(jsonObj["mapEdges"][chosenEdge], coordinatesA['x'], coordinatesA['y'], lineLength, lineWidth, rotation, tempLineCanvasContext);
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
					}
	
					var dx = coordinatesB['x'] - coordinatesA['x'];
					var dy = coordinatesB['y'] - coordinatesA['y'];
					var rotation = Math.atan2(dy, dx);
					var lineLength = Math.sqrt(dx * dx + dy * dy);

					drawn += 1;
					
					canvasA.save();
					canvasA.beginPath();
	
					canvasA.globalAlpha = 1;

					var weightRange = edgeObj.weight/10;
					var confidenceRange = edgeObj.confidence;
					weightScore = Math.round(weightRange * 254);

					confidenceScore = (confidenceRange <= 0.05)?255:0;
					
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
					canvasA.fillStyle = "#" + colorHex;
					canvasA.strokeStyle = "#" + hxResultStroke + hxResultStroke + "00" + strokeAlpha;
	
					canvasA.fill();
					canvasA.stroke();
					canvasA.restore();
					canvasA.closePath();
					canvasA.translate(0, 0);
				}
				
				// NEEDS UPDATEWORK
				var confidenceRange = jsonObj["mapEdges"][edge].confidence;
				if(confidenceRange < 0.5)
				{
					var vertexA = jsonObj["mapVertices"][jsonObj["mapEdges"][edge]["vertices"][0]];
					var vertexB = jsonObj["mapVertices"][jsonObj["mapEdges"][edge]["vertices"][1]];
					
					var coordinatesA = getBearing(ref_lon, ref_lat, vertexA["longitude"], vertexA["latitude"], zm, 300, 500);
					var coordinatesB = getBearing(ref_lon, ref_lat, vertexB["longitude"], vertexB["latitude"], zm, 300, 500);
				
					var mPosX = ((coordinatesA['x'] + coordinatesB['x']) / 2) - 8;
					var mPosY = ((coordinatesA['y'] + coordinatesB['y']) / 2) - 8;
					var cautionImg = document.getElementById("lowConfidence");
					tempLineCanvasContext.drawImage(cautionImg, mPosX, mPosY, 16, 16);
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
	canvasInputBox.style.left = "-200pt";
	canvasInputBox.style.top = "-200pt";
	tempLineCanvas.getContext("2d").clearRect(0, 0, tempLineCanvas.width, tempLineCanvas.height);
	refresh();
}

function getBearing(rlon, rlat, lon, lat, zoom, xOffset, yOffset)
{
	var coords = new Object();
		// coords['x'] = ((((lon - rlon)*zoom)+xOffset)*0.725)+0;
		// coords['y'] = ((((rlat - lat)*zoom)+yOffset)*0.9)+25;
		coords['x'] = ((((lon - rlon)*zoom)+xOffset)*1)+0;
		coords['y'] = ((((rlat - lat)*zoom)+yOffset)*1)+25;
		
	return coords;
}

function getBearingInverse(rlon, rlat, x, y, zoom, xOffset, yOffset)
{
	var coords = new Object();
		// coords['lon'] = ((((x - 0)/0.725)-xOffset)/zoom) + rlon;
		// coords['lat'] = -(((((y - 25)/0.9)-yOffset)/zoom)-rlat);
		coords['lon'] = ((((x - 0)/1)-xOffset)/zoom) + rlon;
		coords['lat'] = -(((((y - 25)/1)-yOffset)/zoom)-rlat);

	return coords;
}

function normalize(x, y, scale)
{
	var norm = Math.sqrt((x * x) + (y * y));
	return {"x": (x / norm) * scale, "y": (y / norm) * scale};
}

function vLength(x, y)
{
	var norm = Math.sqrt((x * x) + (y * y));
	return norm;
}

function vDotProduct(aX, aY, bX, bY)
{
	return (aX * bX) + (aY * bY);
}

function updateEntry(edge, value)
{
	console.log("Update on: " + edge + " :: " + value);
	if(value > 0)
	{
		inputData[edge] = value;
	}
	else
	{
		delete inputData[edge];
	}
}

function clearAndSelectEdge(edge, x, y, lineLength, lineWidth, rotation, canvas)
{
	context = canvas.getContext("2d");
	context.clearRect(0, 0, canvas.width, canvas.height)
	
	selectEdge(edge, x, y, lineLength, lineWidth, rotation, context);
}

function selectEdge(edge, x, y, lineLength, lineWidth, rotation, context)
{
	// also update edge div
	context.beginPath();
	context.globalAlpha = 1;
	context.translate(x, y);
	context.rotate(rotation);
	context.rect(0, -lineWidth / 2, lineLength, lineWidth);
	context.fillStyle = "#00FFFFAA";
	context.lineWidth = 3;
	context.strokeStyle = "#FF0000";
	context.fill();
	context.stroke();
	context.translate(-x, -y);
	context.setTransform(1, 0, 0, 1, 0, 0);
	context.closePath();
}