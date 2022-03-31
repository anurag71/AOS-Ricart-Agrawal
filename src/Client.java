import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
		String serverip;
		if (args.length < 1) {
			Random rand = new Random();
			serverip = givenList.get(rand.nextInt(givenList.size()));
		} else {
			serverip = args[0];
		}
		System.out.println(serverip);
		sc = new Scanner(System.in);
		OutputStream outputStream;
		ObjectOutputStream objectOutputStream;
		InputStream inputStream;
		Message message;
		Message ackMsg;
		ObjectInputStream objectInputStream = null;
		int choice = 0;
		while (choice != 3) {

			System.out.println("Menu:\n1. Display files\n2. Write to file\n3. Quit\nEnter Your choice: ");
			choice = sc.nextInt();
			switch (choice) {
			case 1:
				try {
					s1 = new Socket(serverip, port); // You can use static final constant PORT_NUM
				} catch (IOException e) {
					e.printStackTrace();
					System.err.print("Cannot connect to Server.");
				}
				outputStream = s1.getOutputStream();
				// create an object output stream from the output stream so we can send an
				// object through it
				objectOutputStream = new ObjectOutputStream(outputStream);
				try {
					inputStream = s1.getInputStream();
					objectInputStream = new ObjectInputStream(inputStream);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				message = new Message("ENQUIRE", "Read contents of directory", "", 0);
				objectOutputStream.writeObject(message);
				System.out.println("Sending " + message.type + " request to Server");
				ackMsg = (Message) objectInputStream.readObject();
				System.out.println("List of files: \n" + ackMsg.content);
				objectOutputStream.flush();
				s1.close();
				break;
			case 2:
				try {
					s1 = new Socket(serverip, port); // You can use static final constant PORT_NUM
				} catch (IOException e) {
					e.printStackTrace();
					System.err.print("Cannot connect to Server.");
				}
				outputStream = s1.getOutputStream();
				// create an object output stream from the output stream so we can send an
				// object through it
				objectOutputStream = new ObjectOutputStream(outputStream);
				try {
					inputStream = s1.getInputStream();
					objectInputStream = new ObjectInputStream(inputStream);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				InetAddress localhost = InetAddress.getLocalHost();
				String clientId = localhost.getHostAddress().trim();
				System.out.println(
						"List of files: \n1. file1.txt\n2. file2.txt\n3. file3.txt" + "\nEnter the name of file:");
				String filename = sc.next();
				message = new Message("WRITE", clientId + "," + sdf.format(new Timestamp(System.currentTimeMillis())),
						filename, 0);
				objectOutputStream.writeObject(message);
				System.out.println("Sending " + message.type + " request to Server");
				ackMsg = (Message) objectInputStream.readObject();
				System.out.println(ackMsg.content);
				objectOutputStream.flush();
				s1.close();
				break;
			case 3:
				System.out.println("Thank you. Goodbye!");
				sc.close();
				break;
			}
		}

	}
}