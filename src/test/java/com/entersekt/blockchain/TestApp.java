package com.entersekt.blockchain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ethereum.crypto.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import com.entersekt.blockchain.ethereum.BlockChainInterfaceServiceEthereumImpl;
import com.entersekt.configuration.ConfigurationService;

public class TestApp {

	private static final Logger log = LoggerFactory.getLogger(TestApp.class);

	private static final ConfigurationService configService = GuiceBindingsModule.injector
			.getInstance(ConfigurationService.class);

	// private BlockChainInterfaceService blockChainInterfaceService = GuiceBindingsModule.injector
	// .getInstance(BlockChainInterfaceService.class);

	private static String starts; // = "affab1e6206225089082";

	// private BlockChainInterfaceServiceEthereumImpl blockChainInterfaceService = new
	// BlockChainInterfaceServiceEthereumImpl();

	public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
		final TestApp app = new TestApp();
		// app.test();
		// app.makeAndStoreSenderData();

		if (args.length < 1) {
			System.err.println("Usage: startsWith, eg affab1e6206225089082");
			System.exit(0);
		}
		starts = args[0];
		int maxThreadCount = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(maxThreadCount - 1);

		for (int i = 0; i < (maxThreadCount - 1); i++) {
			Runnable worker = new Runnable() {

				@Override
				public void run() {
					System.out.println("starting thread");
					app.makeAndStoreSenderData();

				}
			};
			executor.execute(worker);
		}

	}

	private void makeAndStoreSenderData() {
		while (true) {
			ECKey key = new ECKey();

			byte[] addr = key.getAddress();
			byte[] priv = key.getPrivKeyBytes();

			String addrBase16 = Hex.toHexString(addr);
			String privBase16 = Hex.toHexString(priv);

			if (addrBase16.startsWith(starts)) {
				System.out.println("Address     : " + addrBase16);
				System.out.println("Private Key : " + privBase16);

				configService.setProperty("SenderAddress", BlockChainInterfaceServiceEthereumImpl.PROPERTY_KEY,
						addrBase16);

				configService.setProperty("SenderPrivateKey", BlockChainInterfaceServiceEthereumImpl.PROPERTY_KEY,
						privBase16);
				configService.persist();
				System.exit(0);
			}
		}
	}

	private void test() {

		// System.out.println("Blockchain transaction count: " + blockChainInterfaceService.getTransactionCount());
		// System.out
		// .println("Blockchain gas price: " + ByteUtil.byteArrayToInt(blockChainInterfaceService.getGasPrice()));
		// System.out.println("Blockchain logged transaction details: " +
		// blockChainInterfaceService.writeData("affab1e"));

	}

}
