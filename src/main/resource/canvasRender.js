const mapCanvas = document.getElementById("mapCanvas");
const canvasInputBox = document.getElementById("canvasInputBox");
const canvasInput = document.getElementById("canvasInputBoxInput");
const tempLineCanvas = document.getElementById("tempLineCanvas");
const tempLineCanvasContext = tempLineCanvas.getContext("2d");
const canvasA = mapCanvas.getContext("2d");

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
				x: e.clientX - mapCanvas.offsetLeft,
				y: e.clientY - mapCanvas.offsetTop
			};
			offsetMousePosX = mousePos.x;
			offsetMousePosY = mousePos.y;
			
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
				
				canvasInput.value = "0";
			}
			
			var coordFinal = getBearingInverse(ref_lon, ref_lat, offsetMousePosX, offsetMousePosY, zm, 300, 500);
			
			var distance = 9999;
			finalX = 0;
			finalY = 0;
			
			tempLineCanvas.getContext("2d").clearRect(0, 0, tempLineCanvas.width, tempLineCanvas.height);
			edgeCollection = {};
			
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
				// bX, bY
				
				lineAX = bX - aX;
				lineAY = bY - aY;
				
				slopeA = lineAX / lineAY;
				slopeB = -(lineAY / lineAX);
				slopeA = (isFinite(slopeA))?slopeA:1.2;
				slopeB = (isFinite(slopeB))?slopeB:1.2;
				
				directionRad = Math.atan(lineAX / lineAY);
				crossPoint = (Math.PI/2) + directionRad;

				targetOffset = -((coordinatesA['x'] * slopeA) - coordinatesA['y']) // Needs more
				myOffset = -((aX * slopeA) - aY)

				ySect = 50 * Math.cos(crossPoint);
				xSect = 50 * Math.sin(crossPoint);

				xSect = xSect + coordinatesA['x'];
				ySect = ySect + coordinatesA['y'];
				
				lineANormalized = normalize(cX, cY, 1);
				lineBNormalized = normalize(dX, dY, 1);
				dotProduct = 1 - vDotProduct(lineANormalized['x'], lineANormalized['y'], lineBNormalized['x'], lineBNormalized['y']);
				vLenA = vLength(cX, cY);
				vLenB = vLength(dX, dY) * dotProduct;
				
				ratio = vLenB / vLenA;
				xSect = coordinatesB['x'] - (cX * ratio);
				ySect = coordinatesB['y'] - (cY * ratio);

				mDistance = Math.sqrt(Math.pow(xSect - mousePos['x'], 2) + Math.pow(ySect - mousePos['y'], 2));

				if(mDistance < 10)
				{
					edgeCollection[edge] = true;
				}
				
				if(distance > mDistance)
				{
					hxVal = hxVal + 30;
					hxC = ("00" + hxVal.toString(16)).substr(-2);
					
					xPos = ((coordinatesA['x']+coordinatesB['x'])/2)+10;
					yPos = ((coordinatesA['y']+coordinatesB['y'])/2);
					
					if(edge == 5 || edge == 0)
					{
						tempLineCanvasContext.beginPath();
						tempLineCanvasContext.moveTo(coordinatesA['x'], coordinatesA['y']);
						tempLineCanvasContext.lineTo(coordinatesB['x'], coordinatesB['y']);
						tempLineCanvasContext.strokeStyle = "#00" + hxC + hxC;
						tempLineCanvasContext.lineWidth = 5;
						tempLineCanvasContext.stroke();
						tempLineCanvasContext.closePath();
						
						tempLineCanvasContext.beginPath();
						tempLineCanvasContext.moveTo(mousePos['x'], mousePos['y']);
						tempLineCanvasContext.lineTo(xSect, ySect);
						tempLineCanvasContext.strokeStyle = "#" + hxC + "0000";
						tempLineCanvasContext.lineWidth = 2;
						
						console.log("CX: " + JSON.stringify(lineANormalized));
						console.log("DX: " + JSON.stringify(lineBNormalized));
						console.log("DP: " + dotProduct);
						
						tempLineCanvasContext.fillStyle = "#FF0000";
						tempLineCanvasContext.fillText(Math.round(dotProduct*100)/100, xSect, ySect);
						
						tempLineCanvasContext.stroke();
						tempLineCanvasContext.closePath();
					}
					
					tempLineCanvasContext.beginPath();
					tempLineCanvasContext.font = "bold 16px Tahoma"
					tempLineCanvasContext.fillStyle = "#000000";
					tempLineCanvasContext.fillText(edge, xPos+1, yPos+1);
					tempLineCanvasContext.stroke();
					tempLineCanvasContext.closePath();
					tempLineCanvasContext.beginPath();
					tempLineCanvasContext.fillStyle = "#8000FF";
					tempLineCanvasContext.fillText(edge, xPos, yPos);
					tempLineCanvasContext.stroke();
					tempLineCanvasContext.closePath();
					
					// Need to pad in mDistance??  Or, use else statements for out of bounds values
					if((aX >= 0 && 0 >= bX) || (bX >= 0 && 0 >= aX)) 
					{
						chosenEdge = edge;
						distance = mDistance;
						finalX = xSect;
						finalY = ySect;
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
							finalX = xSect;
							finalY = ySect;
						}
					}
				}

				finalX = xSect;
				finalY = ySect;

				var confidenceRange = jsonObj["mapEdges"][edge].confidence;
				if(confidenceRange < 0.5)
				{
					var mPosX = ((coordinatesA['x'] + coordinatesB['x']) / 2) - 8;
					var mPosY = ((coordinatesA['y'] + coordinatesB['y']) / 2) - 8;
					var cautionImg = document.getElementById("lowConfidence");
					tempLineCanvasContext.drawImage(cautionImg, mPosX, mPosY, 16, 16);
				}
				if(chosenEdge > 0) edgeCollection[chosenEdge] = true;
			}

			const selectedRoad = document.getElementById("selectedRoadType");
			
			strTable = "<table>";
			for(edge in edgeCollection)
			{
				strTable += "<tr><td>" + edge + "</td><td>" + jsonObj['mapEdges'][edge]['mode'] + "</td></tr>";
				console.log("ECOL: " + edgeCollection[edge]);
			}
			strTable += "</table>";
			selectedRoad.innerHTML = "[" + chosenEdge + "] " + strTable;		
			if(typeof inputData[chosenEdge] != 'undefined')
			{
				canvasInput.value = inputData[chosenEdge];
			}
			else
			{
				canvasInput.value = "0";
			}
			
			var length = jsonObj["mapEdges"][chosenEdge]["vertices"].length;

			var chosenEdgeVertexA = jsonObj["mapVertices"][jsonObj["mapEdges"][chosenEdge]["vertices"][0]];
			var chosenEdgeVertexB = jsonObj["mapVertices"][jsonObj["mapEdges"][chosenEdge]["vertices"][1]];
			
			var coordinatesA = getBearing(ref_lon, ref_lat, chosenEdgeVertexA["longitude"], chosenEdgeVertexA["latitude"], zm, 300, 500);
			var coordinatesB = getBearing(ref_lon, ref_lat, chosenEdgeVertexB["longitude"], chosenEdgeVertexB["latitude"], zm, 300, 500);
			
			var lineWidth = 12;
			var dx = coordinatesB['x'] - coordinatesA['x'];
			var dy = coordinatesB['y'] - coordinatesA['y'];
			var rotation = Math.atan2(dy, dx);
			var lineLength = Math.sqrt(dx * dx + dy * dy);
			
			var xPos = (coordinatesA['x']+coordinatesB['x'])/2;
			var yPos = (coordinatesA['y']+coordinatesB['y'])/2;
			
			canvasInputBox.style.left = 150;
			canvasInputBox.style.top = 50;
							
			tempLineCanvasContext.beginPath();
			tempLineCanvasContext.globalAlpha = 1;
			tempLineCanvasContext.translate(coordinatesA['x'], coordinatesA['y']);
			tempLineCanvasContext.rotate(rotation);
			tempLineCanvasContext.rect(0, -lineWidth / 2, lineLength, lineWidth);
			tempLineCanvasContext.fillStyle = "#FF00FF";
			tempLineCanvasContext.strokeStyle = "#0000FF";
			tempLineCanvasContext.fill();
			tempLineCanvasContext.stroke();
			tempLineCanvasContext.translate(0, 0);
			tempLineCanvasContext.setTransform(1, 0, 0, 1, 0, 0);
			tempLineCanvasContext.closePath();
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
	canvasInput.value = "0";
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
	return Math.acos((aX * bX) + (aY * bY));
}