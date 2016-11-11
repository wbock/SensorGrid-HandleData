import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import controlP5.*; 
import java.io.BufferedWriter; 
import java.io.FileWriter; 
import java.util.Random; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class handleData extends PApplet {

/* Handle serial data, graph sets after recording
 * @author wbock
 *
 */







ControlP5 cp5; // UI layer
PShape backgroundSVG; // SVG for background
PImage img; // Instructional image
Serial myPort;	// The serial port
PFont fontL, fontM, fontS, fontXS; // Special cool-font

float graphPos[]; // position of the graph; x0, y0, x1, y1
float xMax;	// max x data value

// Session values
String file = "";
int iteration, setNr;
int[] order = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};

// Constants
static final boolean[][] tests = {{false, false, false, true},
								  {false, false, true, false},
								  {false, false, true, true},
								  {false, true, false, false},
								  {false, true, false, true},
								  {false, true, true, false},
								  {false, true, true, true},
								  {true, false, false, false},
								  {true, false, false, true},
								  {true, false, true, false},
								  {true, false, true, true},
								  {true, true, false, false},
								  {true, true, false, true},
								  {true, true, true, false},
								  {true, true, true, true}};

// Values from arduino
StringList rawData = new StringList();

// Interpreted values
FloatList sensor0 = new FloatList();
FloatList sensor1 = new FloatList();
FloatList millis = new FloatList();

boolean showData = false;

// UI-related items
ControlFont cFont;
Button saveData;
Textfield fileName;

public void setup () {
	// General setup
	
	frame.setResizable(true);
	cp5 = new ControlP5(this);
	backgroundSVG = loadShape("background_alt.svg");
	img = loadImage("tube.png");
	setNr = 0;

	// Set special text font
	fontL = createFont("Gungsuh", 44, true);
	fontM = createFont("Gungsuh", 32, true);
	fontS = createFont("Gungsuh", 20, true);
	fontXS = createFont("Gungsuh", 14, true);

	// Arduino setup
	println(Serial.list());
	myPort = new Serial(this, Serial.list()[0], 9600);
	myPort.bufferUntil('\n');


	// Styles
	stroke(0, 170, 212);

	// UI setup
	cFont = new ControlFont(fontL, 15);
	saveData = cp5.addButton("logData")
				.setValue(0)
				.setPosition(1570,700)
				.setSize(200,70);

	cp5.getController("logData")
		.getCaptionLabel()
		.setFont(cFont);

	fileName = cp5.addTextfield("fileName")
				.setPosition(1100, 640)
				.setSize(550, 30)
				.setAutoClear(false)
				.setFocus(true);

	cp5.addButton("reset", 0, 1670, 640, 100, 30);

	cp5.getController("reset")
		.getCaptionLabel()
		.setFont(cFont);
}

public void draw () {
	
	shape(backgroundSVG, 0, 0, width, height);

	// Define file name before starting
	if(!file.equals("")) {
		fill(0, 170, 212);
		textFont(fontL);
		text("Press Points:", 110, 130);
		image(img, 50, 250);

		// Show numbers for instructions
		textFont(fontM);
		if(tests[order[setNr]][0])
			text("1", 230, 445);
		if(tests[order[setNr]][1])
			text("2", 400, 410);
		if(tests[order[setNr]][2])
			text("3", 570, 375);
		if(tests[order[setNr]][3])
			text("4", 740, 340);
		textFont(fontS);
		text("Set: " + setNr + "/14", 1100, 745);
		text("Iteration: " + iteration, 1300, 745);
	}

	noFill();
	graphPos = new float[] {0.6f * width, 0.06f * height, 0.94f * width, 0.465f * height};
	if(showData) {
		beginShape();
		// Get first data coords
		float xPos = map(millis.get(0), 0, xMax, graphPos[0], graphPos[2]);
		float yPos0 = map(sensor0.get(0), 300, 500, graphPos[1], graphPos[3]);
		float yPos1 = map(sensor1.get(0), 300, 500, graphPos[1], graphPos[3]);
		// Creat curveVertex control point - start
		curveVertex(xPos, graphPos[3] - yPos1);
		for(int i = 0; i < sensor0.size(); i++) {
			xPos = map(millis.get(i), 0, xMax, graphPos[0], graphPos[2]);
			yPos0 = map(sensor0.get(i), 300, 500, graphPos[1], graphPos[3]);
			yPos1 = map(sensor1.get(i), 300, 500, graphPos[1], graphPos[3]);
			line(xPos, graphPos[3], xPos, graphPos[3] - yPos0);
			curveVertex(xPos, graphPos[3] - yPos1);
		}
		// Creat curveVertex control point - end
		curveVertex(xPos, graphPos[3] - yPos1);
		endShape();

		// time-axis labels
		try {
			fill(0, 170, 212);
			textFont(fontXS);
			text(millis.min() / 1000, 1078, 515);
			text(millis.max() * 0.25f / 1000, 1235, 515);
			text(millis.max() * 0.50f / 1000, 1390, 515);
			text(millis.max() * 0.75f / 1000, 1545, 515);
			text(millis.max() / 1000, 1703, 515);
		}
		catch (RuntimeException e) {
			//If list is empty, do nothing
		}
	}
}

public void serialEvent (Serial myPort) {
	// get the ASCII string:
	String inString = myPort.readStringUntil('\n');
	inString = trim(inString);

	if(inString.equals("START")) {
		// Clear data, get ready for a new set
		rawData.clear();
		sensor0.clear();
		sensor1.clear();
		millis.clear();
		showData = false;
	} else	if(inString.equals("STOP")) {
		// Display collected data
		xMax = millis.max();
		showData = true;
	} else {
		// Collect data, put in intArray
		rawData.append(inString);
		String[] data = inString.split(",");
		millis.append(PApplet.parseFloat(data[0]));
		sensor0.append(PApplet.parseFloat(data[1]));
		sensor1.append(PApplet.parseFloat(data[2]));
	}
}

// UI methods
public void logData(int theValue) {
	// Check if file path has been set, and then append data to file
	if(!file.equals("")) {
		BufferedWriter output = null;
		String path = dataPath("/logs/"+file + ".csv");
		try {
			output = new BufferedWriter(new FileWriter(path, true));
			for(String rawValue: rawData) {
				output.write(file + "," + iteration + "," + order[setNr] + "," + rawValue + '\n');
			}
		}
		catch (IOException e) {
			println("It Broke");
			e.printStackTrace();
		}
		finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					println("Error while closing the writer");
				}
			}
		}
		// go to next set/iteration
		setNr++;
		if(setNr > 14) {
			setNr = 0;
			iteration++;
			shuffleOrder();
		}
	}
}

public void reset(int theValue) {
	fileName.submit();
}
public void fileName(String theText) {
	//Check if the file name was changed, and then restart the set
	if(!theText.equals(file)) {
		file = theText;
		iteration = 0;
		setNr = 0;
		shuffleOrder();
	}
}

// Shuffle the test order
public void shuffleOrder() {
    int index, temp;
    Random random = new Random();
    for (int i = order.length - 1; i > 0; i--)
    {
        index = random.nextInt(i + 1);
        temp = order[index];
        order[index] = order[i];
        order[i] = temp;
    }
}
  public void settings() { 	size(1827 , 1046); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "handleData" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
