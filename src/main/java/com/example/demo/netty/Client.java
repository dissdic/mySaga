package com.example.demo.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Client {

    public static void main(String[] args){

        EventLoopGroup boss = new NioEventLoopGroup();
        try{
            ChannelFuture future = new Bootstrap()
                    .group(boss)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new RetryChannelOutboundHandler());
                }
            }).remoteAddress("localhost",8080)
                    .localAddress(9090)
                    .connect().sync();
            Channel channel = future.channel();

            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(()->{
                try{
                    ByteBuf msg = Unpooled.copiedBuffer("你好".getBytes());
                    System.out.println("4");
                    channel.writeAndFlush(msg).syncUninterruptibly();
                }catch (Exception e){
                    e.printStackTrace();
                }

            },0,3,TimeUnit.SECONDS);

            channel.closeFuture().syncUninterruptibly();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
        }
    }

    public static class RetryChannelOutboundHandler extends ChannelOutboundHandlerAdapter {

        private Timer timer = new HashedWheelTimer();
        private static final int MAX = 3;
        private static final long DURATION = 3;
        Map<Object,Integer> times = new HashMap<>();

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

            promise.addListener(future->{
                System.out.println("1 "+ Thread.currentThread()+ " "+ future.isSuccess());
                if(future.isSuccess()){
                    //停止计时器
                    timer.stop();
                }else if(future.isDone() || !future.isSuccess()){
                    retry(ctx,msg);
                }
            });
            System.out.println("3 "+Thread.currentThread());
            super.write(ctx, msg, promise);
        }

        private void retry(ChannelHandlerContext ctx, Object msg){
            times.putIfAbsent(msg,1);
            timer.newTimeout(timeout -> {
                System.out.println("2");
                if(times.get(msg)<=MAX){
                    ctx.writeAndFlush(msg);
                    times.put(ctx,times.get(ctx)+1);
                }else{
                    System.out.println("发送失败："+ctx);
                }

            },DURATION, TimeUnit.SECONDS);

        }
    }

}
