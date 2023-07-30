package ithaki.diktya1;

class Packet{
	boolean wrong;
	int latency, repeats;
	
	public Packet(int latency, boolean isWrong, int repeats) {
		this.latency = latency;
		this.repeats = repeats;
		wrong = isWrong;
	}
}