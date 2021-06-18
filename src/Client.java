import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class Client {
	private static final Logger logger = LoggerFactory.getLogger(Client.class);

	public static void main(String args[]) throws Exception {
		logger.info("Trying to connect to the server");
		Socket cs = new Socket();
		SocketAddress sa = new InetSocketAddress("", 1278);
		cs.connect(sa);
		logger.info(String.format("Connecting to the server %s", cs.getRemoteSocketAddress()));

		byte[] arr = "hello world".getBytes(StandardCharsets.UTF_8);
		OutputStream os = cs.getOutputStream();
		while (true) {
			os.write(arr.length);
			os.write(arr);
			Thread.sleep(10);
		}
	}
}
