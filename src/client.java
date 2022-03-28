import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class client {

	public static void main(String[] args) throws Exception {
		Socket s1 = null;
		String line = null;
		BufferedReader br = null;
		BufferedReader is = null;
		PrintWriter os = null;
		Scanner sc = null;

		try {
			s1 = new Socket("localhost", 22222); // You can use static final constant PORT_NUM
			System.out.println("Connected to Server.");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.print("Cannot connect to Server.");
		}
		try {
			br = new BufferedReader(new InputStreamReader(System.in));
			sc = new Scanner(System.in);
			is = new BufferedReader(new InputStreamReader(s1.getInputStream()));
			os = new PrintWriter(s1.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
			System.err.print("IO Exception");
		}

		OutputStream outputStream = s1.getOutputStream();
		// create an object output stream from the output stream so we can send an
		// object through it
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		InputStream inputStream;
		Message message;
		Message ackMsg;
		ObjectInputStream objectInputStream = null;
		try {
			inputStream = s1.getInputStream();
			objectInputStream = new ObjectInputStream(inputStream);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int choice = 0;
		while (choice != 3) {
			System.out.println("Menu:\n1. Display files\n2. Write to file\n3. Quit\nEnter Your choice: ");
			choice = sc.nextInt();
			switch (choice) {
			case 1:
				System.out.println("Enter message");
				message = new Message("ENQUIRE", "Message to get list of files");
				objectOutputStream.writeObject(message);
				System.out.println("Sending " + message.type + " request to Server");
				ackMsg = (Message) objectInputStream.readObject();
				System.out.println(ackMsg.content);
				break;
			case 2:
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				System.out.println("hello");
				InetAddress localhost = InetAddress.getLocalHost();
				System.out.println("hello");
				String clientId = localhost.getHostAddress().trim();
				System.out.println("hello");
				message = new Message("WRITE", clientId + "," + sdf.format(new Timestamp(System.currentTimeMillis())));
				objectOutputStream.writeObject(message);
				System.out.println("Sending " + message.type + " request to Server");
				ackMsg = (Message) objectInputStream.readObject();
				System.out.println(ackMsg.content);
				break;
			case 3:
				message = new Message("QUIT", "quit");
				objectOutputStream.writeObject(message);
				is.close();
				os.close();
				br.close();
				s1.close();
				sc.close();
				System.out.println("Connection Closed");
				break;
			}
		}

	}
}