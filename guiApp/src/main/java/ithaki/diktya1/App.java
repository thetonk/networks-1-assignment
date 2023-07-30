package ithaki.diktya1;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * JavaFX App
 */
public class App extends Application {
	
	protected static HashMap<String, String> CODES = new HashMap<>();
	
    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
    	getSessionCodes();
    	try {
			Files.createDirectories(Paths.get("outputs/images"));
			Files.createDirectories(Paths.get("outputs/csv"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        scene = new Scene(loadFXML("ithaki"), 640, 480);
        stage.setTitle("Ithaki diktya I GUI");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			
			@Override
			public void handle(WindowEvent event) {
				// TODO Auto-generated method stub
				//GUIController.killAllThreads = true;
				System.exit(0);
			}
		});
        stage.show();
    }

    /*static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }*/

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
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
    

}