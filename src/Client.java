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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Client {

	public static void main(String[] args) throws Exception {
		Socket s1 = null;
		String line = null;
		BufferedReader br = null;
		BufferedReader is = null;
		PrintWriter os = null;
		Scanner sc = null;
		int port = 22223;
		ArrayList<String> givenList = new ArrayList<>();
		givenList.addAll(Arrays.asList("10.176.69.32", "10.176.69.33", "10.176.69.34"));
		Random rand = new Random();
		String serverip = givenList.get(rand.nextInt(givenList.size()));
		System.out.println(serverip);

		int choice = 0;
		while (choice != 3) {
			try {
				s1 = new Socket(serverip, port); // You can use static final constant PORT_NUM
			} catch (IOException e) {
				e.printStackTrace();
				System.err.print("Cannot connect to Server.");
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
			System.out.println("Menu:\n1. Display files\n2. Write to file\n3. Quit\nEnter Your choice: ");
			choice = sc.nextInt();
			switch (choice) {
			case 1:
				System.out.println("Enter message");
				message = new Message("ENQUIRE", "Read contents of directory", "", 0);
				objectOutputStream.writeObject(message);
				System.out.println("Sending " + message.type + " request to Server");
				ackMsg = (Message) objectInputStream.readObject();
				System.out.println("List of files: " + ackMsg.content);
				is.close();
				os.close();
				br.close();
				s1.close();
				sc.close();
				break;
			case 2:
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				InetAddress localhost = InetAddress.getLocalHost();
				String clientId = localhost.getHostAddress().trim();
				message = new Message("ENQUIRE", "Read contents of directory", "", 0);
				objectOutputStream.writeObject(message);
				ackMsg = (Message) objectInputStream.readObject();
				System.out.println("List of files: " + ackMsg.content + "\nEnter the name of file:");
				String filename = sc.nextLine();
				message = new Message("WRITE", clientId + "," + sdf.format(new Timestamp(System.currentTimeMillis())),
						filename, 0);
				objectOutputStream.writeObject(message);
				System.out.println("Sending " + message.type + " request to Server");
				ackMsg = (Message) objectInputStream.readObject();
				System.out.println(ackMsg.content);
				is.close();
				os.close();
				br.close();
				s1.close();
				sc.close();
				break;
			case 3:
				System.out.println("Connection Closed");
				break;
			}
		}

	}
}