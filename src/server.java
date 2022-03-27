import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
		String line;
		while (true) {
			try {
				line = brinp.readLine();
				while (!line.equalsIgnoreCase("QUIT")) {

					out.println(line);
					out.flush();
					System.out.println("Response to Client  :  " + line);
					line = brinp.readLine();
				}
			} catch (IOException e) {
				line = this.getName(); // reused String line for getting thread name
				System.out.println("IO Error/ Client " + line + " terminated abruptly");
			} catch (NullPointerException e) {
				line = this.getName(); // reused String line for getting thread name
				System.out.println("Client " + line + " Closed");
			}

			finally {
				try {
					System.out.println("Connection Closing..");
					if (brinp != null) {
						brinp.close();
						System.out.println(" Socket Input Stream Closed");
					}

					if (out != null) {
						out.close();
						System.out.println("Socket Out Closed");
					}
					if (socket != null) {
						socket.close();
						System.out.println("Socket Closed");
					}

				} catch (IOException ie) {
					System.out.println("Socket Close Error");
				}
			}
		}
	}

}