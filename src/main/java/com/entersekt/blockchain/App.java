package com.entersekt.blockchain;

import java.net.UnknownHostException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entersekt.configuration.ConfigurationService;
import com.entersekt.hub.common.RestCommonService;
import com.entersekt.topology.Node;
import com.entersekt.topology.NodeRegisterService;
import com.entersekt.topology.NodeType;
import com.entersekt.utils.AppUtils;
import com.entersekt.utils.SwaggerUtils;

public class App {

	private static final Logger log = LoggerFactory.getLogger(App.class);

	static String myHostname;
	static Node myNode;
	static int myPortNo;
	static String hubHostname;
	static int hubPortNo;

	public static NodeRegisterService nodeRegisterService = GuiceBindingsModule.injector
			.getInstance(NodeRegisterService.class);

	private static String who = com.entersekt.hub.App.BLOCKCHAIN_WHO;

	public static final ConfigurationService configService = GuiceBindingsModule.injector
			.getInstance(ConfigurationService.class);

	public static void main(String[] args) throws Exception {
		usage(args);
		new App().start();
	}

	public void start() throws Exception, UnknownHostException, InterruptedException {
		final HandlerList handlers = new HandlerList();

		ResourceConfig resourceConfig = new ResourceConfig();

		resourceConfig.packages(BlockChainRestService.class.getPackage().getName(), RestCommonService.class
				.getPackage().getName());

		SwaggerUtils.buildSwaggerBean("Ethereum Bridge", "Ethereum Bridge API", BlockChainRestService.class
				.getPackage().getName() + "," + RestCommonService.class.getPackage().getName());

		SwaggerUtils.attachSwagger(handlers, App.class, resourceConfig);

		ServletContainer servletContainer = new ServletContainer(resourceConfig);
		ServletHolder jerseyServlet = new ServletHolder(servletContainer);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.addServlet(jerseyServlet, "/*");

		handlers.addHandler(context);

		Server jettyServer = new Server(myPortNo);

		jettyServer.setHandler(handlers);

		// tell the Hub about me
		final NodeType nodeType = NodeType.BLOCKCHAIN;

		myNode = nodeRegisterService.registerNodeWithHub(nodeType, who, who, myHostname, myPortNo, null, hubHostname,
				hubPortNo);

		try {
			jettyServer.start();
			jettyServer.join();
		} finally {
			nodeRegisterService.deregister(who, nodeRegisterService.getNode(com.entersekt.hub.App.WHO));
			jettyServer.destroy();
		}
	}

	private static void usage(String[] args) throws Exception {

		if (args.length < 4) {
			log.error("Usage requires command line parameters MY_HOSTNAME, MY_PORT, HUB_HOSTNAME, HUB_PORT  eg 192.168.99.100 8081 192.168.99.100 8080");
			System.exit(0);
		} else {
			myHostname = args[0];
			myPortNo = AppUtils.extractPortNumber(args[1]);
			hubHostname = args[2];
			hubPortNo = AppUtils.extractPortNumber(args[3]);

		}
		log.info("Starting Blockchain interface service with parameters:  myHostname='" + myHostname + "', myPortNo='"
				+ myPortNo + "', hubHostname='" + hubHostname + "', hubPortNo='" + hubPortNo + "'");
	}

}
