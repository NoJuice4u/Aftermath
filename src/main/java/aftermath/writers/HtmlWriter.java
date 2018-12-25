package main.java.aftermath.writers;

import main.java.aftermath.server.AftermathServer;
import main.java.encephalon.dto.MapVertex;

public class HtmlWriter extends main.java.encephalon.writers.HtmlWriter
{
    private StringBuilder dataArray = new StringBuilder();
    private StringBuilder imagesArray = new StringBuilder();
    
    public HtmlWriter(int indentationDepth, AftermathServer as)
    {
        super(indentationDepth, as);
    }

    public void importScript(String script)
    {
        loadedPostScript.append("<script type='application/javascript'>" + System.lineSeparator());
        loadedPostScript.append(script + System.lineSeparator());
        loadedPostScript.append("</script>" + System.lineSeparator());
    }

    public void canvasIcons()
    {
        stringBuilder.append(
                "<img id=\"lowConfidence\" style=\"position:absolute; visibility: hidden\" src=\"/resource/question.png\"/>");
        stringBuilder.append(
                "<img id=\"depot\" style=\"position:absolute; visibility: hidden\" src=\"/resource/depot.png\"/>");
    }

    public void drawCanvasLine(String name, float width, String color, int xA, int yA, int xB, int yB)
    {
        drawCanvasLine(name, width, color, 1.0f, xA, yA, xB, yB);
    }

    public void initializeCanvasJS(String name)
    {
        stringBuilder.append(
                "var ctx = document.getElementById(\"" + name + "\").getContext(\"2d\");\r" + System.lineSeparator());
        drawArc(MapVertex.WIDTH / 2, MapVertex.HEIGHT / 2, 3, 2, "#00C0C0");
    }
    
    public void addCanvasLineRect(float alpha, int xA, int yA, double rotation, double lineWidth, double lineLength, String color, String color2)
    {
        dataArray.append(", [" + alpha + ", " + xA + ", " + yA + ", " + rotation + ", " + lineWidth + ", " + lineLength + ", \"" + color + "\", \"" + color2 + "\"]");
    }
    
    public void addRoadLineData()
    {
        if(dataArray.length() > 2)
        {
            stringBuilder.append(System.lineSeparator() + "var roadLines = [" + dataArray.substring(2) + "];" + System.lineSeparator());
            
            stringBuilder.append("for(i = 0; i < roadLines.length; i++) {" + System.lineSeparator());
            stringBuilder.append("ctx.save();" + System.lineSeparator());
            stringBuilder.append("ctx.beginPath();" + System.lineSeparator());
            stringBuilder.append("ctx.globalAlpha = roadLines[i][0];" + System.lineSeparator());
            stringBuilder.append("ctx.translate(roadLines[i][1],roadLines[i][2]);" + System.lineSeparator());
            stringBuilder.append("ctx.rotate(roadLines[i][3]);" + System.lineSeparator());
            stringBuilder.append("ctx.rect(0, -roadLines[i][4]/2, roadLines[i][5], roadLines[i][4]);" + System.lineSeparator());
            stringBuilder.append("ctx.translate(-roadLines[i][1],-roadLines[i][2]);" + System.lineSeparator());
            stringBuilder.append("ctx.rotate(-roadLines[i][3]);" + System.lineSeparator());
    
            stringBuilder.append("ctx.fillStyle=roadLines[i][6];" + System.lineSeparator());
            stringBuilder.append("ctx.fill();" + System.lineSeparator());
    
            stringBuilder.append("ctx.lineWidth=\"1\";" + System.lineSeparator());
            stringBuilder.append("ctx.strokeStyle=roadLines[i][7];" + System.lineSeparator());
            stringBuilder.append("ctx.stroke();" + System.lineSeparator());
            
            stringBuilder.append("ctx.restore();" + System.lineSeparator());
            
            stringBuilder.append("};" + System.lineSeparator());
        }
    }
    
    public void addRoadConfidenceData()
    {
        if(imagesArray.length() > 2)
        {
            stringBuilder.append("window.onload = function(e){"); 
            stringBuilder.append(System.lineSeparator() + "var images = [" + imagesArray.substring(2) + "];" + System.lineSeparator());
            
            stringBuilder.append("for(i = 0; i < images.length; i++) {" + System.lineSeparator());
            stringBuilder.append("ctx.drawImage(images[i][0], images[i][1], images[i][2], images[i][3], images[i][4]);" + System.lineSeparator());
            stringBuilder.append("};" + System.lineSeparator());
            
            stringBuilder.append("};" + System.lineSeparator());
        }
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

    public void drawCanvasLineAsRect(String name, float width, String color, String color2, int xA, int yA, int xB,
            int yB)
    {
        drawCanvasLineAsRect(name, width, color, color2, 1.0f, xA, yA, xB, yB);
    }

    public void drawCanvasLineAsRect(String name, float width, String color, String color2, float alpha, int xA, int yA,
            int xB, int yB)
    {
        int dx = xB - xA;
        int dy = yB - yA;

        double rotation = Math.atan2(dy, dx);
        double lineLength = Math.sqrt(dx * dx + dy * dy);
        double lineWidth = width;
        
        addCanvasLineRect(alpha, xA, yA, rotation, lineWidth, lineLength, color, color2);
    }

    public void drawImage(String name, int width, int height, String image, int positionX, int positionY)
    {
        /*
        stringBuilder.append("var cautionImg = document.getElementById(\"" + image + "\");");
        stringBuilder.append(
                "ctx.drawImage(cautionImg, " + positionX + ", " + positionY + ", " + width + ", " + height + ");");
        */
        imagesArray.append(", [ document.getElementById(\"" + image + "\"), " + positionX + ", " + positionY + ", " + width + ", " + height + "]");
    }

    public void drawRect(String name, float width, String color, String color2, float alpha, int xA, int yA, int xB,
            int yB)
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
        stringBuilder.append("<div id=\"" + name
                + "\" style=\"text-align:center; position:absolute; background-color:#FF6060; top:-500pt; left:-500pt; z-index:30\">"
                + System.lineSeparator() + "<div id=\"selectedRoadType\" style=\"\">&lt;PH&gt;</div>"
                + System.lineSeparator() + "<div><table class=\"inputSliderLabels\" align=\"center\"><tr>"
                + "<td><img width=\"320\" src=\"/resource/RoadPictogram.png\"/></td></tr></table></div>"
                + "<div><input type=\"range\" min=\"0\" max=\"10\" value=\"0\" class=\"slider\" id=\"" + name
                + "Input\" name=\"entry\" size=\"3\" style=\"border-width:2pt; border-style:solid; border-color:"
                + borderColor + "\"/></div>" + System.lineSeparator() + "<div><button onclick=\"" + submitFunction
                + "\"/>Send Data!</button></div>" + System.lineSeparator() + "</div>" + System.lineSeparator());
    }
    
    public void flush()
    {
        super.flush();
        dataArray.setLength(0);
        imagesArray.setLength(0);
    }
}