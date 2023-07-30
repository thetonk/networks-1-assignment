package ithakidiktya1.diktya1consoleapp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

class PacketTest {
	protected ArrayList<Packet> packets = new ArrayList<>(); //Needed for CSV generation and statistical processing.
	
	protected void echoTest(int seconds) {
		long testStart = System.currentTimeMillis(),start,stop;
		int latency = 0;
		String data;
		while((System.currentTimeMillis()-testStart)/1000 < seconds) {
			start = System.currentTimeMillis();
			data = App.getText(App.CODES.get("ECHO"),App.DataTypes.PACKETS);
        	stop = System.currentTimeMillis();
        	System.out.println(data);
        	latency = (int) (stop-start);
        	System.out.printf("Latency: %d ms.%n", latency);
        	packets.add(new Packet(latency, false,0));
		}
	}
	protected void ackTest(int seconds) {
		long testStart = System.currentTimeMillis(),start,stop;
		int latency = 0,repeatCount;
		String data;
		boolean wrong;
		while((System.currentTimeMillis()-testStart)/1000 < seconds) {
			repeatCount = 0;
			wrong = false;
			start = System.currentTimeMillis();
        	data = App.getText(App.CODES.get("ACK"),App.DataTypes.PACKETS);
        	stop = System.currentTimeMillis();
        	System.out.println(data);
        	latency = (int) (stop-start);
        	while(!ARQChecksum(data)) {
        		repeatCount++;
        		System.out.printf("sequence %s is incorrect, sending NACK%n",data);
        		start = System.currentTimeMillis();
        		data = App.getText(App.CODES.get("NACK"),App.DataTypes.PACKETS);
        		stop = System.currentTimeMillis();
        		latency += (int) (stop-start);
        		wrong = true;
        	}
        	System.out.printf("Latency: %d ms.%n", latency);
        	packets.add(new Packet(latency, wrong,repeatCount));
		}
	}
	protected double getBER() {
		//assume on every NACK, all 16 characters are wrong. Packet has 58 bytes in total.
		int wrongPacketCount = 0,totalPackets=0;
		for(Packet packet:packets) {
			totalPackets += packet.repeats +1;
			if(packet.wrong) {
				wrongPacketCount += packet.repeats;
			}
		}
		return (wrongPacketCount/(double) totalPackets)*16/58;
	}
	
	private boolean ARQChecksum(String data) {
    	//TODO: implement ARQ algorithm.
    	try {
    		byte[] sequence = data.substring(31, 47).getBytes("ASCII");
            final short fcs = Short.parseShort(data.substring(49,52));
            short checksum = 0;
            for(byte character: sequence) {
            	checksum ^= character;
            }
            	if(checksum == fcs) {
            	return true;
            }
            else {
            	return false;
            }
    	}
    	catch (Exception e) {
			// TODO: handle exception
    		e.printStackTrace();
    		return false;
		}
    }
	protected void toCSV(String filename) {
		try {
			File file = new File(String.format("outputs/csv/%s",filename));
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write("packetNo,delay,isWrong,repeats\n");
			Packet packet;
			byte isPacketWrong = 0;
			for(int i = 0;i<packets.size();++i) {
				packet = packets.get(i);
				isPacketWrong = (byte) (packet.wrong ? 1: 0);
				writer.write(String.format("%d,%d,%d,%d%n",i+1,packet.latency,isPacketWrong,packet.repeats));
			}
			writer.flush();
			writer.close();
		}
		catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	class Packet{
		boolean wrong;
		int latency,repeats;
		
		public Packet(int latency, boolean isWrong,int repeats) {
			this.latency = latency;
			this.repeats = repeats;
			wrong = isWrong;
		}
	}
}
