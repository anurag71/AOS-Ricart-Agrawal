import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

class serverStore {
	ObjectInputStream objectInputStream = null;
	ObjectOutputStream objectOutputStream = null;

	public serverStore(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream) {
		this.objectInputStream = objectInputStream;
		this.objectOutputStream = objectOutputStream;
	}

}

public class Server {

	ConcurrentHashMap<String, Boolean> insideCritical = new ConcurrentHashMap<String, Boolean>();
	ConcurrentHashMap<String, Boolean> hasRequested = new ConcurrentHashMap<String, Boolean>();
	ConcurrentHashMap<String, Integer> hasRequestedTimestamp = new ConcurrentHashMap<String, Integer>();
	ConcurrentHashMap<String, ArrayList<String>> deffered = new ConcurrentHashMap<String, ArrayList<String>>();
	ConcurrentHashMap<String, boolean[]> replies = new ConcurrentHashMap<String, boolean[]>();
	ConcurrentHashMap<String, boolean[]> replicateAck = new ConcurrentHashMap<String, boolean[]>();
	ConcurrentHashMap<String, Integer> time = new ConcurrentHashMap<String, Integer>();
	ConcurrentHashMap<String, Integer> hostMap = new ConcurrentHashMap<String, Integer>();

	int d = 1;
	Socket socket;
	ArrayList<String> hosts = new ArrayList<String>();
	volatile boolean isExecutingCritical = false;
	volatile boolean isRequested = false;
	volatile boolean checkreply[] = new boolean[2];

	public Server() {

		boolean[] temp = new boolean[2];
		temp[0] = false;
		temp[1] = false;
		insideCritical.put("file1.txt", false);
		insideCritical.put("file2.txt", false);
		insideCritical.put("file3.txt", false);

		hasRequested.put("file1.txt", false);
		hasRequested.put("file2.txt", false);
		hasRequested.put("file3.txt", false);

		hasRequestedTimestamp.put("file1.txt", 0);
		hasRequestedTimestamp.put("file2.txt", 0);
		hasRequestedTimestamp.put("file3.txt", 0);

		deffered.put("file1.txt", new ArrayList<String>());
		deffered.put("file2.txt", new ArrayList<String>());
		deffered.put("file3.txt", new ArrayList<String>());

		replies.put("file1.txt", temp);
		replies.put("file2.txt", temp);
		replies.put("file3.txt", temp);

		replicateAck.put("file1.txt", temp);
		replicateAck.put("file2.txt", temp);
		replicateAck.put("file3.txt", temp);

		time.put("file1.txt", 0);
		time.put("file2.txt", 0);
		time.put("file3.txt", 0);

		System.out.println(replicateAck.get("file1.txt")[0] + " " + replicateAck.get("file1.txt")[1]);
		System.out.println(replicateAck.get("file2.txt")[0] + " " + replicateAck.get("file2.txt")[1]);
		System.out.println(replicateAck.get("file3.txt")[0] + " " + replicateAck.get("file3.txt")[1]);

	}

	ConcurrentHashMap<String, serverStore> serverlist = new ConcurrentHashMap<>();
	String filesystem;

	public static void main(String[] args) throws Exception {
		// Initialize ServerSocket
		if (args.length < 1) {
			System.out.println("Incorrect number of command line srguments");
			System.out.println("Please run the server as\n java server <fs1/fs2>");
			return;
		}
		Server server = new Server();
		server.filesystem = args[0];
		String[] hosts = { "10.176.69.32", "10.176.69.33", "10.176.69.34" };
//		
		int serverPort = 22222;
		int clientPort = 22223;
		AcceptServer acceptServer = new AcceptServer(serverPort, server);
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
				server.serverlist.put(socket.getInetAddress().getHostAddress(), ss);
				server.Initialize();
				CheckMsgThread checkMsgThread = new CheckMsgThread(in, out, server);
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
				clientThread thread = new clientThread(socket, server);
				thread.start();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Connection Error");
			}
		}

		// Accept connection to server socket
//
	}

	public void Initialize() {
		hosts.clear();
		int i = 0;
		for (String host : serverlist.keySet()) {
			System.out.println("Host check " + host);
			hosts.add(host);
			hostMap.put(host, i);
			i++;
		}
	}
}

class CheckMsgThread extends Thread {

	Message m;
	ObjectInputStream objectInputStream;
	ObjectOutputStream objecOutputStream;
	Server server;

	public CheckMsgThread(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Server server) {
		// TODO Auto-generated constructor stub
		this.objectInputStream = objectInputStream;
		this.objecOutputStream = objectOutputStream;
		this.server = server;
	}

	@Override
	public void run() {

		// TODO Auto-generated method stub
		while (true) {
			try {
				m = (Message) objectInputStream.readObject();
				String filename = m.fileName;
				int messagetime = m.timestamp;
				String source = m.source;
				String type = m.type;
				server.time.put(filename, Math.max(server.time.get(filename) + server.d, messagetime));
				if (type.equals("REQUEST")) {
					System.out.println("REQUEST received from " + source);
					server.replies.get(filename)[server.hostMap.get(source)] = false;

					if (server.insideCritical.get(filename)) {
						server.deffered.get(m.fileName).add(m.source);
						System.out.println("Deferring REQUEST");
					} else {
						System.out.println("Not executing critical");
						System.out.println(filename + " " + server.hasRequested.get(filename));
						if (server.hasRequested.get(filename)) {

							if (server.hasRequestedTimestamp.get(filename) < messagetime) {
								server.deffered.get(filename).add(m.source);
								System.out.println("Deferring REQUEST");
							} else {
								String hs = null;
								try {
									hs = InetAddress.getLocalHost().getHostAddress();
								} catch (UnknownHostException e1) {
									e1.printStackTrace();
								}
								int temp = server.time.get(filename) + server.d;
								server.time.put(filename, temp);
								Message reply = new Message("REPLY", hs, m.content, filename,
										server.time.get(filename));
								objecOutputStream.writeObject(reply);
								System.out.println("REPLY sent to " + source);
							}
						} else {
							String hs = null;
							try {
								hs = InetAddress.getLocalHost().getHostAddress();
							} catch (UnknownHostException e1) {
								e1.printStackTrace();
							}
							int temp = server.time.get(filename) + server.d;
							server.time.put(filename, temp);
							Message reply = new Message("REPLY", hs, m.content, filename, server.time.get(filename));
							objecOutputStream.writeObject(reply);
							System.out.println("REPLY sent to " + source);
						}
					}

				} else if (m.type.equals("REPLY")) {
					System.out.println("Received REPLY from " + source);
					boolean[] a = server.replies.get(filename);
					a[server.hostMap.get(source)] = true;
					server.replies.put(filename, a);
				} else if (m.type.equals("REPLICATE")) {
					System.out.println("Received REPLICATE from " + m.source);
					writeThread w = new writeThread(server.filesystem, filename, m.content);
					Thread t = new Thread(w);
					t.start();
					try {
						t.join();
						System.out.println("File replicated");
						String hs = null;
						try {
							hs = InetAddress.getLocalHost().getHostAddress();
						} catch (UnknownHostException e1) {
							e1.printStackTrace();
						}
						int temp = server.time.get(filename) + server.d;
						server.time.put(filename, temp);
						Message reply = new Message("REPLICATEACK", hs, m.content, filename, server.time.get(filename));
						objecOutputStream.writeObject(reply);
						System.out.println("REPLICATEACK sent to " + source);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (m.type.equals("REPLICATEACK")) {
					server.replicateAck.get(filename)[server.hostMap.get(source)] = true;
					System.out.println("Received REPLICATEACK from " + source);
				}
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				break;
			}
		}
		System.out.println("Connection to server lost");
		System.exit(0);
	}
}

class RicartAgrawal extends Thread {

	Server server;
	String filename;
	String content;

	public RicartAgrawal(Server server, String filename, String content) {
		// TODO Auto-generated constructor stub
		this.server = server;
		this.filename = filename;
		this.content = content;
	}

	@Override
	public void run() {
		System.out.println("Started RA");
		// create a DataInputStream so we can read data from it.
		while (true) {
			if (!server.insideCritical.get(filename)) {
				if (server.replies.get(filename)[0] && server.replies.get(filename)[1]) {
					System.out.println("Executing write operation");
					Boolean value = server.insideCritical.get(filename);
					if (value != null)
						server.insideCritical.computeIfPresent(filename, (key, oldValue) -> true);
					writeThread t1 = new writeThread(server.filesystem, filename, content);
					Thread t = new Thread(t1);
					try {
						t.start();
						t.join();
						ReplicateThread replicateThread = new ReplicateThread(server, 0, filename, content);
						Thread repl1 = new Thread(replicateThread);
						replicateThread = new ReplicateThread(server, 1, filename, content);
						Thread repl2 = new Thread(replicateThread);
						repl1.start();
						repl2.start();
						repl1.join();
						repl2.join();
						server.insideCritical.put(filename, false);
						break;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Sending requests to other servers");
					String hs = null;
					try {
						hs = InetAddress.getLocalHost().getHostAddress();
					} catch (UnknownHostException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					int temp = server.time.get(filename) + server.d;
					server.time.put(filename, temp);
					Message request = new Message("REQUEST", hs, content, filename, temp);
					server.hasRequested.put(filename, true);
					for (String s : server.hasRequested.keySet()) {
						System.out.println(s + " " + server.hasRequested.get(s));
					}
					if (!server.replies.get(filename)[0]) {
						try {
							server.serverlist.get(server.hosts.get(0)).objectOutputStream.writeObject(request);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("Sent request to " + server.hosts.get(0));
					}
					if (!server.replies.get(filename)[1]) {
						try {
							server.serverlist.get(server.hosts.get(1)).objectOutputStream.writeObject(request);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("Sent request to " + server.hosts.get(1));
					}

					System.out.println("Sent request to server");
					while (true) {
						if (server.replies.get(filename)[0] && server.replies.get(filename)[1]) {
							System.out.println("Sufficient replies received");
							System.out.println("Executing write operation");
							server.insideCritical.put(filename, true);
							writeThread wt = new writeThread(server.filesystem, filename, content);
							Thread t = new Thread(wt);
							try {
								t.start();
								t.join();
								server.replicateAck.get(filename)[0] = false;
								server.replicateAck.get(filename)[1] = false;
								ReplicateThread replicateThread = new ReplicateThread(server, 0, filename, content);
								Thread repl1 = new Thread(replicateThread);
								int temp1 = server.time.get(filename) + server.d;
								server.time.put(filename, temp1);
								replicateThread = new ReplicateThread(server, 1, filename, content);
								Thread repl2 = new Thread(replicateThread);
								repl1.start();
								repl2.start();
								repl1.join();
								repl2.join();
								int temp2 = server.time.get(filename) + server.d;
								server.time.put(filename, temp2);
								server.insideCritical.put(filename, false);
								server.hasRequested.put(filename, false);
								for (String host : server.deffered.get(filename)) {
									Message reply = new Message("REPLY", hs, "", filename, temp2);
									try {
										server.serverlist.get(host).objectOutputStream.writeObject(reply);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									System.out.println("Deferred REPLY sent to " + host);
								}
								break;
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
				break;
			} else {
				System.out.println("Waiting to get access to critical section");
			}
		}
	}
}

class ReplicateThread extends Thread {
	int hostindex;
	String filename;
	String content;
	Server server;

	public ReplicateThread(Server server, int hostindex, String filename, String content) {
		// TODO Auto-generated constructor stub
		this.hostindex = hostindex;
		this.filename = filename;
		this.content = content;
		this.server = server;
	}

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
		Message replicate = new Message("REPLICATE", hs, content, filename, server.time.get(filename));
		System.out.println(server.replicateAck.get(filename)[hostindex]);
		try {
			server.serverlist.get(server.hosts.get(hostindex)).objectOutputStream.writeObject(replicate);
			System.out.println("REPLICATE sent to " + server.hosts.get(hostindex));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(server.replicateAck.get(filename)[hostindex]);

		while (true) {
			if (server.replicateAck.get(filename)[hostindex]) {
				break;
			}
		}
		server.replicateAck.get(filename)[hostindex] = false;
		System.out.println(server.replicateAck.get(filename)[hostindex]);
	}
}

class writeThread extends Thread {

	String filename;
	String filesystem;
	String content;
	boolean writeCheck = false;

	public writeThread(String filesystem, String filename, String content) {
		// TODO Auto-generated constructor stub
		this.filename = filename;
		this.content = content;
		this.filesystem = filesystem;
	}

	@Override
	public void run() {
		System.out.println("Writting contents to file");
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try (FileWriter fw = new FileWriter(filesystem + "/" + filename, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println(content);
			// more code
		} catch (IOException e) {
			// exception handling left as an exercise for the reader
		}
		System.out.println("Contents written to file");

	}

}

class clientThread extends Thread {

	Socket socket;
	Server server;

	clientThread(Socket socket, Server server) {
		System.out.println("Connected to Client");
		this.socket = socket;
		this.server = server;
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
			System.out.println("Waiting for client message");
			Message message = (Message) objectInputStream.readObject();
			if (message.type.equals("ENQUIRE")) {
				System.out.println("ENQUIRE request from Client");
				String[] pathnames;
				File f = new File(server.filesystem);
				pathnames = f.list();
				int i = 1;
				String content = "";
				// For each pathname in the pathnames array
				for (String pathname : pathnames) {
					// Print the names of files and directories
					content += i + ". " + pathname + "\n";
					i++;
				}
				ackMsg = new Message("ENQUIREACK", content, "", 0);
				objectOutputStream.writeObject(ackMsg);
			} else if (message.type.equals("WRITE")) {

				String content = message.content;
				String filename = message.fileName;
				System.out.println("WRITE request from Client");
				RicartAgrawal ricartAgrawal = new RicartAgrawal(server, filename, content);
				Thread thread = new Thread(ricartAgrawal);
				thread.start();
				try {
					thread.join();
					System.out.println("WRITE served");
					ackMsg = new Message("WACK", "WRITE Successful", "", 0);
					objectOutputStream.writeObject(ackMsg);
					outputStream.flush();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
			System.out.println("Request served");
			try {
				if (brinp != null) {
					brinp.close();
				}

				if (out != null) {
					out.close();
				}
				if (socket != null) {
					socket.close();
				}

			} catch (IOException ie) {
			}
		}
	}

}

class AcceptServer extends Thread {

	int port;
	Socket socket;
	Server server;
	RicartAgrawal ricartAgrawal;

	public AcceptServer(int port, Server server) {
		// TODO Auto-generated constructor stub
		this.port = port;
		this.server = server;
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
				server.serverlist.put(socket.getInetAddress().getHostAddress(), new serverStore(in, out));
				server.Initialize();
				System.out.println(server.serverlist);
				System.out.println("Connected to server " + socket.getInetAddress().getHostAddress());
				CheckMsgThread checkMsgThread = new CheckMsgThread(in, out, server);
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