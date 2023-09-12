package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import Client.Command;

public class ClientHandler implements Runnable, Observer {

	private Server server;
	private Socket clientSocket;
	private BufferedReader fromClient;
	private PrintWriter toClient;
	private int port;
	private String user;
	
	Gson gsonMessage;
	Command cmd;
	
	protected ClientHandler(Server server, Socket clientSocket, int port) {
		this.server = server;
		this.clientSocket = clientSocket;
		try {
			fromClient = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			toClient = new PrintWriter(this.clientSocket.getOutputStream());
		} catch(IOException e) {
			e.printStackTrace();
		}
		this.port = port;
		this.user = "";
//		sendToPortClient(port);
	}
	
	protected void sendToPortClient(int port) {
		toClient.println(port);
		toClient.flush();
	}
	
	protected void sendToClient(String message) {
		Gson gsonMessage = new Gson();
		Command cmd = gsonMessage.fromJson(message, Command.class);
		System.out.println("Sending to client "+ port + ": " + cmd.input);
		toClient.println(message);
		toClient.flush();
	}
	
	@Override
	public void run() {
		String input;
		try {
			while((input = fromClient.readLine()) != null) {
				// obtain message from json
				gsonMessage = new Gson();
				cmd = gsonMessage.fromJson(input, Command.class);
				
				// if the command is quit/exit
				if(cmd.command.equals("quit")) {
					server.removeClient(this);
					this.clientSocket.close();
				}
				
				// if the command is message
				if(cmd.command.equals("message")) {
					Command cmdSend = new Command(cmd.input, cmd.command, port);
					Gson gsonMessageSend = (new GsonBuilder()).create();
					if(user.equals("")) {
						server.processMessage(gsonMessageSend.toJson(cmdSend), ""+cmd.port);
					}
					else {
						server.processMessage(gsonMessageSend.toJson(cmdSend), user);
					}
				}
				
				// if the command is register attempt
				if(cmd.command.equals("register")) {
					String regStatus = "this username is already taken";
					if(server.registerAttempt(cmd.username, cmd.password))
						regStatus = "registration successful";
					cmd = new Command(regStatus, "register", port);
					sendToClient(gsonMessage.toJson(cmd));
				}
				
				// if the command is login attempt
				if(cmd.command.equals("login")) {
					if(server.loginAttempt(cmd.username, cmd.password)) {
						user = cmd.username; // port now has username
						// attempt passed
						cmd = new Command("2", "login", port);
						gsonMessage = (new GsonBuilder()).create();
						sendToClient(gsonMessage.toJson(cmd));
					}
					else { // attempt failed
						cmd = new Command("incorrect user/password", "login", port);
						gsonMessage = (new GsonBuilder()).create();
						sendToClient(gsonMessage.toJson(cmd));
					}
				}
				
				// if the command is login guest
				if(cmd.command.equals("guest")) {
					int guest = server.loginGuest();
					user = "Guest_"+port+""+guest;
					cmd = new Command("Guest_"+port+""+guest, "guest", port);
					gsonMessage = (new GsonBuilder()).create();
					sendToClient(gsonMessage.toJson(cmd));
				}
				
				// if the command is guestList
				if(cmd.command.equals("itemList")) {
					String auction_items[] = server.get_auction_items();
					cmd = new Command("", "itemList", port);
					cmd.auction_arr = auction_items;
					gsonMessage = (new GsonBuilder()).create();
					sendToClient(gsonMessage.toJson(cmd));
				}
				
				// if the command is logout
				if(cmd.command.equals("logout")) {
					user = cmd.input; // user field is empty
					cmd = new Command("logout", "logout", port);
					gsonMessage = (new GsonBuilder()).create();
					sendToClient(gsonMessage.toJson(cmd));
				}
				
				// if the command is selection
				if(cmd.command.equals("itemInfo")) {
					String auction_info[] = server.get_auction_info_of(cmd.input);
					cmd.auction_arr = auction_info;
					cmd.input = "success";
					if(auction_info == null)
						cmd.input = "fail";
					gsonMessage = (new GsonBuilder()).create();
					sendToClient(gsonMessage.toJson(cmd));
				}
				
				// if the command is a bid
				if(cmd.command.equals("bid")) {
					int bidStatus = server.validBid(cmd.input, cmd.item);
					if(bidStatus == 1) { // invalid (1)
						cmd = new Command("1", "bid", port);
						gsonMessage = (new GsonBuilder()).create();
						sendToClient(gsonMessage.toJson(cmd));
					}
					else if(bidStatus == 2) { // valid (2)
						
						// notify the clients
						Command cmdSend = new Command(cmd.input, cmd.command, port);
						cmdSend.item = cmd.item;
						cmdSend.description = server.getDesc(cmd.item);
						Gson gsonMessageSend = (new GsonBuilder()).create();
						
						if(user.equals("")) {
							server.processMessage(gsonMessageSend.toJson(cmdSend), ""+cmd.port);
						}
						else {
							server.processMessage(gsonMessageSend.toJson(cmdSend), user);
						}
						
						// send the client back the results
						cmdSend = new Command("2", "bid", port);
						gsonMessageSend = (new GsonBuilder()).create();
						sendToClient(gsonMessageSend.toJson(cmdSend));
						
					}
					else { // time out (3)
						// send the client back the results
						Command cmdSend = new Command("3", "bid", port);
						Gson gsonMessageSend = (new GsonBuilder()).create();
						sendToClient(gsonMessageSend.toJson(cmdSend));
					}
				}
				
				// if the command is refresh
//				if(cmd.command.equals("refresh")) {
////					Command cmdSend = server.refreshItem(cmd.input, port);
//					cmdSend.item = cmd.input;
//					Gson gsonMessageSend = (new GsonBuilder().create());
//					sendToClient(gsonMessageSend.toJson(cmdSend));
//				}
				
			}
			clientSocket.close();
		} catch(IOException e) {
			
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		Gson gsonSend = new Gson();
		Command cmd = gsonSend.fromJson((String)arg, Command.class);		
		// if command is message
		if(cmd.command.equals("message")) {
			sendToClient(gsonSend.toJson(cmd));
		}
		// if command is update
		if(cmd.command.equals("bid")) {
			cmd.command = "update";
			sendToClient(gsonSend.toJson(cmd));
		}
	}
}