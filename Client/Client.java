/* Auction Network Client.java
 * Project created by Jan Rubio
 * Start Date: Spring 2021 */

package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;


public class Client extends Application{
	
	// Socket Variables
	private static Socket socket;
	private static final String HOST = "localhost"; //10.165.200.55
	private static BufferedReader fromServer;
	private static PrintWriter toServer;
	private Scanner consoleInput;
	
	// GUI Fields
	private static Stage window;
	private static Button quitBtn = new Button("quit");
	private static Login_Scene login_scene;
	private static Auction_Scene auction_scene;
	private static Registration_Scene registration_scene;
	
	// Client Information Variables
	public static String clientUsername = "";
	private static int clientPort = 0;
	public static Command cmd = new Command(null, null, 0);
	
	// Auction Variables
	public static double currHighestBid;
	public static double currHighestBid1;
	public static String currHighestDesc;
	
	public static ArrayList<Double> bidHistory = new ArrayList<Double>();
	public static ArrayList<String> itemHistory = new ArrayList<String>();
	public static ArrayList<String> bidderHistory = new ArrayList<String>();
	public static ArrayList<String> descHistory = new ArrayList<String>();
	
	public static String highestBidder;
	public static ArrayList<String> history;
    
	
	public static void main(String[] args) {
		new Client().runClient(); // set up the network of client
		launch(args);
	}
	
	private void runClient() {
		consoleInput = new Scanner(System.in);
		try {
			setUpNetworking();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void sendToServer(Command cmd) {		
		Gson gsonMessage = (new GsonBuilder()).create();
		System.out.println("Client Sent: " + gsonMessage.toJson(cmd));
		toServer.println(gsonMessage.toJson(cmd));
		toServer.flush();
	}
	
	protected void loginToServer(String jsonLoginCommand) {
		toServer.println(jsonLoginCommand);
		toServer.flush();
	}
	
	private void setUpNetworking() throws Exception {
		// Connect socket to server socket
		try {
			
			// attempt to connect
			socket = new Socket(HOST, 4242);
			System.out.println("Connecting to..." + socket);
			
			// set client port from connection
			clientPort = socket.getLocalPort();
			
			// initialize input and output
			InputStreamReader readSock = new InputStreamReader(socket.getInputStream());
			fromServer = new BufferedReader(readSock);
			toServer = new PrintWriter(socket.getOutputStream());
			
			// create a reader task
			Thread readerThread = new Thread(new Runnable() {
				@Override
				public void run() {
					String input;
					try { // wait for output from server
						while((input = fromServer.readLine()) != null) {
							
							System.out.println("Client Received: " + input);
							
							// obtain message from json
							cmd = new Gson().fromJson(input, Command.class);
							
							if(cmd.command.equals("register")) { // register command
								if(cmd.input.equals("registration successful")) {
									Platform.runLater(new Runnable() {
											@Override
											public void run() {
												make_login_scene(window, quitBtn);
											}
									}); // Display primaryStage
								}
								else {
									Platform.runLater(new Runnable() {
										@Override
										public void run() {
											registration_scene.user_is_taken(cmd.input);
										}
									}); // Display invalid
								}
							}
							else if(cmd.command.equals("login")) { // login command
								if(cmd.input.equals("incorrect user/password")) {
									Platform.runLater(new Runnable() {
										@Override
										public void run() {
											login_scene.incorrect_login(cmd.input);
										}
									});
								}
								else {
									Platform.runLater(new Runnable() {
										@Override
										public void run() {
											clientUsername = cmd.get_username();
											make_auction_scene(window, cmd.get_username(), quitBtn);
										}
									});
								}
							}
							else if(cmd.command.equals("itemList")) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										String[] auction_items = cmd.auction_arr;
										auction_scene.items.getItems().addAll(auction_items);
										auction_scene.items.setValue(auction_scene.items.getItems().get(0));
									}
								});
							}
							else if(cmd.command.equals("message")) { // message command
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										auction_scene.chatDisplay.appendText(cmd.input + "\n");
										auction_scene.chatInput.clear();
									}
								});
							}
							else if(cmd.command.equals("logout")) { // logout command
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										clientUsername = "";
										make_login_scene(window, quitBtn);
									}
								});
							}
							else if(cmd.command.equals("itemInfo")) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										String[] product_info = cmd.auction_arr;
//										System.out.println(Arrays.toString(product_info));
										auction_scene.highestBid.setText("$" + product_info[1]);
										auction_scene.currItemDesc.setText(product_info[2]);
										auction_scene.currItemStartDate.setText(product_info[3]);
										auction_scene.currItemEndDate.setText(product_info[4]);
									}
								});
							}
						}
					} catch(IOException e) {
						System.exit(0); // if server crashes, just close.
					}
				}
			});
			
			// create a writer task
			Thread writerThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while(true) {
						String input = consoleInput.nextLine();
						Command cmd = new Command(input, "message", clientPort);
						sendToServer(cmd);
					}
				}
			});
			
			// run reading and writing tasks
			readerThread.start();
			writerThread.start();
		}
		catch(ConnectException e){
			System.out.println("\u001B[31m" + "Could not find host \"" + HOST + "\":");
			System.out.println(e + "\u001B[0m");
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// initialize stage
		window = primaryStage;
		window.setTitle("Auction Client - login");
		make_login_scene(window, quitBtn);
		window.show();
	}

	private void make_login_scene(Stage window, Button quitBtn) { // button set up
		
		// set title
		window.setTitle("Jan's Auction - Login");
		
		// make scene
		login_scene = new Login_Scene(quitBtn);
		
		// fill stage with scene
		window.setScene(new Scene(login_scene.make_scene(), 300, 200));
		
		// set-up login
		login_scene.loginBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(!login_scene.username.getText().trim().equals("") && !login_scene.password.getText().trim().equals("")) {
					cmd.setCommand("login");
					cmd.port = clientPort;
					cmd.tryToLogin(login_scene.username.getText(), login_scene.password.getText());
					sendToServer(cmd);
				}
				else
					System.out.println("Cannot login user, one or more fields missing.");
			}
		});
		
		// set-up login and pressed
		login_scene.loginBtn.setOnKeyPressed(e -> {
			if(e.getCode() == KeyCode.ENTER) {
				login_scene.loginBtn.fire(); // TEST THIS
			}
		});
		
		// set-up password enter
		login_scene.password.setOnKeyPressed(e -> {
			if(e.getCode() == KeyCode.ENTER && !login_scene.username.getText().equals("") && !login_scene.password.getText().equals("")) {
				login_scene.loginBtn.fire();
			}
		});
		
		// set-up register
		login_scene.registerBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				make_registration_scene(window, quitBtn);
			}
		});
		
		// set-up register pressed
		login_scene.registerBtn.setOnKeyPressed(e -> {
			if(e.getCode() == KeyCode.ENTER) {
				login_scene.registerBtn.fire();
			}
		});
		
		// set-up quit button
		login_scene.quitBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					sendToServer(new Command(null, "quit", clientPort));
					fromServer.close();
					toServer.close();
					System.exit(0);
				}
				catch(Exception e) {
					System.out.println("Exiting...");
					System.exit(0); // just exit
				}
				window.close();
			}
		});
		
		// set-up exit button
		window.setOnCloseRequest(e -> {
			quitBtn.fire();
		});
	}

	private void make_registration_scene(Stage window, Button quitBtn) {
		// set title
		window.setTitle("Jan's Auction - Registration");
		
		// make scene
		registration_scene = new Registration_Scene(quitBtn);
		
		// fill stage with scene
		window.setScene(new Scene(registration_scene.make_scene(), 400, 220));
		
		// set-up registration button
	 	registration_scene.registerMeBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(!registration_scene.username.getText().equals("") && !registration_scene.password.getText().equals("") && !registration_scene.confirmPassword.getText().equals("")) {
					if(registration_scene.password.getText().equals(registration_scene.confirmPassword.getText())) {
	 					cmd.command = "register";
	 					cmd.set_username(registration_scene.username.getText());
	 					cmd.set_password(registration_scene.password.getText());
	 					sendToServer(cmd);
					}
					else {
						registration_scene.password_missmatch();
					}
				}
			}
		});
	 	
	 	registration_scene.backToLogin.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				make_login_scene(window, quitBtn);
			}
		});
	}

	private void make_auction_scene(Stage window, String user, Button quitBtn) {
		
		// set title
		window.setTitle("Jan's Auction - Welcome, " + user + "!");
		
		// make scene
		auction_scene = new Auction_Scene(quitBtn);
		
		// fill stage with scene
		window.setScene(new Scene(auction_scene.make_scene(), 500, 500));
		
		// request item list from server
		cmd.command = "itemList";
		sendToServer(cmd);	// access the auction items
		
		// set-up exit button
		window.setOnCloseRequest(e -> {
			auction_scene.quitBtn.fire();
		});
		
		// set-up item selection
		auction_scene.items.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				cmd.input = auction_scene.items.getValue();
				cmd.command = "itemInfo";
				sendToServer(cmd);
			}
		});
		
		// set-up logout button
		auction_scene.logoutBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				cmd.command = "logout";
				sendToServer(cmd);
			}
		});
		
		// set-up chat button
		auction_scene.chatInput.setOnKeyPressed(e -> {
			if(e.getCode() == KeyCode.ENTER) {
				String message = auction_scene.chatInput.getText().trim();
				if(!message.equals("")) {
					cmd.set_username(clientUsername);
					cmd.command = "message";
					cmd.input = message;
					sendToServer(cmd); // register button
				}
			}
		});
		
		// set-up quit button
		auction_scene.quitBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				
				cmd.command = "logout";
				sendToServer(cmd);
				
				try {
					sendToServer(new Command(null, "quit", clientPort));
					fromServer.close();
					toServer.close();
					System.exit(0);
				} catch (IOException e) {
					System.exit(0); // just exit
				}
				window.close();
			}
		});
	}
	
}
