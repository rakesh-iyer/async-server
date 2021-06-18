import java.nio.*;
import java.nio.channels.*;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

class ClientHandler extends Thread {
	private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
	public static final int MAX_SIZE = 1000;
	SocketChannel clientChannel;

	ClientHandler(SocketChannel socketChannel) {
		clientChannel = socketChannel;
	}

	@Override
	public void run() {
		System.out.println("ClientHandler invoked");
		super.run();
		try {
			doServing();
		} catch (Exception e) {
			logger.info(String.format("Exception %s", Debug.print(e)));
		}
	}

	private void doServing() throws Exception {
		Selector ioSelector = Selector.open();
		clientChannel.register(ioSelector, SelectionKey.OP_READ);
		ByteBuffer transactionBuffer = ByteBuffer.allocate(MAX_SIZE);
		int read = 0;
		while (read >= 0) {
			int selected = ioSelector.select();
			logger.info(String.format("%d channels selected.", selected));
			read = clientChannel.read(transactionBuffer);
			if (read > 0) {
				logger.info(String.format("Did read %d bytes from client %s", read, clientChannel.getRemoteAddress()));
			} else {
				logger.error("Nothing was read, this should not happen");
				break;
			}
			transactionBuffer.clear();
			ioSelector.selectedKeys().clear();
		}
		logger.info(String.format("End of stream reach for %s.", clientChannel.getRemoteAddress()));
	}
}
