# 元交易解析

## 元交易原理

1. 代付手续费地址 A，签名已经封装好的签名信息SignedTx中的一些信息，并且生成签名 data。封装该 data 到 SignedTx 的 data 中。整体节点解析以后，会根据 data 中的 feePercent 进行手续费的扣除折算。
2. 元交易标识：SignedTx 的 data hex String 的开头为 0x234d6574615472616e73616374696f6e23

## 元交易的构造流程

* 1. 获取初始签名交易 SignedTx。 (hex String)
* 2. 解析 SignedTx，获取其对应的 nonce， gasprice，gaslimit，from地址，to 地址，value，data




```
Transaction transaction = new Transaction(Hex.decode(this.removeHexPrefix(rawTx)));
```

* 3. RLP 按照 nonce，gasprice，gaslimit，to addr，value，data，from 地址，feePercent, blockNumLimit, chainId（手续费折扣比例，万分之一计算）进行编码获取待签名的数据

```

MetaRawData metaRawData = new MetaRawData(
                transaction.getNonce(),
                transaction.getGasPrice(),
                transaction.getGasLimit(),
                transaction.getReceiveAddress(),
                transaction.getValue(),
                transaction.getData(),
                transaction.getSender(),
                BigInteger.valueOf(feePercent),
                BigInteger.valueOf(blockNumber),
                BigInteger.valueOf(chainId)
        );

byte[] encodeData = metaRawData.getEncodeData();

// 编码方法为

private synchronized void rlpEncode() {
        if (!this.isEncode) {
            List<RlpType> result = new ArrayList();
            result.add(RlpString.create(nonce));
            result.add(RlpString.create(gasPrice));
            result.add(RlpString.create(gasLimit));
            result.add(RlpString.create(receiveAddress));

            result.add(RlpString.create(value));
            result.add(RlpString.create(data));
            result.add(RlpString.create(sendAddress));
            result.add(RlpString.create(feePercent));
            result.add(RlpString.create(blockNumber));
            result.add(RlpString.create(chainId));

            RlpList rlpList = new RlpList(result);
            this.rlpEncodeData = RlpEncoder.encode(rlpList);

            this.rawHash = HashUtil.sha3(this.rlpEncodeData);
            this.isEncode = true;
        }
    }

```

* 4. 代付手续费地址 A，签名第3步获取到的encodeData。并且依据chainId 组装成 EIP155的 Signature

```
    private Sign.SignatureData getSignatureData(byte[] encodeData, String privateKey, int chainId) {
        ECKeyPair aPair = ECKeyPair.create(Numeric.toBigInt(privateKey));
        Sign.SignatureData signatureData = Sign.signMessage(encodeData, aPair);
        Sign.SignatureData eip155SignatureData = TransactionEncoder.createEip155SignatureData(signatureData, metaChainId);

        return eip155SignatureData;
    }

```

* 5. 拼装最新的data字段,  将第4部的signaturedata 传入下面方法

```
private String getMetaData(Transaction transaction, int blockNumber, int feePercent, Sign.SignatureData signatureData) {
        MetaData metaData = new MetaData(
                transaction.getData(),
                BigInteger.valueOf(blockNumber),
                BigInteger.valueOf(feePercent),
                signatureData.getV(),
                signatureData.getR(),
                signatureData.getS()
        );

        byte[] encodeData = metaData.getRlpEncoded();

        return Constant.META_DATA_PREFIX + Hex.toHexString(encodeData);
    }

    // encode 规范为
public byte[] getRlpEncoded() {
        if (this.rlpEncoded != null) {
            return this.rlpEncoded;
        } else {
            List<RlpType> finalResult = new ArrayList();

            finalResult.add(RlpString.create(blockNumLimit));
            finalResult.add(RlpString.create(feePercent));
            finalResult.add(RlpString.create(Bytes.trimLeadingZeroes(this.V)));
            finalResult.add(RlpString.create(Bytes.trimLeadingZeroes(this.R)));
            finalResult.add(RlpString.create(Bytes.trimLeadingZeroes(this.S)));
            finalResult.add(RlpString.create(data));

            RlpList finalRlpList = new RlpList(finalResult);
            byte[] finalEncodeData = RlpEncoder.encode(finalRlpList);
            this.rlpEncoded = finalEncodeData;
            return finalEncodeData;
        }
    }

```

* 6. 更新 SignedTx 中的 data 字段。

```
// 其中 metaData 是第5步获取到的 hex string

  Transaction tx = new Transaction(transaction.getNonce(), transaction.getGasPrice(),
                transaction.getGasLimit(), transaction.getReceiveAddress(), transaction.getValue(),
                ByteUtil.hexStringToBytes(metaData), ByteUtil.bigIntegerToBytes(transaction.getSignature().r),ByteUtil.bigIntegerToBytes(transaction.getSignature().s),
                transaction.getSignature().v, chainId);
                
 return "0x" + Hex.toHexString(tx.getEncoded());
              
```

* 至此获取足够的信息进行广播

```
 EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(metaSignedTx).send();
```

## 元交易decode

```
public void metaDataDecodeTest() throws Exception {
// 元交易 hex string
        String rawhex = "0xf8c602850dbcac8e0082c35094000000000000000000000000000000000000000001b85e234d6574615472616e73616374696f6e23f84b822f930a83050a00a0e2e037042f160bca30ff54921eed7fd8886f5f5cd8e3f7c1a414f2d7f093a2f2a0615d0bd271e181897078229a6bc463269cb463dfb33cf31ba08c30fb9065c5c480830509ffa034ae31e249554cc4727b52581ff2055c0964f429b3ec66c90d98acd8f0e3eb9ba038337050ef236268a3ffd1ba0cd3a344193b93cdff92443bb9345c099e2fe30b";

// 拼装的 data
        String hex = "0x234d6574615472616e73616374696f6e23f84b822f930a83050a00a0e2e037042f160bca30ff54921eed7fd8886f5f5cd8e3f7c1a414f2d7f093a2f2a0615d0bd271e181897078229a6bc463269cb463dfb33cf31ba08c30fb9065c5c480";
        String rawData = hex.substring(Constant.META_DATA_PREFIX.length());
        System.out.println(rawData);

// 解析拼装的 data，获取 blocknumlimit, feepercent, 原来交易的 data
        MetaData metaData = new MetaData(ByteUtil.hexStringToBytes(rawData));

        System.out.println(metaData.getBlockNumLimit());
        System.out.println(metaData.getFeePercent());
        System.out.println(metaData.getRealV(BigInteger.valueOf(chainId)));
        System.out.println("data " + Hex.toHexString(metaData.getData()));
        Sign.SignatureData signatureData = metaData.getSignatureData();

        System.out.println("r:" + Numeric.toHexString(signatureData.getR()));
        System.out.println("s:" + Numeric.toHexString(signatureData.getS()));
        System.out.println("v:" + Numeric.toHexString(signatureData.getV()));

        Transaction transaction = new Transaction(Hex.decode(Util.removeHexPrefix(rawhex)));

        Transaction transaction1 = new Transaction(transaction.getNonce(), transaction.getGasPrice(),
                transaction.getGasLimit(), transaction.getReceiveAddress(), transaction.getValue(),
                metaData.getData(), ByteUtil.bigIntegerToBytes(transaction.getSignature().r),ByteUtil.bigIntegerToBytes(transaction.getSignature().s),
                transaction.getSignature().v, chainId);



        MetaRawData metaRawData = new MetaRawData(
                transaction1.getNonce(),
                transaction1.getGasPrice(),
                transaction1.getGasLimit(),
                transaction1.getReceiveAddress(),
                transaction1.getValue(),
                transaction1.getData(),
                transaction1.getSender(),
                BigInteger.valueOf(feePercent),
                BigInteger.valueOf(metaData.getBlockNumLimit().intValue()),
                BigInteger.valueOf(chainId)
        );
        byte[] encodeData = metaRawData.getEncodeData();

        System.out.println("encode hash :" + Hex.toHexString(Hash.sha3(encodeData)));

// 回复代支付手续费的地址
        ECDSASignature signature = new ECDSASignature(new BigInteger(1, signatureData.getR()), (new BigInteger(1, signatureData.getS())));

        String addressRecovered = "";
        BigInteger publicKey = Sign.recoverFromSignature(metaData.getRealV(BigInteger.valueOf(chainId)).intValue(), signature, Hash.sha3(encodeData));
        byte[] pub = ByteUtil.bigIntegerToBytes(publicKey);
        if (publicKey != null) {
            addressRecovered = "0x" + Keys.getAddress(publicKey);
            System.out.println("recover address:" + addressRecovered);

            Assert.assertEquals("0xaE5FFda3163c68cDa9a9DBDd1c599A8337911d2d", addressRecovered.toLowerCase());
        }

    }

```

## 元交易链上处理

```
如果交易 data 字段符合元交易的格式，并且能够正常解析，那么就进入 mempool。 否者，拒绝改交易，不能进入mempool。

unc metaTransactionCheck(tx *types.Transaction,  b Backend,) error {
	if types.IsMetaTransaction(tx.Data()) {
		metaData, err := types.DecodeMetaData(tx.Data(), b.CurrentBlock().Number())
		if err != nil {
			return err
		}

		signer := types.MakeSigner(b.ChainConfig(), b.CurrentBlock().Number())
		from, err := signer.Sender(tx)
		if err != nil {
			return err
		}

		addr, err := metaData.ParseMetaData(tx.Nonce(), tx.GasPrice(), tx.Gas(), tx.To(), tx.Value(), metaData.Payload, from, b.ChainConfig().ChainID)
		if err != nil {
			return err
		}
		log.Debug("metaTransfer found, feeaddr:", addr.Hex() + " feePercent : " + strconv.FormatUint(metaData.FeePercent, 10))
	}
	return nil
}

```