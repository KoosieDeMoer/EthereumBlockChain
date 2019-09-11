package com.entersekt.blockchain.ethereum;

import java.math.BigInteger;
import java.util.Date;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.pqc.math.linearalgebra.ByteUtils;
import org.spongycastle.util.encoders.Hex;

import com.entersekt.blockchain.App;
import com.entersekt.blockchain.BlockChainInterfaceService;
import com.entersekt.communications.RestfulExternalCommunicationsServiceBase;
import com.entersekt.hub.GuiceBindingsModule;
import com.entersekt.json.JsonSerialisationService;
import com.entersekt.utils.AppUtils;

public class BlockChainInterfaceServiceEthereumImpl extends RestfulExternalCommunicationsServiceBase implements
		BlockChainInterfaceService {

	private static final int GAS_PRICING_FACTOR_FOR_SPEED = 5;

	private static final Logger log = LoggerFactory.getLogger(BlockChainInterfaceServiceEthereumImpl.class);

	public static final String PROPERTY_KEY = "EthereumInterface";

	public static final String ETHEREUM_MODULE = "proxy";
	public static final String ETHEREUM_LATEST_TAG = "latest";
	public static final String ETHEREUM_HEX_PREFIX = "0x";
	public static final String ELEMENT_NAME_ERROR = "error";
	public static final String ELEMENT_NAME_RESULT = "result";

	JsonSerialisationService jsonSerialisationService = GuiceBindingsModule.injector
			.getInstance(JsonSerialisationService.class);

	static String apiKey = configService.getProperty(AppUtils.API_KEY, PROPERTY_KEY);
	static String senderAddress = configService.getProperty("SenderAddress", PROPERTY_KEY);
	static String senderPrivateKey = configService.getProperty("SenderPrivateKey", PROPERTY_KEY);
	static int chainId = Integer.parseInt(configService.getProperty("ChainId", PROPERTY_KEY));
	static String receiverAddress = configService.getProperty("ReceiverAddress", PROPERTY_KEY);

	private static final String ethereumNodeHost = configService.getProperty(AppUtils.PROPERTY_TYPE_HTTP_HOSTNAME,
			PROPERTY_KEY);

	public BlockChainInterfaceServiceEthereumImpl() {
		super();
		init(PROPERTY_KEY);
	}

	@Override
	public String writeData(String hexData) {
		byte[] nonce = ByteUtil.intToBytesNoLeadZeroes(getTransactionCountFake() + 1);
		byte[] gasPrice = ByteUtil.intToBytesNoLeadZeroes(getGasPrice() * GAS_PRICING_FACTOR_FOR_SPEED);
		byte[] receiverAddress2 = new byte[0]; // ByteUtil.hexStringToBytes(receiverAddress);
		Transaction tx = new Transaction(nonce, gasPrice, ByteUtil.longToBytesNoLeadZeroes(4700000), receiverAddress2,
				ByteUtil.bigIntegerToBytes(BigInteger.valueOf(0)), // 0 gwei
				ByteUtil.hexStringToBytes(hexData), chainId);

		tx.sign(generateECKey());

		byte[] rawTx = tx.getEncoded();

		String rawTxString = ByteUtils.toHexString(rawTx);

		// https://ropsten.etherscan.io/api?module=proxy&action=eth_estimateGas&to=0xf0160428a8552ac9bb7e050d90eeade4ddd52843&value=0xff22&gasPrice=0x051da038cc&gas=0xffffff&apikey=YourApiKeyToken

		WebTarget target = baseTarget.queryParam("module", ETHEREUM_MODULE).queryParam("apikey", apiKey)
				.queryParam("action", "eth_sendRawTransaction").queryParam("hex", rawTxString);

		log.info("Submitting signed raw transaction to '" + ethereumNodeHost + "'");
		Response response = target.request(MediaType.APPLICATION_JSON).get();

		wrapReturnedError(target, response);
		log.info("Signed raw transaction subitted to '" + ethereumNodeHost
				+ "' sucessfully. Response may indicate errors.");

		return extractValue(response);
	}

	public int getGasPrice() {

		WebTarget target = baseTarget.queryParam("module", ETHEREUM_MODULE).queryParam("apikey", apiKey)
				.queryParam("action", "eth_gasPrice");

		log.info("Requesting gas price from '" + ethereumNodeHost + "' with: \n" + target.getUri());
		Response response = target.request(MediaType.APPLICATION_JSON).get();

		wrapReturnedError(target, response);

		String retValString = extractValue(response);
		log.info("Received gas price from '" + ethereumNodeHost + "' sucessfully: " + retValString);
		return ByteUtil.byteArrayToInt(ByteUtil.hexStringToBytes(retValString));
	}

	public int getTransactionCount() {

		WebTarget target = baseTarget.queryParam("module", ETHEREUM_MODULE).queryParam("apikey", apiKey)
				.queryParam("action", "eth_getTransactionCount")
				.queryParam("address", ETHEREUM_HEX_PREFIX + senderAddress).queryParam("tag", ETHEREUM_LATEST_TAG);

		log.info("Requesting transaction price from '" + ethereumNodeHost + "' with: \n" + target.getUri());
		Response response = target.request(MediaType.APPLICATION_JSON).get();

		wrapReturnedError(target, response);

		String retValString = extractValue(response);
		log.info("Received transaction count from '" + ethereumNodeHost + "' sucessfully: " + retValString);
		return ByteUtil.byteArrayToInt(ByteUtil.hexStringToBytes(retValString));
	}

	public int getTransactionCountFake() {

		// the transactions are taking to long and eth_getTransactionCount is not taking pending transactions into
		// account
		// so the transaction count will be the number of seconds I have been alive
		Date dob = new Date(62, 5, 22, 1, 50, 00);
		int secondsAlive = (int) ((new Date().getTime() - dob.getTime()) / 1000);
		return secondsAlive;
	}

	@Override
	public String generateBlockchainNodeServiceUrl() {
		String protocol = App.configService.getProperty(AppUtils.PROPERTY_TYPE_PROTOCOL,
				BlockChainInterfaceServiceEthereumImpl.PROPERTY_KEY);
		String hostName = App.configService.getProperty(AppUtils.PROPERTY_TYPE_HTTP_HOSTNAME,
				BlockChainInterfaceServiceEthereumImpl.PROPERTY_KEY);
		String targetPort = App.configService.getProperty(AppUtils.PROPERTY_TYPE_HTTP_PORT,
				BlockChainInterfaceServiceEthereumImpl.PROPERTY_KEY);
		return protocol + "://" + hostName + ":" + targetPort;
	}

	private String extractValue(Response response) {
		String responseData = response.readEntity(String.class);

		jsonSerialisationService.setCurrentDoc(responseData);

		try {
			return jsonSerialisationService.getElementValue(ELEMENT_NAME_RESULT);
		} catch (NullPointerException e) {
			// no result so should have an error
			String errorString = "Ethereum blockchain at '"
					+ ethereumNodeHost
					+ "' returned error: "
					+ jsonSerialisationService.deSerialise(
							jsonSerialisationService.getObjectElementValue(ELEMENT_NAME_ERROR), JsonRpcError.class).message;
			throw new IllegalArgumentException(errorString);
		}
	}

	private ECKey generateECKey() {
		return ECKey.fromPrivate(ByteUtil.bytesToBigInteger(Hex.decode(senderPrivateKey)));
	}

}
