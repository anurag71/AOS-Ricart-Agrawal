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
	ConcurrentHashMap<String, Integer> hostMap = new ConcurrentHashMap<String, Integer>();

	volatile int time = 0;
	int d = 1;
	Socket socket;
	ArrayList<String> hosts = new ArrayList<String>();
	volatile boolean isExecutingCritical = false;
	volatile boolean isRequested = false;
	volatile boolean checkreply[] = new boolean[2];

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
		int i = 0;
		for (String host : serverlist.keySet()) {

			hosts.add(host);
			replicateAck.get("test1")[i] = false;
			replicateAck.get("test2")[i] = false;
			replicateAck.get("test3")[i] = false;
			replies.get("test1")[i] = false;
			replies.get("test2")[i] = false;
			replies.get("test3")[i] = false;
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
				if (type.equals("REQUEST")) {
					server.replies.get(filename)[server.hostMap.get(source)] = false;
					server.time = Math.max(server.time + server.d, messagetime);
					if (server.insideCritical.get(filename)) {
						server.deffered.get(m.fileName).add(m.source);
					} else {
						server.time = Math.max(server.time + server.d, messagetime);
						if (server.hasRequested.get(filename)) {
							if (server.hasRequestedTimestamp.get(filename) < messagetime) {
								server.deffered.get(filename).add(m.source);
							} else {
								String hs = null;
								try {
									hs = InetAddress.getLocalHost().getHostAddress();
								} catch (UnknownHostException e1) {
									e1.printStackTrace();
								}
								server.time = server.time + server.d;
								Message reply = new Message("REPLY", hs, m.content, filename, server.time);
								objecOutputStream.writeObject(reply);
							}
						}
					}

				} else if (m.type.equals("REPLY")) {
					server.time = Math.max(server.time + server.d, messagetime);
					System.out.println("Received REPLY from " + source);
					server.replies.get(filename)[server.hostMap.get(source)] = true;
				} else if (m.type.equals("REPLICATE")) {
					server.time = Math.max(server.time + server.d, messagetime);
					System.out.println("Received REPLICATE from " + m.source);
					writeThread wt = new writeThread(server.filesystem, filename, m.content);
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
			if (!server.insideCritical.get(filename) && server.deffered.get(filename).isEmpty()) {
				if (server.replies.get(filename)[0] && server.replies.get(filename)[1]) {
					System.out.println("Executing write operation");
					server.insideCritical.put(filename, true);
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
					server.time = server.time + server.d;
					final Message request = new Message("REQUEST", hs, content, filename, server.time);
					Thread t1 = null;
					server.hasRequested.put(filename, true);
					server.hasRequestedTimestamp.put(filename, server.time);
					if (!server.replies.get(filename)[0]) {
						t1 = new Thread(new Runnable() {

							@Override

							public void run() {
								try {

									server.serverlist.get(server.hosts.get(0)).objectOutputStream.writeObject(request);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

						});
						t1.start();
						System.out.println("Sent request to " + server.hosts.get(0));
					}
					Thread t2 = null;
					if (!server.replies.get(filename)[1]) {
						t2 = new Thread(new Runnable() {

							@Override

							public void run() {
								try {

									server.serverlist.get(server.hosts.get(1)).objectOutputStream.writeObject(request);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

						});
						t2.start();
						System.out.println("Sent request to " + server.hosts.get(1));
					}

					System.out.println("Sent request to server");
					while (true) {
						if (server.replies.get(filename)[0] && server.replies.get(filename)[1]) {
							System.out.println("Sufficient replies received");
							System.out.println("Executing write operation");
							server.insideCritical.put(filename, true);
							writeThread wt = new writeThread(server.filesystem, filename, content);
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
						}
					}
				}
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
		server.time = server.time + server.d;
		Message replicate = new Message("REPLICATE", hs, content, filename, server.time);

		try {
			server.serverlist.get(server.hosts.get(hostindex)).objectOutputStream.writeObject(replicate);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (true) {
			if (server.replicateAck.get(filename)[hostindex]) {
				break;
			}
		}
		server.replicateAck.get(filename)[hostindex] = true;
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
		try {
			System.out.println("inside write thread");
			FileWriter myWriter = new FileWriter(filesystem + "/" + filename, true);
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
			Boolean bool = true;
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
				ackMsg = new Message("ENQUIREACK", content, "", server.time);
				objectOutputStream.writeObject(ackMsg);
			} else if (message.type.equals("WRITE")) {

				String content = message.content;
				String filename = message.fileName;
				System.out.println("WRITE request from Client");
				server.time = server.time + server.d;
				RicartAgrawal ricartAgrawal = new RicartAgrawal(server, filename, content);
				Thread thread = new Thread(ricartAgrawal);
				thread.start();
				try {
					thread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				server.time = server.time + server.d;
				ackMsg = new Message("WACK", "WRITE Successful", "", server.time);
				objectOutputStream.writeObject(ackMsg);
				outputStream.flush();
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