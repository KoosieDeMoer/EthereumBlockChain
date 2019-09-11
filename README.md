# EthereumBlockChain
Drops document hashes on the Ethereum Blockchain as Proof of Existence

Wraps the etherscan.io interface to the Ethereum Ropsten chain (id=3) in a RESTful service.

All the service does is submit a 'contract start' (ie no recipient address) transaction with hex data in the 'data' section to the ropsten.ethereum.org blockchain. It return a URL that will load the transaction in a browser so the 'Input data' can be viewed.

The purpose of this is to serve as a proof of existence of a document that has a timestamp signature that in hex form is in the blockchain.

The sender used is 0x8affab1e30056e7098889d4962dcbcad8f8bdcd7 - 8 affable - send me some ether if you like.

HTTP-GET :8600/BlockChain/hash?hexData=f1e2d3c4b5a6... will drop the hash on the Etherium blockchain.

## Swagger-ui URL ##
http://localhost:8600/swagger-ui/?url=/swagger.json

## Configuration ##
Use the .conf files, eg morden.con, ropsten.conf etc
