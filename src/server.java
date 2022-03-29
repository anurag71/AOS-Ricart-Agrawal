import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class server {

	class The_Comparator implements Comparator<Message> {
		public int compare(Message str1, Message str2) {
			Timestamp first_Str;
			Timestamp second_Str;
			first_Str = str1.timestamp;
			second_Str = str2.timestamp;
			return first_Str.compareTo(second_Str);
		}
	}

	volatile static Queue<Message> messageQueue = new PriorityQueue<Message>();

	public static void main(String[] args) throws Exception {
		// Initialize ServerSocket
		if (args.length < 2) {
			System.out.println("Incorrect number of command line srguments");
			System.out.println("Please run the server as\n java server <fs1/fs2> <client listening port ex:22222> ");
			return;
		}
		int clientPort = Integer.parseInt(args[1]);
		int serverPort = 22223;
		String filesystem = args[0];
		ServerSocket clientSocket = null;
		ServerSocket serverSocket = null;
		try {
			clientSocket = new ServerSocket(clientPort);
			if (!filesystem.equals("fs2"))
				serverSocket = new ServerSocket(serverPort);
		} catch (Exception e) {
			System.out.println("Could not start server on " + clientPort);
			e.printStackTrace();
			return;
		}
		Socket socket = null;
		Socket ssocket = null;
		if (!filesystem.equals("fs2")) {
			ssocket = serverSocket.accept();
			System.out.println("Connected to a server");
		} else {
			ssocket = new Socket("localhost", 22223);
			System.out.println("Connected to a server");
		}
		InputStream inputStream;
		ObjectInputStream objectInputStream = null;
		OutputStream outputStream = null;
		ObjectOutputStream objectOutputStream = null;
		// create a DataInputStream so we can read data from it.
		try {
			outputStream = ssocket.getOutputStream();
			objectOutputStream = new ObjectOutputStream(outputStream);
			inputStream = ssocket.getInputStream();
			objectInputStream = new ObjectInputStream(inputStream);

		} catch (IOException e) {
			e.printStackTrace();
		}
		RicartAgrawal ricartAgrawal = new RicartAgrawal(filesystem, objectOutputStream);
		Thread t = new Thread(ricartAgrawal);
		t.start();
		CheckMsgThread checkMsgThread = new CheckMsgThread(objectInputStream, ricartAgrawal);
		Thread t1 = new Thread(checkMsgThread);
		t1.start();
		while (true) {
			try {
				socket = clientSocket.accept();
				clientThread thread = new clientThread(socket, filesystem, ricartAgrawal);
				thread.start();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Connection Error");
			}
		}

		// Accept connection to server socket

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
					server.messageQueue.add(m);
					System.out.println("Added message to message queue");
				} else if (m.type.equals("REPLY")) {
					ricartAgrawal.numReplies++;
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

	String filesystem;
	Socket socket;
	boolean isExecutingCritical = false;
	boolean isRequested = false;
	ObjectInputStream objectInputStream = null;
	ObjectOutputStream objectOutputStream = null;

	public RicartAgrawal(String filesystem, ObjectOutputStream objectOutputStream) {
		// TODO Auto-generated constructor stub
		this.filesystem = filesystem;
		this.objectOutputStream = objectOutputStream;
	}

	volatile int numReplies = 0;

	@Override
	public void run() {
		System.out.println("Started RA");
		// create a DataInputStream so we can read data from it.
		while (true) {
			Message m = server.messageQueue.poll();
			if (m != null) {
				String type = m.type;
				switch (type) {
				case "REQUEST":
					numReplies = 0;
					Message reply = new Message("REPLY", m.content, m.fileName,
							new Timestamp(System.currentTimeMillis()));
					try {
						objectOutputStream.writeObject(reply);
						System.out.println("Sending reply");
						break;
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				case "WRITE":
					if (numReplies == 1) {
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
						System.out.println("Sending requests to other servers");
						Message request = new Message("REQUEST", m.content, m.fileName,
								new Timestamp(System.currentTimeMillis()));
						try {
							objectOutputStream.writeObject(request);
							System.out.println("Sent request to server");
							while (true) {
//								System.out.print("");
//								Thread.sleep(6000);
//								System.out.println(numReplies);

								if (numReplies > 0) {
									System.out.println("mmm" + numReplies);
									System.out.println("Sufficient replies received");
									isExecutingCritical = true;
									writeThread t1 = new writeThread(m.fileName, m.content);
									Thread t = new Thread(t1);
									try {
										t.start();
										t.join();
										isExecutingCritical = false;
										break;
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							}
							objectOutputStream.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
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
		try {
			FileWriter myWriter = new FileWriter("fs2/" + filename, true);
			myWriter.write(content + "\n");
			myWriter.close();
		} catch (IOException e) {
			writeCheck = false;
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

					// For each pathname in the pathnames array
					for (String pathname : pathnames) {
						// Print the names of files and directories
						content += pathname + "\n";
					}

					ackMsg = new Message("ENQUIREACK", content, "", new Timestamp(System.currentTimeMillis()));
					objectOutputStream.writeObject(ackMsg);
				} else if (message.type.equals("WRITE")) {
					System.out.println("WRITE request from Client");
					server.messageQueue.add(message);
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