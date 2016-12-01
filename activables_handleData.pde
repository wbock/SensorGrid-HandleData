/* 
 * Display graph of pressure data from serial
 * 
 * @author wbock
 *
 */
import processing.serial.*;

Serial myPort;	// The serial port

// Array[3][6] for  3 rows x 6 columns = 18 sensor values, range 0...1024
float[][] sensorValues = {{0,0,0,0,0,0}, {0,0,0,0,0,0}, {0,0,0,0,0,0}}; 

// Values from arduino
StringList rawData = new StringList();

void setup () {
	// General setup
	size(1200 , 600);
	//surface.setResizable(true);
  textSize(45);

	// Arduino setup
  try {
  	myPort = new Serial(this, Serial.list()[0], 9600);
  	myPort.bufferUntil('\n');
  } catch (ArrayIndexOutOfBoundsException e) {
    println("Port not found, is the arduino plugged in?");
  }
}

void draw () {
  // Loop through 4x4 array, draw squares
  for(int row = 0; row < 3; row++) {
    for(int column = 0; column < 6; column++) {
      // Set fill based on pressure value
      fill(sensorValues[row][column] / 1024 * 255);
      rect(200 * column + 1, 200 * row + 1, 198, 198);
      fill(0,150,150);
      text((int)sensorValues[row][column], 200 * row + 60, 200 * column + 125);
    }
  }
  System.lineSeparator();
}

void serialEvent (Serial myPort) {
	// get the ASCII string:
	String inString = myPort.readStringUntil('\n');
	inString = trim(inString);
	String[] data = inString.split(" ");

  try {
    // first value is rowNr
    int rowNr = Integer.parseInt(data[0]);
    
    // Further values are from pressure sensors
    for(int i=1; i < 7; i++) {
      sensorValues[rowNr][i - 1] = Float.parseFloat(data[i]);
    }
  } catch (Exception e) {
    e.printStackTrace();
  }
}