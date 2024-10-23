package com.example.demo.distributedTransaction.saga;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class SagaTransaction {

    private Long xid;
    //先进后出
    private ArrayDeque<Rollback> rollBackChain = new ArrayDeque<>();

    public Long getXid() {
        return xid;
    }

    public void setXid(Long xid) {
        this.xid = xid;
    }

    public ArrayDeque<Rollback> getRollBackChain() {
        return rollBackChain;
    }

    public void setRollBackChain(ArrayDeque<Rollback> rollBackChain) {
        this.rollBackChain = rollBackChain;
    }

    public static class Rollback{

        private Class<?> clazz;

        private String methodName;

        private Object[] args;

        private Object target;

        public Class<?> getClazz() {
            return clazz;
        }

        public void setClazz(Class<?> clazz) {
            this.clazz = clazz;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public Object[] getArgs() {
            return args;
        }

        public void setArgs(Object[] args) {
            this.args = args;
        }

        public Object getTarget() {
            return target;
        }

        public void setTarget(Object target) {
            this.target = target;
        }

    }
}
