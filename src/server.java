import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

class MessageComparator implements Comparator<Message> {
	public int compare(Message str1, Message str2) {
		Timestamp first_Str;
		Timestamp second_Str;
		first_Str = str1.timestamp;
		second_Str = str2.timestamp;
		return first_Str.compareTo(second_Str);
	}
}

class serverStore {
	ObjectInputStream objectInputStream = null;
	ObjectOutputStream objectOutputStream = null;

	public serverStore(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream) {
		System.out.println("entered");
		this.objectInputStream = objectInputStream;
		this.objectOutputStream = objectOutputStream;
		// TODO Auto-generated constructor stub
	}

}

public class server {

	ConcurrentHashMap<String, serverStore> serverlist = new ConcurrentHashMap<>();
	String filesystem;

	public static void main(String[] args) throws Exception {
		// Initialize ServerSocket
		if (args.length < 1) {
			System.out.println("Incorrect number of command line srguments");
			System.out.println("Please run the server as\n java server <fs1/fs2>");
			return;
		}
		server s = new server();
		s.filesystem = args[0];
		String[] hosts = { "10.176.69.32", "10.176.69.33", "10.176.69.34" };
//		
		int serverPort = 22222;
		int clientPort = 22223;
		RicartAgrawal ricartAgrawal = new RicartAgrawal(s);
		Thread t = new Thread(ricartAgrawal);
		t.start();
		AcceptServer acceptServer = new AcceptServer(serverPort, s, ricartAgrawal);
		Thread serveraccept = new Thread(acceptServer);
		serveraccept.start();
		serverStore ss = null;
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		System.out.println(InetAddress.getLocalHost().getHostAddress());
		try {
			for (String a : hosts) {
				if (a.equals(InetAddress.getLocalHost().getHostAddress())) {
					break;
				}
				Socket socket = new Socket(a, 22222);
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());
				ss = new serverStore(in, out);
				s.serverlist.put(socket.getInetAddress().getHostAddress(), ss);
				ricartAgrawal.Initialize();
				CheckMsgThread checkMsgThread = new CheckMsgThread(in, ricartAgrawal);
				Thread t1 = new Thread(checkMsgThread);
				t1.start();
				System.out.println("Connected to server " + socket.getInetAddress().getHostAddress());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		acceptServer.join();

		Socket socket;
		ServerSocket clientSocket = new ServerSocket(clientPort);
		while (true) {
			try {
				socket = clientSocket.accept();
				clientThread thread = new clientThread(socket, s.filesystem, ricartAgrawal);
				thread.start();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Connection Error");
			}
		}

		// Accept connection to server socket
//
	}
}

class CheckMsgThread extends Thread {

	Message m;
	ObjectInputStream objectInputStream;
	RicartAgrawal ricartAgrawal;

	public CheckMsgThread(ObjectInputStream objectInputStream, RicartAgrawal ricartAgrawal) {
		// TODO Auto-generated constructor stub
		this.objectInputStream = objectInputStream;
		this.ricartAgrawal = ricartAgrawal;
	}

	@Override
	public void run() {

		// TODO Auto-generated method stub
		while (true) {
			try {
				m = (Message) objectInputStream.readObject();
				if (m.type.equals("REQUEST")) {
//					if(ricartAgrawal.isExecutingCritical) {
//						ricartAgrawal.messageQueue.add(m);
//						System.out.println("Added message to message queue");
//					}
//					else {
//						if(ricartAgrawal.isRequested) {
//							if (m.timestamp.before(ricartAgrawal.timestamp)) {
//								m.timestamp=new TimeStamp();
//								ricartAgrawal.messageQueue.add(arg0)
//							} else {
//								ricartAgrawal.messageQueue.add(m);
//								System.out.println("Added message to message queue");
//							}
//						}
//					}
					ricartAgrawal.messageQueue.add(m);
					System.out.println("Added message to message queue");
				} else if (m.type.equals("REPLY")) {
					System.out.println("Received REPLY from " + m.source);
					ricartAgrawal.checkreply[ricartAgrawal.replies.get(m.source)] = true;

					System.out.println("0 " + ricartAgrawal.checkreply[0]);
					System.out.println("1 " + ricartAgrawal.checkreply[1]);
				} else if (m.type.equals("REPLICATE")) {
					System.out.println("Received REPLICATE from " + m.source);
					writeThread wt = new writeThread(m.fileName, m.content);
					Thread t = new Thread(wt);
					t.start();
					try {
						t.join();
						System.out.println("File replicated");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				break;
			}
		}
		System.out.println("Connection to server lost");
	}
}

class RicartAgrawal extends Thread {

	Comparator<Message> nameSorter = Comparator.comparing(Message::getTimestamp);
	volatile static Queue<Message> messageQueue;
	Socket socket;
	ArrayList<String> hosts = new ArrayList<String>();
	volatile boolean isExecutingCritical = false;
	volatile boolean isRequested = false;
	volatile Timestamp timestamp;
	String filesystem;
	ConcurrentHashMap<String, Integer> replies = new ConcurrentHashMap<String, Integer>();
	volatile boolean checkreply[] = new boolean[2];
	server s;

	public RicartAgrawal(server s) {
		// TODO Auto-generated constructor stub
		this.filesystem = s.filesystem;
		this.s = s;
		messageQueue = new PriorityBlockingQueue<Message>(200, nameSorter);
	}

	public void Initialize() {
		int i = 0;
		hosts.clear();
		for (String host : s.serverlist.keySet()) {
			hosts.add(host);
			checkreply[i] = false;
			replies.put(host, i);
			i++;
		}
	}

	@Override
	public void run() {
		System.out.println(hosts.toString());
		System.out.println("Started RA");
		// create a DataInputStream so we can read data from it.
		while (true) {
			Message m = messageQueue.poll();
			if (m != null) {
				String type = m.type;
				switch (type) {
				case "REQUEST":
					System.out.println("Caught REQUEST from " + m.source);
					checkreply[replies.get(m.source)] = false;
					Message reply = null;
					try {
						reply = new Message("REPLY", InetAddress.getLocalHost().getHostAddress(), m.content, m.fileName,
								new Timestamp(System.currentTimeMillis()));
					} catch (UnknownHostException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					try {
						s.serverlist.get(m.source).objectOutputStream.writeObject(reply);
						System.out.println("Sending reply");
						break;
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				case "WRITE":
					if (checkreply[0] && checkreply[1]) {
						System.out.println("Executing write operation");
						isExecutingCritical = true;
						writeThread t1 = new writeThread(m.fileName, m.content);
						Thread t = new Thread(t1);
						try {
							t.start();
							t.join();
							isExecutingCritical = false;
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						timestamp = new Timestamp(System.currentTimeMillis());
						System.out.println("Sending requests to other servers");
						String hs = null;
						try {
							hs = InetAddress.getLocalHost().getHostAddress();
						} catch (UnknownHostException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
						final Message request = new Message("REQUEST", hs, m.content, m.fileName,
								new Timestamp(System.currentTimeMillis()));

						;
						Thread t1 = null;
						if (!checkreply[0]) {
							t1 = new Thread(new Runnable() {

								@Override

								public void run() {
									try {

										s.serverlist.get(hosts.get(0)).objectOutputStream.writeObject(request);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}

							});
							t1.start();
							System.out.println("Sent request to " + hosts.get(0));
						}
						Thread t2 = null;
						if (!checkreply[1]) {
							t2 = new Thread(new Runnable() {

								@Override

								public void run() {
									try {

										s.serverlist.get(hosts.get(1)).objectOutputStream.writeObject(request);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}

							});
							t2.start();
							System.out.println("Sent request to " + hosts.get(1));
						}

						System.out.println("Sent request to server");
						while (true) {
							if (checkreply[0] && checkreply[1]) {
								System.out.println("Sufficient replies received");
								isExecutingCritical = true;
								writeThread wt = new writeThread(m.fileName, m.content);
								Thread t = new Thread(wt);
								try {
									t.start();
									t.join();
									Thread repl = new Thread(new Runnable() {

										@Override
										public void run() {
											// TODO Auto-generated method stub
											String hs = null;
											try {
												hs = InetAddress.getLocalHost().getHostAddress();
											} catch (UnknownHostException e1) {
												// TODO Auto-generated catch block
												e1.printStackTrace();
											}
											Message replicate = new Message("REPLICATE", hs, m.content, m.fileName,
													new Timestamp(System.currentTimeMillis()));

											try {
												s.serverlist.get(hosts.get(0)).objectOutputStream
														.writeObject(replicate);
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
											try {
												s.serverlist.get(hosts.get(1)).objectOutputStream
														.writeObject(replicate);
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
										}
									});
									repl.start();
									repl.join();
									isExecutingCritical = false;
									break;
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}

					}
				}
			}
		}
	}
}

class writeThread extends Thread {

	String filename;
	String content;
	boolean writeCheck = false;

	public writeThread(String filename, String content) {
		// TODO Auto-generated constructor stub
		this.filename = filename;
		this.content = content;
	}

	@Override
	public void run() {
		try {
			System.out.println("inside write thread");
			FileWriter myWriter = new FileWriter("fs1/" + filename, true);
			myWriter.write(content + "\n");
			myWriter.close();
			writeCheck = true;
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
		System.out.println("Contents written to file");
	}

}

class clientThread extends Thread {

	Socket socket;
	String filesystem;
	RicartAgrawal ricartAgrawal;

	clientThread(Socket socket, String filesystem, RicartAgrawal ricartAgrawal) {
		System.out.println("Connected to Client");
		this.socket = socket;
		this.filesystem = filesystem;
		this.ricartAgrawal = ricartAgrawal;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		InputStream inp = null;
		BufferedReader brinp = null;
		PrintWriter out = null;
		try {
			inp = socket.getInputStream();
			brinp = new BufferedReader(new InputStreamReader(inp));
			out = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			return;
		}

		InputStream inputStream;
		ObjectInputStream objectInputStream = null;
		OutputStream outputStream = null;
		ObjectOutputStream objectOutputStream = null;
		Message ackMsg;
		// create a DataInputStream so we can read data from it.
		try {
			inputStream = socket.getInputStream();
			objectInputStream = new ObjectInputStream(inputStream);
			outputStream = socket.getOutputStream();
			objectOutputStream = new ObjectOutputStream(outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String line;
		try {
			Boolean bool = true;
			while (true) {
				Message message = (Message) objectInputStream.readObject();
				String content = "";
				if (message.type.equals("QUIT")) {
					break;
				} else if (message.type.equals("ENQUIRE")) {
					System.out.println("ENQUIRE request from Client");
					String[] pathnames;
					File f = new File(filesystem);
					pathnames = f.list();
					int i = 1;
					// For each pathname in the pathnames array
					for (String pathname : pathnames) {
						// Print the names of files and directories
						content += i + ". " + pathname + "\n";
						i++;
					}
					ackMsg = new Message("ENQUIREACK", content, "", new Timestamp(System.currentTimeMillis()));
					objectOutputStream.writeObject(ackMsg);
				} else if (message.type.equals("WRITE")) {
					System.out.println("WRITE request from Client");
					ricartAgrawal.messageQueue.add(message);
					System.out.println("Added message to message queue");
					outputStream.flush();
				}

			}
		} catch (IOException e) {
			line = this.getName(); // reused String line for getting thread name
			System.out.println("IO Error/ Client " + line + " terminated abruptly");
		} catch (NullPointerException e) {
			line = this.getName(); // reused String line for getting thread name
			System.out.println("Client " + line + " Closed");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		finally {
			try {
				System.out.println("Client connection Closing..");
				if (brinp != null) {
					brinp.close();
				}

				if (out != null) {
					out.close();
				}
				if (socket != null) {
					socket.close();
					System.out.println("Client socket Closed");
				}

			} catch (IOException ie) {
				System.out.println("Socket Close Error");
			}
		}
	}

}

class AcceptServer extends Thread {

	int port;
	Socket socket;
	server s;
	RicartAgrawal ricartAgrawal;

	public AcceptServer(int port, server s, RicartAgrawal ricartAgrawal) {
		this.ricartAgrawal = ricartAgrawal;
		// TODO Auto-generated constructor stub
		this.port = port;
		this.s = s;
//		this.ricartAgrawal = ricartAgrawal;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		while (true) {
			try {
				socket = serverSocket.accept();
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());
				s.serverlist.put(socket.getInetAddress().getHostAddress(), new serverStore(in, out));
				ricartAgrawal.Initialize();
				System.out.println(s.serverlist);
				System.out.println("Connected to server " + socket.getInetAddress().getHostAddress());
				CheckMsgThread checkMsgThread = new CheckMsgThread(in, ricartAgrawal);
				Thread t1 = new Thread(checkMsgThread);
				t1.start();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Connection Error");
				break;
			}
		}
		try {
			serverSocket.close();
			System.out.println("Terminating, could'nt establish connection to server.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

//System.out.println("Started RA");
//// create a DataInputStream so we can read data from it.
//while (true) {
//	Message m = server.messageQueue.poll();
//	if (m != null) {
//		String type = m.type;
//		switch (type) {
//		case "REQUEST":
//			numReplies = 0;
//			Message reply = new Message("REPLY", m.content, m.fileName,
//					new Timestamp(System.currentTimeMillis()));
//			try {
//				objectOutputStream.writeObject(reply);
//				System.out.println("Sending reply");
//				break;
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		case "WRITE":
//			if (numReplies == 1) {
//				System.out.println("Executing write operation");
//				isExecutingCritical = true;
//				writeThread t1 = new writeThread(m.fileName, m.content);
//				Thread t = new Thread(t1);
//				try {
//					t.start();
//					t.join();
//					isExecutingCritical = false;
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			} else {
//				System.out.println("Sending requests to other servers");
//				Message request = new Message("REQUEST", m.content, m.fileName,
//						new Timestamp(System.currentTimeMillis()));
//				try {
//					objectOutputStream.writeObject(request);
//					System.out.println("Sent request to server");
//					while (true) {
////						System.out.print("");
////						Thread.sleep(6000);
////						System.out.println(numReplies);
//
//						if (numReplies > 0) {
//							System.out.println("mmm" + numReplies);
//							System.out.println("Sufficient replies received");
//							isExecutingCritical = true;
//							writeThread t1 = new writeThread(m.fileName, m.content);
//							Thread t = new Thread(t1);
//							try {
//								t.start();
//								t.join();
//								isExecutingCritical = false;
//								break;
//							} catch (InterruptedException e) {
//								e.printStackTrace();
//							}
//						}
//					}
//					objectOutputStream.flush();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//			}
//		}
//	}
//}
//}