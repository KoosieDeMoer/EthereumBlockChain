package com.entersekt.blockchain;

import com.entersekt.blockchain.ethereum.BlockChainInterfaceServiceEthereumImpl;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class GuiceBindingsModule extends com.entersekt.hub.GuiceBindingsModule {

	public static final GuiceBindingsModule module = new GuiceBindingsModule();
	public static final Injector injector = Guice.createInjector(module);

	@Override
	protected void configure() {
		super.configure();
		bind(BlockChainInterfaceService.class).to(BlockChainInterfaceServiceEthereumImpl.class);
	}

}
