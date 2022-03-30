import java.io.Serializable;
import java.sql.Timestamp;

public class Message implements Serializable, Comparable<Message> {
	String type;
	String content;
	String fileName;
	Timestamp timestamp;
	String source;

	public Message(String type, String content, String fileName, Timestamp timestamp) {
		super();
		this.type = type;
		this.content = content;
		this.fileName = fileName;
		this.timestamp = timestamp;

	}

	public Message(String type, String source, String content, String fileName, Timestamp timestamp) {
		this.type = type;
		this.content = content;
		this.fileName = fileName;
		this.timestamp = timestamp;
		this.source = source;

	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	@Override
	public int compareTo(Message arg0) {
		// TODO Auto-generated method stub
		return timestamp.compareTo(arg0.timestamp);
	}
}
