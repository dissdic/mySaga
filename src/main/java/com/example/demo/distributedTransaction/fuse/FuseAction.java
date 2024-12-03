package com.example.demo.distributedTransaction.fuse;

import java.lang.annotation.*;
import java.math.BigDecimal;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface FuseAction {

    final static int Stream = 0;

    final static int QPS = 1;

    final static int ExceptionRate = 2;

    final static int AvgDuration= 3;

    double threshold();

}
