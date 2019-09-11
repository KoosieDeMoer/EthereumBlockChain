package com.entersekt.blockchain;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/BlockChain")
@Api(value = "/BlockChain")
public class BlockChainRestService {

	private static final Logger log = LoggerFactory.getLogger(BlockChainRestService.class);

	private BlockChainInterfaceService blockChainInterfaceService = GuiceBindingsModule.injector
			.getInstance(BlockChainInterfaceService.class);

	private String basePath = blockChainInterfaceService.generateBlockchainNodeServiceUrl();

	@GET
	@Path("hash")
	@ApiOperation(value = "Drops a hash in the blockchain")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Hash logged in blockchain") })
	public Response putHash(
			@ApiParam(value = "Hash to put in blockchain", required = true) @QueryParam("hexData") String hexData) {
		final String txHash = blockChainInterfaceService.writeData(hexData);
		basePath = blockChainInterfaceService.generateBlockchainNodeServiceUrl();

		return Response.status(Response.Status.OK).entity(basePath + "/tx/" + txHash).build();
	}

}