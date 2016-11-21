/* 
 * Display graph of pressure data from serial
 * 
 * @author wbock
 *
 */
import processing.serial.*;
import controlP5.*;

Serial myPort;	// The serial port

float[] sensorValues = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}; // Array for  4x4 = 16 sensor values, range 0...1024

// Values from arduino
StringList rawData = new StringList();

void setup () {
	// General setup
	size(800 , 800);
	surface.setResizable(true);
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
  for(int row = 0; row < 4; row++) {
    for(int column = 0; column < 4; column++) {
      // Set fill based on pressure value
      fill(sensorValues[row * 4 + column] / 1024 * 255);
      rect(200 * row + 1, 200 * column + 1, 198, 198);
      fill(0,150,150);
      text((int)sensorValues[row * 4 + column],200 * row + 60, 200 * column + 125);
    }
  }
}

void serialEvent (Serial myPort) {
	// get the ASCII string:
	String inString = myPort.readStringUntil('\n');
	inString = trim(inString);
	String[] data = inString.split(" ");

  try {
    // Catch stray bits from serial
    for(int i=0; i<16; i++) {
      sensorValues[i] = Float.parseFloat(data[i]);
    }
  } catch (Exception e) {
    // Just use 0
    for(int i=0; i<16; i++) {
      sensorValues[i] = 0;
    }
  }
}