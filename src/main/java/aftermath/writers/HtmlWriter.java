package main.java.aftermath.writers;

import main.java.aftermath.server.AftermathServer;
import main.java.encephalon.dto.MapVertex;

public class HtmlWriter extends main.java.encephalon.writers.HtmlWriter {
	public HtmlWriter(int indentationDepth, AftermathServer as)
	{
		super(indentationDepth, as);
	}
	
	public void importScript(String script)
	{
		loadedScript.append("<script type='application/javascript'>" + System.lineSeparator());
		loadedScript.append(script + System.lineSeparator());
		loadedScript.append("</script>" + System.lineSeparator());
	}
	
	public void canvasIcons()
	{
		stringBuilder.append("<img id=\"lowConfidence\" src=\"https://cdn2.iconfinder.com/data/icons/aspneticons_v1.0_Nov2006/help_16x16.gif\"/>");
	}
	
	public void drawCanvasLine(String name, float width, String color, int xA, int yA, int xB, int yB)
	{
		drawCanvasLine(name, width, color, 1.0f, xA, yA, xB, yB);
	}
	
	public void initializeCanvasJS(String name)
	{
		stringBuilder.append("var ctx = document.getElementById(\"" + name + "\").getContext(\"2d\");\r" + System.lineSeparator());
		drawArc(MapVertex.WIDTH/2, MapVertex.HEIGHT/2, 3, 2, "#00C0C0");
	}

	public void drawCanvasLine(String name, float width, String color, float alpha, int xA, int yA, int xB, int yB)
	{
		stringBuilder.append("ctx.beginPath();" + System.lineSeparator());
		stringBuilder.append("ctx.globalAlpha = " + alpha + ";" + System.lineSeparator());
		stringBuilder.append("ctx.lineWidth = " + width + ";" + System.lineSeparator());
		stringBuilder.append("ctx.moveTo(" + xA + "," + yA + ");" + System.lineSeparator());
		stringBuilder.append("ctx.lineTo(" + xB + "," + yB + ");" + System.lineSeparator());
		stringBuilder.append("ctx.strokeStyle=\"" + color + "\";" + System.lineSeparator());
		stringBuilder.append("ctx.stroke();" + System.lineSeparator());
	}

	public void drawArc(int x, int y, int r, int width, String color)
	{
		stringBuilder.append("ctx.beginPath();" + System.lineSeparator());
		stringBuilder.append("ctx.lineWidth = " + width + ";" + System.lineSeparator());
		stringBuilder.append("ctx.arc(" + x + "," + y + "," + r + ",0,2*Math.PI);" + System.lineSeparator());
		stringBuilder.append("ctx.strokeStyle=\"" + color + "\";" + System.lineSeparator());
		stringBuilder.append("ctx.stroke();" + System.lineSeparator());
	}
	
	public void drawCanvasLineAsRect(String name, float width, String color, String color2, int xA, int yA, int xB, int yB)
	{
		drawCanvasLineAsRect(name, width, color, color2, 1.0f, xA, yA, xB, yB);
	}
	
	public void drawCanvasLineAsRect(String name, float width, String color, String color2, float alpha, int xA, int yA, int xB, int yB)
	{
		// Math.atan2(dy, dx)
		int dx = xB - xA;
		int dy = yB - yA;

		double rotation = Math.atan2(dy, dx);
		double lineLength = Math.sqrt(dx * dx + dy * dy);
		double lineWidth = width;
		
		stringBuilder.append("ctx.save();" + System.lineSeparator());
		stringBuilder.append("ctx.beginPath();" + System.lineSeparator());
		stringBuilder.append("ctx.globalAlpha = " + alpha + ";" + System.lineSeparator());
		stringBuilder.append("ctx.translate(" + xA + "," + yA + ");" + System.lineSeparator());
		stringBuilder.append("ctx.rotate(" + rotation + ");" + System.lineSeparator());
		stringBuilder.append("ctx.rect(" + 0 + ", " + -lineWidth / 2 + ", " + lineLength + ", " + lineWidth + ");" + System.lineSeparator());
		stringBuilder.append("ctx.translate(" + -xA + "," + -yA + ");" + System.lineSeparator());
		stringBuilder.append("ctx.rotate(" + -rotation + ");" + System.lineSeparator());
		
		stringBuilder.append("ctx.fillStyle=\"" + color + "\";" + System.lineSeparator());
		stringBuilder.append("ctx.fill();" + System.lineSeparator());

		stringBuilder.append("ctx.lineWidth=\"1\";" + System.lineSeparator());
		stringBuilder.append("ctx.strokeStyle=\"" + color2 + "\";" + System.lineSeparator());
		stringBuilder.append("ctx.stroke();" + System.lineSeparator());

		stringBuilder.append("ctx.restore();" + System.lineSeparator());
	}
	
	public void drawImage(String name, int width, int height, String image, int positionX, int positionY)
	{
		stringBuilder.append("var cautionImg = document.getElementById(\"" + image + "\");");
		stringBuilder.append("ctx.drawImage(cautionImg, " + positionX + ", " + positionY + ", " + width + ", " + height + ");");
		
		System.out.println("ctx.drawImage(cautionImg, " + positionX + ", " + positionY + ", " + width + ", " + height + ");");
	}
	
	public void drawRect(String name, float width, String color, String color2, float alpha, int xA, int yA, int xB, int yB)
	{
		// Math.atan2(dy, dx)
		int dx = xB - xA;
		int dy = yB - yA;

		stringBuilder.append("ctx.save();" + System.lineSeparator());
		stringBuilder.append("ctx.beginPath();" + System.lineSeparator());
		stringBuilder.append("ctx.globalAlpha = " + alpha + ";" + System.lineSeparator());
		stringBuilder.append("ctx.rect(" + xA + ", " + yA + ", " + dx + ", " + dy + ");" + System.lineSeparator());
		
		stringBuilder.append("ctx.fillStyle=\"" + color + "\";" + System.lineSeparator());
		stringBuilder.append("ctx.fill();" + System.lineSeparator());

		stringBuilder.append("ctx.lineWidth=\"2\";" + System.lineSeparator());
		stringBuilder.append("ctx.strokeStyle=\"" + color2 + "\";" + System.lineSeparator());
		stringBuilder.append("ctx.stroke();" + System.lineSeparator());

		stringBuilder.append("ctx.restore();" + System.lineSeparator());
	}

	public void drawVertex(String name, int size, double x, double y, String str, String color)
	{
		drawVertex(name, size, 1, x, y, str, color);
	}

	public void drawVertex(String name, int size, float alpha, double x, double y, String str, String color)
	{
		stringBuilder.append("ctx.beginPath();" + System.lineSeparator());
		stringBuilder.append("ctx.globalAlpha = " + alpha + ";" + System.lineSeparator());
		stringBuilder.append("ctx.lineWidth = 1;" + System.lineSeparator());
		stringBuilder.append("ctx.font='" + size + "pt Tahoma';" + System.lineSeparator());
		stringBuilder.append("ctx.fillStyle=\"" + color + "\";" + System.lineSeparator());
		stringBuilder.append("ctx.fillText(\"" + str + "\", " + x + ", " + y + ");" + System.lineSeparator());
	}
	
	public void canvasInputDiv(String name, String borderColor, String submitFunction)
	{
		stringBuilder.append("<div id=\"" + name + "\" style=\"text-align:center; position:absolute; background-color:#FF6060; top:-500pt; left:-500pt; z-index:30\">" + System.lineSeparator()
				+ "<div id=\"selectedRoadType\" style=\"\">&lt;PH&gt;</div>" + System.lineSeparator()
				+ "<div>Clear, Drivable, Walkable, Destroyed</div>"
				+ "<div><input type=\"range\" min=\"0\" max=\"10\" value=\"0\" class=\"slider\" id=\"" + name + "Input\" name=\"entry\" size=\"3\" style=\"border-width:2pt; border-style:solid; border-color:" + borderColor + "\"/></div>" + System.lineSeparator()
				+ "<div><button onclick=\"" + submitFunction + "\"/>Send Data!</button></div>" + System.lineSeparator()
				+ "</div>" + System.lineSeparator());
	}
}