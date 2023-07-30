package ithaki.diktya1;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import ithakimodem.Modem;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

public class GUIController {
	protected enum DataTypes{PACKETS, GPS, INITIALIZATION};
	protected static volatile boolean stopSessionTest = false;
	private static final int MODEM_BITRATE = 8000;
	private final int MODEM_TIMEOUT = 3000;
	private boolean latencyDistribution = true;
	private ArrayList<Packet> packets;
	private Thread cameraThread, packetThread,gpsThread,sessionTestThread;
	private Tab selectedTab;
	private int min,max,range;
	
	@FXML
	private TabPane tabPane;
	@FXML
	private ImageView cameraView, gpsView ;	
	@FXML
	private Tab sessTab, cameraTab, gpsTab, packetsTab, testTab;
	@FXML
	private Label echoLabel, imageLabel,imageErrorLabel,gpsLabel,ackLabel, nackLabel;
	@FXML
	private TextArea testTextArea;
	@FXML
	private Button sessionTestBtn;
	@FXML
	private BarChart<String, Number> packetChart;
	@FXML
	private CheckBox errorPackets;
	@FXML
	private RadioButton latencyDist,packerrDist;
	@FXML
	private Spinner<Integer> packetdurationSpinner, imageCountSpinner,imageErrorCountSpinner; 
	
	@FXML
	public void initialize() {
		echoLabel.setText(App.CODES.get("ECHO"));
		imageLabel.setText(App.CODES.get("IMAGE_NO_ERROR"));
		imageErrorLabel.setText(App.CODES.get("IMAGE_ERROR"));
		gpsLabel.setText(App.CODES.get("GPS"));
		ackLabel.setText(App.CODES.get("ACK"));
		nackLabel.setText(App.CODES.get("NACK"));
		packetChart.setAnimated(false);
		packetdurationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,86400));
		imageCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10));
		imageErrorCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10));
		Runnable cameraRunnable = new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					//wait for all other threads to finish first
					if(gpsThread != null) {
						if(gpsThread.isAlive()) gpsThread.join();
					}
					if(packetThread != null) {
						if(packetThread.isAlive()) packetThread.join();
					}
					if(sessionTestThread != null) {
						if(sessionTestThread.isAlive()) sessionTestThread.join();
					}
					Modem modem = new Modem(MODEM_BITRATE);
					modem.setTimeout(MODEM_TIMEOUT);
					getText(modem,null,"ithaki", DataTypes.INITIALIZATION);
					while(cameraTab.isSelected()) {
						byte[] image = downloadImage(modem, true, null);
						System.out.println(image);
						Platform.runLater(()->{
							Image frame = new Image(new ByteArrayInputStream(image));
							cameraView.setImage(frame);
						});
					}
					modem.close();
				}
				catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		};
		Runnable packetRunnable = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					//wait for all other threads to finish first
					if(gpsThread != null) {
						if(gpsThread.isAlive()) gpsThread.join();
					}
					if(cameraThread != null) {
						if(cameraThread.isAlive()) cameraThread.join();
					}
					if(sessionTestThread != null) {
						if(sessionTestThread.isAlive()) sessionTestThread.join();
					}
					boolean oldcheckBoxStatus = errorPackets.isSelected();
					int latencyBinCount = 10;
					int[] latencyBins = new int[latencyBinCount];
					int[] errorBins = new int[2];
					Modem modem = new Modem(MODEM_BITRATE);
					modem.setTimeout(MODEM_TIMEOUT);
					getText(modem, null,"ithaki", DataTypes.INITIALIZATION);
					packets = new ArrayList<>();
					PacketTest packetTest = new PacketTest(modem);
					while(packetsTab.isSelected()) {
						if(errorPackets.isSelected() != oldcheckBoxStatus) {
							packets.clear();
							oldcheckBoxStatus = errorPackets.isSelected();
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									// TODO Auto-generated method stub
									packetChart.getData().clear();
								}
							});
							
						}
						if(errorPackets.isSelected()) {
							packetTest.ackTest(1);
						}
						else {
							packetTest.echoTest(1);
						}
						//Populate bar chart
						Platform.runLater(new Runnable() {
								
							@Override
							public void run() {
								// TODO Auto-generated method stub
									XYChart.Series series = new XYChart.Series<>();
									System.out.printf("Old packet total: %d ", packets.size());
									packets.addAll(packetTest.packets);
									System.out.printf("New packet total: %d added: %d%n", packets.size(),packetTest.packets.size());
									if(latencyDistribution) {
										series.setName("Latency of packets in ms.");
										for(int i = 0; i<latencyBinCount;++i) {
											latencyBins[i] = 0;
										}
										min = Integer.MAX_VALUE;
										max = 0;
										range = 0;
										for(Packet packet: packets) {
											if(packet.latency > max) max = packet.latency;
											if(packet.latency < min) min = packet.latency;
										}
										range = max - min;
										System.out.printf("MAX: %d MIN %d RANGE %d %n", max,min,range);
										if(range > 0) {
											for(int i = 0; i<latencyBinCount;++i) {
												for(Packet packet: packets) {
													if(packet.latency >= min+ range*i/(double) latencyBinCount &&packet.latency <=min+ range*(i+1)/(double)latencyBinCount) {
														latencyBins[i]++;
													}
												}
											}
										}
										else {
											for(Packet packet:packets) {
												latencyBins[0]++;
											}
										}
										for(int i = 0; i< latencyBinCount; ++i) {
											series.getData().add(new XYChart.Data(String.format("[%.1f-%.1f]",min + range*i/(double) latencyBinCount, min+ range*(i+1)/(double) latencyBinCount), latencyBins[i]));
										}
									}
									else {
										series.setName("Packet error distribution");
										errorBins[0] = 0;
										errorBins[1] = 0;
										for(Packet packet: packets) {
											if(packet.wrong) {
												errorBins[1]++;
											}
											else {
												errorBins[0]++;
											}
											series.getData().add(new XYChart.Data("Correct", errorBins[0]));
											series.getData().add(new XYChart.Data("Wrong", errorBins[1]));
										}
										
									}
									//packetChart.getData().addAll(series);
									if(packetChart.getData() != null) packetChart.getData().clear();
									packetChart.setData(FXCollections.observableArrayList(series));
									packetTest.packets.clear(); //empty packet test list to get ready for new test
								}
							});
						}
					modem.close();
				}
				catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		};
		Runnable GPSRunnable = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					//wait for all other threads to finish first
					if(cameraThread != null) {
						if(cameraThread.isAlive()) cameraThread.join();
					}
					if(packetThread != null) {
						if(packetThread.isAlive()) packetThread.join();
					}
					if(sessionTestThread != null) {
						if(sessionTestThread.isAlive()) sessionTestThread.join();
					}
					Modem modem = new Modem(MODEM_BITRATE);
					modem.setTimeout(MODEM_TIMEOUT);
					getText(modem, null,"ithaki", DataTypes.INITIALIZATION);
					String response = getText(modem,null,App.CODES.get("GPS")+"R=1100209",DataTypes.GPS);
			        System.out.println(response);
			        ArrayList<GPS> path = generatePathPoints(response);
			        StringBuilder mapRequest = new StringBuilder(App.CODES.get("GPS"));
			        for(GPS point: path) {
			        	mapRequest.append(point.generateTParameter());
			        }
			        byte[] image = downloadImage(modem, true, mapRequest.toString());
			        Platform.runLater(new Runnable() {
						
						@Override
						public void run() {
							Image frame  = new Image(new ByteArrayInputStream(image));
							// TODO Auto-generated method stub
							gpsView.setImage(frame);
						}
					});
			        modem.close();
				}
				catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				
			}
		};
		tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {

			@Override
			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
				// TODO Auto-generated method stub
				System.out.printf("Tab changed from %s to %s!%n", oldValue.getId(), newValue.getId());
				selectedTab = newValue;
				//run time consuming tasks on separate thread so UI will not freeze
				if(selectedTab.equals(cameraTab)) {
					cameraThread = new Thread(cameraRunnable);
					cameraThread.start();
				}
				else if(selectedTab.equals(gpsTab)) {
					if(gpsView.getImage() == null) {
						gpsThread = new Thread(GPSRunnable);
						gpsThread.start();
					}
				}
				else if(selectedTab.equals(packetsTab)) {
					packetThread = new Thread(packetRunnable);
					packetThread.start();
				}
				else if (selectedTab.equals(testTab)) {
				}
			}
		});
	}
	
	@FXML
	private void clearPacketData() {
		if(packetChart.getData() != null) packetChart.getData().clear();
		if(packets != null) packets.clear();
	}
	
	@FXML
	private void enableLatency() {
		latencyDistribution = true;
	}
	
	@FXML
	private void enablePackErrors() {
		latencyDistribution = false;
	}
	
	@FXML
	private void cancelSessionTest() {
		stopSessionTest = true;
	}
	
	
	@FXML
	private void runTest() {
		testTextArea.clear();
		stopSessionTest = false;
		sessionTestThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					//wait for all other threads to finish first
					if(gpsThread != null) {
						if(gpsThread.isAlive()) gpsThread.join();
					}
					if(packetThread != null) {
						if(packetThread.isAlive()) packetThread.join();
					}
					if(cameraThread != null) {
						if(cameraThread.isAlive()) cameraThread.join();
					}
					Modem modem = new Modem(MODEM_BITRATE);
					modem.setTimeout(MODEM_TIMEOUT);
					getText(modem, testTextArea,"ithaki", DataTypes.INITIALIZATION);
					final String response = getText(modem,testTextArea,App.CODES.get("GPS")+"R=1100209",DataTypes.GPS);
			        System.out.println(response);
			        Platform.runLater(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							testTextArea.appendText(response);
						}
					});
			        ArrayList<GPS> path = generatePathPoints(response);
			        StringBuilder mapRequest = new StringBuilder(App.CODES.get("GPS"));
			        for(GPS point: path) {
			        	mapRequest.append(point.generateTParameter());
			        }
			        File mapfile = new File("outputs/images/map.jpg");
			        downloadImage(modem,testTextArea,mapfile, true, mapRequest.toString());
			        File file;
			        try {
			        	//modem.getInputStream().skip(modem.getInputStream().available());
						for(int i = 0; i<imageCountSpinner.getValue();++i) { //frame test
							if(stopSessionTest) break;
				        	file = new File(String.format("outputs/images/image%d.jpg",i));
				        	downloadImage(modem,testTextArea,file, true,null);
				        }
						for(int i = 0; i<imageErrorCountSpinner.getValue();++i) {
							if(stopSessionTest) break;
							file = new File(String.format("outputs/images/imageError%d.jpg",i));
							downloadImage(modem,testTextArea,file, false, null);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			        PacketTest test = new PacketTest(modem,testTextArea);
			        test.ackTest(packetdurationSpinner.getValue());
			        test.toCSV("latenciesWithARQ.csv");
			        System.out.printf("BER is %.2f%n", test.getBER());
			        PacketTest test2 = new PacketTest(modem,testTextArea);
			        test2.echoTest(packetdurationSpinner.getValue());
			        test2.toCSV("latencies.csv");
			        modem.close();
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							Alert alert = new Alert(AlertType.INFORMATION, "Session test finished! Check outputs folder for images and CSVs!", ButtonType.OK);
							alert.setTitle("Session test");
							alert.showAndWait();
						}
					});
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		});
		sessionTestThread.start();
	}

	
	
	protected synchronized byte[] downloadImage(Modem modem,boolean isErrorFree, String mapRequest) {
    	int imageByte, previousImageByte = -1;
    	String command;
    	byte[] out;
    	if(mapRequest != null) {
    		command=mapRequest;
    		System.out.println("downloading GPS image!");
    	}
    	else {
    		if(isErrorFree) {
        		command = App.CODES.get("IMAGE_NO_ERROR");
        		System.out.println("downloading image without errors!");
        	}
        	else {
        		command = App.CODES.get("IMAGE_ERROR");
        		System.out.println("downloading image with errors!");
        	}
    	}
    	
    	modem.setSpeed(80000);
    	modem.write((command+"\r").getBytes());
    	ArrayList<Byte> byteArrayList = new ArrayList<>();
    	try {
    		//FileOutputStream fileOutputStream = new FileOutputStream(file);
    		while(true) {
    			imageByte = modem.read();
    			if (imageByte == -1) break;
    			byteArrayList.add((byte)imageByte);
    			if (imageByte == 0xD9 && previousImageByte == 0xFF) break; //JPEG image delimiter found
    			previousImageByte = imageByte;
    		}
    		modem.setSpeed(MODEM_BITRATE);
    	}
    	catch (Exception e) {
			// TODO: handle exception
    		e.printStackTrace();
		}
    	out = new byte[byteArrayList.size()];
    	for(int i = 0; i<byteArrayList.size();++i) {
    		out[i] = byteArrayList.get(i);
    	}
    	
    	System.out.println("downloading image finished!");
    	return out;
    }
	
    protected synchronized static String getText(Modem modem,TextArea writeToTextArea, String command,DataTypes datatype) {
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
    			if(writeToTextArea != null) {
    				Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							writeToTextArea.appendText("GPS end delimiter found!\n");
						}
					});
    			}
    			break;
    		}
    		if(datatype == DataTypes.PACKETS && stringBuilder.indexOf("PSTOP") != -1) {
    			System.out.println("PACKET delimiter found!");
    			if(writeToTextArea != null) {
    				Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							writeToTextArea.appendText("PACKET delimiter found!\n");
						}
					});
    			}
    			break;
    		}
    		if(datatype == DataTypes.INITIALIZATION && stringBuilder.indexOf("the connection tested.\r\n\n\n") != -1) {
    			System.out.println("INITIALIZATION delimiter found!");
    			if(writeToTextArea != null) {
    				Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							writeToTextArea.appendText("INITIALIZATION delimiter found!\n");
						}
					});
    			}
    			break;
    		}
    	}
    	return stringBuilder.toString();
    }
    
    
    private ArrayList<GPS> generatePathPoints(String input){
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
    
    private static void downloadImage(Modem modem, TextArea writeToTextArea,File file, boolean isErrorFree,String mapRequest) {
    	int imageByte, previousImageByte = -1;
    	String command;
    	if(mapRequest != null) {
    		command=mapRequest;
    		System.out.println("downloading GPS image!");
    		if(writeToTextArea != null) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						writeToTextArea.appendText("downloading GPS image!\n");
					}
				});
			}
    	}
    	else {
    		if(isErrorFree) {
        		command = App.CODES.get("IMAGE_NO_ERROR");
        		System.out.println("downloading image WITHOUT errors!");
        		if(writeToTextArea != null) {
    				Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							writeToTextArea.appendText("Downloading image WITHOUT errors!\n");
						}
					});
    			}
        	}
        	else {
        		command = App.CODES.get("IMAGE_ERROR");
        		System.out.println("downloading image WITH errors!");
        		if(writeToTextArea != null) {
    				Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							writeToTextArea.appendText("downloading image WITH errors!\n");
						}
					});
    			}
        	}
    	}
    	
    	modem.setSpeed(80000);
    	modem.write((command+"\r").getBytes());
    	ArrayList<Byte> bytelist = new ArrayList<>();
    	try {
    		FileOutputStream fileOutputStream = new FileOutputStream(file);
    		while(true) {
    			imageByte = modem.read();
    			if (imageByte == -1) break;
    			//fileOutputStream.write(imageByte);
    			bytelist.add((byte) imageByte);
    			if (imageByte == 0xD9 && previousImageByte == 0xFF) break; //JPEG image delimiter found
    			previousImageByte = imageByte;
    		}
    		byte[] buffer = new byte[bytelist.size()];
    		for(int i = 0; i<bytelist.size();++i) {
    			buffer[i] = bytelist.get(i);
    		}
    		fileOutputStream.write(buffer);
    		fileOutputStream.flush();
    		fileOutputStream.close();
    		modem.setSpeed(MODEM_BITRATE);
    	}
    	catch (Exception e) {
			// TODO: handle exception
    		e.printStackTrace();
		}
    	System.out.println("downloading image finished!");
    	if(writeToTextArea != null) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					writeToTextArea.appendText("downloading image finished!\n");
				}
			});
		}
    }
}
