import java.io.Serializable;
import java.sql.Timestamp;

public class Message implements Serializable {
	String type;
	String content;
	String fileName;
	int timestamp;
	String source;

	public Message(String type, String content, String fileName, int timestamp) {
		super();
		this.type = type;
		this.content = content;
		this.fileName = fileName;
		this.timestamp = timestamp;

	}

	public Message(String type, String source, String content, String fileName, int timestamp) {
		this.type = type;
		this.content = content;
		this.fileName = fileName;
		this.timestamp = timestamp;
		this.source = source;

	}
}
