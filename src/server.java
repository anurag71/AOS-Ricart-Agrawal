import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class server {

	public static void main(String[] args) throws Exception {
		// Initialize ServerSocket
		int clientPort = 22222;
		ServerSocket clientSocket = null;
		try {
			clientSocket = new ServerSocket(clientPort);
		} catch (Exception e) {
			System.out.println("Could not start server on " + clientPort);
			e.printStackTrace();
			return;
		}
		Socket socket = null;
		while (true) {
			try {
				socket = clientSocket.accept();
				clientThread thread = new clientThread(socket);
				thread.start();

			}

			catch (Exception e) {
				e.printStackTrace();
				System.out.println("Connection Error");

			}
		}

		// Accept connection to server socket

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
		try (PrintWriter out = new PrintWriter("fs1/" + filename)) {
			out.println(content);
			writeCheck = true;
			System.out.println("Contents written to file");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Cannot open file for writing content");
			writeCheck = false;
			e.printStackTrace();
		}
	}

}

class clientThread extends Thread {

	Socket socket;

	clientThread(Socket socket) {
		System.out.println("Connected to Client");
		this.socket = socket;
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
					File f = new File("fs1");

					// Populates the array with names of files and directories
					pathnames = f.list();

					// For each pathname in the pathnames array
					for (String pathname : pathnames) {
						// Print the names of files and directories
						content += pathname + "\n";
					}

					ackMsg = new Message("ENQUIREACK", content);
					objectOutputStream.writeObject(ackMsg);
				} else if (message.type.equals("WRITE")) {
					System.out.println("WRITE request from Client");
					writeThread t1 = new writeThread("file1.txt", message.content);
					Thread thread = new Thread(t1);
					thread.start();
					thread.join();
					if (t1.writeCheck) {
						ackMsg = new Message("WRITEACK", "Write request fulfilled");
						objectOutputStream.writeObject(ackMsg);
						outputStream.flush();
					} else {
						ackMsg = new Message("WRITEACK", "Write request could not be fulfilled");
						objectOutputStream.writeObject(ackMsg);
					}
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
		} catch (InterruptedException e) {
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