import java.io.Serializable;
import java.sql.Timestamp;

class Message implements Serializable {
	String type;
	String content;
	String fileName;
	Timestamp timestamp;

	public Message(String type, String content, String fileName, Timestamp timestamp) {
		this.type = type;
		this.content = content;
		this.fileName = fileName;
		this.timestamp = timestamp;
	}
}
