/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.db.copycat.server;

import com.db.util.MyAssetManager;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * An HTTP server that sends back the content of the received HTTP request
 * in a pretty plaintext form.
 */
public final class Server1 {

    private static final boolean SSL = true;
    private static final int PORT = 8443;

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;
    private Channel chSvr;
    private boolean started = false;

    MyAssetManager myAssetManager;
    ServerActionHandler serverActionHandler;

    public Server1(MyAssetManager assetManager, ServerActionHandler serverActionHandler) {
        this.myAssetManager = assetManager;
        this.serverActionHandler = serverActionHandler;
    }

    public boolean isStarted() {
        return this.started;
    }

//    public String getIp() throws Exception {
//        if(chSvr==null) {
//            return null;
//        }
//        String addr = InetAddress.getLocalHost().getHostAddress();
//        return addr;
//    }

    public int getPort() {
        return PORT;
    }

    final static String PKG = "";

    public void start() throws Exception {
        final SslContext sslCtx;
        try {
            if (SSL) {
                // SelfSignedCertificate ssc = new SelfSignedCertificate();
                // sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

                KeyStore keyStore = KeyStore.getInstance("PKCS12"); //KeyStore.getDefaultType());
                InputStream is = myAssetManager.open(PKG + "chenzero.pkcs12");
                if(is==null) {
                    throw new Exception("ks null !!!");
                }
                char[] keyStorePassword = "123456".toCharArray();
                keyStore.load(is, keyStorePassword);

                KeyStore.ProtectionParameter entryPassword =
                        new KeyStore.PasswordProtection(keyStorePassword);

                KeyStore.Entry e = keyStore.getEntry("chenzero",  entryPassword);
                if(!(e instanceof KeyStore.PrivateKeyEntry)) {
                    throw new Exception("invalid ks: " + e.getClass().getName() );
                }

                KeyStore.PrivateKeyEntry pe = (KeyStore.PrivateKeyEntry)e;
                Certificate[] cs0 = pe.getCertificateChain();
                int csLen = pe.getCertificateChain().length;
                X509Certificate[] x509cs = new X509Certificate[csLen];
                for(int i=0; i<csLen; i++) {
                    x509cs[i] = (X509Certificate)cs0[i];
                }
                sslCtx = SslContextBuilder.forServer(pe.getPrivateKey(), x509cs).build();
            }
            else {
                //sslCtx = null;
            }

            // Configure the server.
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.option(ChannelOption.SO_BACKLOG, 20);
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HttpServerInitializer(sslCtx, this.myAssetManager, serverActionHandler));

            chSvr = bootstrap.bind(PORT).sync().channel();

            started = true;

            System.out.println("Open your web browser and navigate to " +
                    (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');

        }
        catch(Exception e) {
            stop();
            throw e;
        }
    }

    public void stop() {
        started = false;

        if(this.chSvr!=null) {
            this.chSvr.close(); //.sync();
            this.chSvr=null;
        }
        if(this.bossGroup!=null) {
            bossGroup.shutdownGracefully();
            this.bossGroup=null;
        }
        if(this.workerGroup!=null) {
            workerGroup.shutdownGracefully();
            this.workerGroup = null;
        }
    }

}
