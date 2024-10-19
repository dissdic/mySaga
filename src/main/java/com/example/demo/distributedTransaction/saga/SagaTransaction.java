package com.example.demo.distributedTransaction.saga;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class SagaTransaction {

    private Long xid;

    private List<Runnable> rollBackChain = new ArrayList<>();

    public Long getXid() {
        return xid;
    }

    public void setXid(Long xid) {
        this.xid = xid;
    }

    public List<Runnable> getRollBackChain() {
        return rollBackChain;
    }

    public void setRollBackChain(List<Runnable> rollBackChain) {
        this.rollBackChain = rollBackChain;
    }
}
