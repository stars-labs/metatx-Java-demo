package com.hecochain.metatx;

import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Data
public class MetaTx {

    private Long id;

    private String txHash;

    private long feePercent;

    private String rawTx;

    private String metaRawTx;

    private BigInteger blockHeight;

    private String rawTxHash;

    private BigInteger from_addr_balance;

    private Date created;

    private Date updated;
}
