import java.net.*;
import java.nio.channels.*;
import java.io.*;


import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class AsyncServer implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(AsyncServer.class);

	// main while loop
	// start with single threaded app and then build a multi threaded app.
	// idea is to have a test of 2 client instances and a server instance
	// clients can use simple streams.

	public static void main(String args[]) throws Exception {
		Thread t  = new Thread(new AsyncServer());

		t.start();
		logger.info("AsyncServer thread joining");
		t.join();
		logger.info("AsyncServer thread joined");
	}

	private void syncServer() throws Exception {
		ServerSocket ss = new ServerSocket(1278, 2);

		while (true) {
			logger.info(String.format("server socket - %s", Debug.print(ss)));
			Socket cs = ss.accept();
			logger.info(String.format("accepted socket - %s", Debug.print(cs)));
			InputStream is = cs.getInputStream();
			int count = is.read();
			logger.info(String.format("socket bytes to read - %s", count));
			byte[] readBytes = new byte[count];
			is.read(readBytes);
			logger.info(Debug.printData(readBytes));
		}
	}

	private void asyncServerThreadPerClient() throws Exception {
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		SocketAddress socketAddress = new InetSocketAddress(1278);
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.bind(socketAddress);
		// here value of non blocking is not much but we choose to use it anyways for future
		// evolution.
		Selector acceptSelector = Selector.open();
		serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
		// this is only way to kill a connection, when client dies looks like.
		// probably only linux supports this.
//        serverSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		while (true) {
			acceptSelector.select();
			// this is a non blocking accept call.
			SocketChannel socketChannel = serverSocketChannel.accept();
			socketChannel.configureBlocking(false);
			logger.info(String.format("Connection est %s:%s", socketChannel.getRemoteAddress(), socketChannel.getLocalAddress()));
			new ClientHandler(socketChannel).start();
		}
	}

	private void asyncServerThreadForAllClients() throws Exception {
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		SocketAddress socketAddress = new InetSocketAddress(1278);
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.bind(socketAddress);
		// here value of non blocking is not much but we choose to use it anyways for future
		// evolution.
		Selector acceptSelector = Selector.open();
		serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
		MultiClientHandler multiClientHandler = new MultiClientHandler();
		multiClientHandler.start();
		// this is only way to kill a connection, when client dies looks like.
		// probably only linux supports this.
//        serverSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		while (true) {
			acceptSelector.select();
			// this is a non blocking accept call.
			SocketChannel socketChannel = serverSocketChannel.accept();
			socketChannel.configureBlocking(false);
			// add channel to multi Client handler.
			// it should be able to process all the clients.
			multiClientHandler.addChannel(socketChannel);
		}
	}

	public void run() {
		logger.info("thread joining");
		try {
			Thread.sleep(1000);
			asyncServerThreadForAllClients();
		} catch (Exception e) {
			logger.info(String.format("Exception hit %s", e));
		}
	}
}
