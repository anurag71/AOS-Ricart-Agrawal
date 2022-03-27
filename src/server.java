import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class server {

	public static void main(String[] args) throws Exception {
		// Initialize ServerSocket
		int port = 22222;
		ServerSocket ssock = null;
		try {
			ssock = new ServerSocket(port);
		} catch (Exception e) {
			System.out.println("Could not start server on " + port);
			e.printStackTrace();
			return;
		}

		// Accept connection to server socket
		Socket socket = null;

		while (true) {
			try {
				socket = ssock.accept();
				clientThread thread = new clientThread(socket);
				thread.start();

			}

			catch (Exception e) {
				e.printStackTrace();
				System.out.println("Connection Error");

			}
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
		try {
			inputStream = socket.getInputStream();
			objectInputStream = new ObjectInputStream(inputStream);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// create a DataInputStream so we can read data from it.

		String line;
		try {
			Boolean bool = true;
			while (true) {
				Message message = (Message) objectInputStream.readObject();
				if (message.type.equals("QUIT")) {
					System.out.println("Inside");
					break;
				}
				System.out.println("Write request from Client  :  " + message.content);
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