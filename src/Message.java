import java.io.Serializable;

class Message implements Serializable {
	String type;
	String content;

	public Message(String type, String content) {
		this.type = type;
		this.content = content;
	}
}
