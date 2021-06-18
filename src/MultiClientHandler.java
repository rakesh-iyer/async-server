import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.locks.*;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class MultiClientHandler extends Thread {
	private static final Logger logger = LoggerFactory.getLogger(MultiClientHandler.class);
	Map<SelectionKey, SocketChannel> clientChannelMap = new HashMap<>();
	Map<SocketChannel, SelectionKey> clientSelectionKeyMap = new HashMap<>();
	Selector readSelector;
	Lock selectorLock = new ReentrantLock();

	MultiClientHandler() throws Exception {
		readSelector = Selector.open();
	}

	public void addChannel(SocketChannel channel) throws Exception {
		// using a lock lets synchronize the main thread with any insertions.
		// lets remove the lock if the upper layer is a single thread.
		selectorLock.lock();
		SelectionKey key = channel.register(readSelector, SelectionKey.OP_READ);
		clientChannelMap.put(key, channel);
		clientSelectionKeyMap.put(channel, key);
		selectorLock.unlock();
	}

	public void removeChannel(SocketChannel channel) throws Exception {
		// using a lock lets synchronize the main thread with any removals.
		// lets remove the lock if the upper layer is a single thread.
		selectorLock.lock();
		SelectionKey key = clientSelectionKeyMap.get(channel);
		key.cancel();
		clientChannelMap.remove(key);
		clientSelectionKeyMap.remove(channel);
		selectorLock.unlock();
	}

	public void doServing() throws Exception {
		// assuming the clients arent sending more than 64k at one go combined.
		ByteBuffer bbCommon = ByteBuffer.allocateDirect(64000);
		List<SocketChannel> readyClientChannels = new ArrayList<>();
		while (true) {
			if (clientChannelMap.isEmpty()) {
				logger.info("No clients connected yet, sleep for a second.");
				Thread.sleep(1000);
				continue;
			}

			int numReady = readSelector.select();
			logger.info(String.format("%d channels ready for read.", numReady));

			Iterator<SelectionKey> skIterator = readSelector.selectedKeys().iterator();
			while (skIterator.hasNext()) {
				readyClientChannels.add(clientChannelMap.get(skIterator.next()));
				skIterator.remove();
			}

			for (SocketChannel readyClientChannel: readyClientChannels) {
				int read = readyClientChannel.read(bbCommon);
				if (read < 0) {
					// the channel has been disconnected from client side.
					logger.info(String.format("Eof from client %s", readyClientChannel.getRemoteAddress()));
					removeChannel(readyClientChannel);
					continue;
				}

				logger.info(String.format("Read %d bytes from client %s", read, readyClientChannel.getRemoteAddress()));
			}

			// clear both the bytebuffer and the ready channels for another select loop.
			bbCommon.clear();
			readyClientChannels.clear();
		}
	}

	@Override
	public void run() {
		super.run();
		try {
			doServing();
		} catch (Exception e) {
			logger.error("Exception was arrived at " + e);
		}
	}
}
