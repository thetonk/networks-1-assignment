package ithaki.diktya1;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

class GPS{
	//TODO: implement this class for GPS stuff and NMEA.
	protected LocalTime time;
	protected Coordinates latitude, longtitude;
	protected float elevation;
	protected int satelliteCount; //Assumed that coordinates refer to N and E respectively.
	
	public GPS(String positionData) {
		String[] data = positionData.split(",");
		time = LocalTime.parse(data[1], DateTimeFormatter.ofPattern("HHmmss.SSS"));
		elevation = Float.parseFloat(data[9]);
		satelliteCount = Integer.parseInt(data[7]);
		latitude = new Coordinates(Double.parseDouble(data[2]));
		longtitude = new Coordinates(Double.parseDouble(data[4]));
	}
	
	@Override
	public String toString() {
		return String.format("t:%s %s N %s E, elev: %.1fm, sats:%d", time,latitude,longtitude,elevation,satelliteCount);
	}
	
	public String generateTParameter() {
		return String.format("T=%d%d%d%d%d%d", longtitude.degrees,longtitude.minutes,longtitude.seconds,latitude.degrees,latitude.minutes,latitude.seconds);
	}
	
	class Coordinates{
		public int degrees, minutes,seconds;
		public Coordinates(double coord) {
			degrees = (int) coord / 100;
			minutes = (int) coord % 100;
			seconds = (int) Math.round((coord % 100 - minutes)*60);
		}
		@Override
		public String toString() {
			return String.format("%d°%d'%d\"", degrees,minutes,seconds);
		}
	}
}
