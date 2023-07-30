package ithakidiktya1.diktya1consoleapp;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import ithakimodem.*;


public class App 
{
	protected static HashMap<String, String> CODES = new HashMap<>();
	protected enum DataTypes{PACKETS, GPS, INITIALIZATION};
	private static Modem modem;
	private static final int MODEM_BITRATE = 8000;
	
    public static void main( String[] args )
    {
    	getSessionCodes();
        modem = new Modem(MODEM_BITRATE);
        modem.setTimeout(3000);
        getText("ithaki", DataTypes.INITIALIZATION);
        String response = getText(CODES.get("GPS")+"R=1011109",DataTypes.GPS);
        System.out.println(response);
        ArrayList<GPS> path = generatePathPoints(response);
        StringBuilder mapRequest = new StringBuilder(CODES.get("GPS"));
        for(GPS point: path) {
        	mapRequest.append(point.generateTParameter());
        }
        File mapfile = new File("outputs/images/map.jpg");
        downloadImage(mapfile, true, mapRequest.toString());
        File file;
        try {
        	//modem.getInputStream().skip(modem.getInputStream().available());
			for(int i = 0; i<5;++i) { //frame test
	        	file = new File(String.format("outputs/images/image%d.jpg",i));
	        	downloadImage(file, true,null);
	        }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        file = new File("outputs/images/imageError.jpg");
        downloadImage(file, false, null);
        file = new File("outputs/images/imageError2.jpg");
        downloadImage(file, false, null);
        PacketTest test = new PacketTest();
        test.ackTest(4*60);
        test.toCSV("latenciesWithARQ.csv");
        System.out.printf("BER is %.2f%n", test.getBER());
        PacketTest test2 = new PacketTest();
        test2.echoTest(4*60);
        test2.toCSV("latencies.csv");
        modem.close();
    }
    private static void getSessionCodes() {
    	try {
    		
            HttpClient client = HttpClient.newBuilder()
            		.version(Version.HTTP_2)
            		.connectTimeout(Duration.ofSeconds(30))
            		.followRedirects(Redirect.NORMAL)
            		.build();
            HttpRequest request = HttpRequest.newBuilder()
            		.uri(URI.create("http://ithaki.eng.auth.gr/netlab/main.php"))
            		.setHeader("Content-Type", "application/x-www-form-urlencoded")
            		.setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36")
            		.POST(HttpRequest.BodyPublishers.ofString("fi=ΣΠΥΡΙΔΩΝ&fa=ΜΠΑΛΤΣΑΣ&am=10443&op=1"))
            		.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Document document = Jsoup.parse(response.body());
            Element sessionElement = document.select("input[name=session]").first();
            String sessionIDString = sessionElement.attr("value");
            System.out.println("session id "+sessionIDString);
            HttpRequest sessionCodesRequest = HttpRequest.newBuilder()
            		.uri(URI.create("http://ithaki.eng.auth.gr/netlab/action.php"))
            		.setHeader("Content-Type", "application/x-www-form-urlencoded")
            		.setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36")
            		.POST(HttpRequest.BodyPublishers.ofString("fi=ΣΠΥΡΙΔΩΝ&fa=ΜΠΑΛΤΣΑΣ&am=10443&x=1&session="+sessionIDString))
            		.build();
            HttpResponse<String> codesHTML = client.send(sessionCodesRequest, HttpResponse.BodyHandlers.ofString());
            //System.out.println(codesHTML.body());
            Document codesDocument = Jsoup.parse(codesHTML.body());
            //select only the specific table element, so regex will not have to search the whole html all over again.
            Element tableElement = codesDocument.select("td[align=left]").get(1);
            Matcher matcher = Pattern.compile("E\\d+|M\\d+|G\\d+|P\\d+|Q+\\d+|R\\d+").matcher(tableElement.text());
            String match;
            while(matcher.find()) {
            	match = matcher.group();
            	if(match.startsWith("E")) {
            		CODES.put("ECHO", match);
            	}
            	else if(match.startsWith("M")) {
            		CODES.put("IMAGE_NO_ERROR", match);
            	}
            	else if(match.startsWith("G")) {
            		CODES.put("IMAGE_ERROR", match);
            	}
            	else if(match.startsWith("P")) {
            		CODES.put("GPS", match);
            	}
            	else if(match.startsWith("Q")) {
            		CODES.put("ACK", match);
            	}
            	else {
            		CODES.put("NACK", match);
            	}
            }
            System.out.println(CODES.toString());
            
		} catch (InterruptedException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
    	catch (IOException e) {
			// TODO: handle exception
    		e.printStackTrace();
		}
    }
    private static void downloadImage(File file, boolean isErrorFree, String mapRequest) {
    	int imageByte, previousImageByte = -1;
    	String command;
    	if(mapRequest != null) {
    		command=mapRequest;
    	}
    	else {
    		if(isErrorFree) {
        		command = CODES.get("IMAGE_NO_ERROR");
        	}
        	else {
        		command = CODES.get("IMAGE_ERROR");
        	}
    	}
    	
    	modem.setSpeed(80000);
    	modem.write((command+"\r").getBytes());
    	try {
    		System.out.println("downloading image!");
    		FileOutputStream fileOutputStream = new FileOutputStream(file);
    		while(true) {
    			imageByte = modem.read();
    			if (imageByte == -1) break;
    			fileOutputStream.write(imageByte);
    			if (imageByte == 0xD9 && previousImageByte == 0xFF) break; //JPEG image delimiter found
    			previousImageByte = imageByte;
    		}
    		fileOutputStream.flush();
    		fileOutputStream.close();
    		modem.setSpeed(MODEM_BITRATE);
    	}
    	catch (Exception e) {
			// TODO: handle exception
    		e.printStackTrace();
		}
    	System.out.println("downloading image finished!");
    }
    protected static String getText(String command, DataTypes datatype) {
    	StringBuilder stringBuilder = new StringBuilder();
    	int k;
    	if(datatype == DataTypes.INITIALIZATION) {
    		modem.open(command);
    	}
    	else {
    		modem.write((command+"\r").getBytes());
    	}
    	while(true) {
    		k = modem.read();
    		if(k==-1) break;
    		stringBuilder.append((char)k);
    		if(datatype == DataTypes.GPS && stringBuilder.indexOf("STOP ITHAKI GPS TRACKING\r\n") != -1) {
    			System.out.println("GPS end delimiter found!");
    			break;
    		}
    		if(datatype == DataTypes.PACKETS && stringBuilder.indexOf("PSTOP") != -1) {
    			System.out.println("PACKET delimiter found!");
    			break;
    		}
    		if(datatype == DataTypes.INITIALIZATION && stringBuilder.indexOf("the connection tested.\r\n\n\n") != -1) {
    			System.out.println("INITIALIZATION delimiter found!");
    			break;
    		}
    	}
    	return stringBuilder.toString();
    }
    
    
    private static ArrayList<GPS> generatePathPoints(String input){
    	ArrayList<GPS> path = new ArrayList<>();
    	BufferedReader reader = new BufferedReader(new StringReader(input));
    	String line;
    	try {
    		while((line = reader.readLine()) != null) {
        		if(line.startsWith("$GPGGA")) {
        			GPS point = new GPS(line);
        			path.add(point);
        		}
        	}
    	}
    	catch (IOException e) {
			// TODO: handle exception
    		e.printStackTrace();
		}
    	return path;
    }
}
