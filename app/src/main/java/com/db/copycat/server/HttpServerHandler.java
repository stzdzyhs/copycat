package com.db.copycat.server;

import com.db.util.MyAssetManager;
import com.db.util.Util;

import java.io.InputStream;
import java.net.URI;
import java.util.Locale;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private MyAssetManager assetManager;
	private ServerActionHandler serverActionHandler;

	public HttpServerHandler(MyAssetManager assetManager, ServerActionHandler serverActionHandler) {
		this.assetManager = assetManager;
		this.serverActionHandler = serverActionHandler;
	}

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("Channel Read Complete" );
        ctx.flush();
    }
    

    public static final String PKG = ""; // "/net/httpsvr1/data";
    public static final String R404="404.html";

	public static final String resultT = "{\"result\":%d,\"desc\":\"%s\"}";

    final String UTF8="UTF-8";

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

        System.out.printf("msg class name: %s \n", req.getClass().getName() );
		//System.out.println("HttpRequest   ");
		//FullHttpRequest req = (FullHttpRequest) request;
		URI uri = new URI(req.uri());

		String path = uri.getPath();
		// process action....
		if(path==null) {
			path = "";
		}

		ByteBuf resp = null;

		if (path.endsWith(".do")) {
			try {
				if (this.serverActionHandler!=null) {
					String respStr = this.serverActionHandler.handle(req);
					resp = Unpooled.wrappedBuffer(String.format(Locale.CHINA, resultT, 0, respStr).getBytes(UTF8) );
				}
				else {
					resp = Unpooled.wrappedBuffer(String.format(Locale.CHINA, resultT, 2, "action Handler null").getBytes(UTF8) );
				}
			}
			catch(Exception e) {
				resp = Unpooled.wrappedBuffer(String.format(Locale.CHINA, resultT, 3, e.getMessage()).getBytes(UTF8) );
			}
		}
		else {
			resp = loadRes(path);
			if(resp==null) {
				resp = loadRes(R404);
				Util.assertTrue("404 missing", resp!=null);
			}
		}
		sendByteBufResponse(ctx, path, resp);
    }

    public ByteBuf loadRes(String name) {
		try {
			if(name==null) {
				throw new NullPointerException("null name");
			}

			name = name.replace('\\', '/');
			name = name.replaceAll("//*",  "/");

			if(!name.startsWith("/")) {
				name = "/" + name;
			}

			// the problem is: if name is just /, it can leak some info on the server
			if(name.endsWith("/")) {
				return null;
			}

			InputStream is = this.assetManager.open(PKG + name);
			if(is==null) {
				return null;
			}
			byte[] buf = new byte[100000];
			int cnt;

			ByteBuf ret = Unpooled.buffer(100000);
			while((cnt=is.read(buf))!=-1) {
				ret.writeBytes(buf,0, cnt);
			}

			return ret;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
    }

    public String getMimeType(String path) {
    	if(path==null) return null;
    	if(path.endsWith(".html")) {
    		return "text/html; charset=UTF-8";
    	}
    	if(path.endsWith(".js")) {
    		return "text/javascript; charset=UTF-8";
    	}
    	if(path.endsWith(".css")) {
    		return "text/css; charset=UTF-8";
    	}
    	return null;
    }
    
    private void sendByteBufResponse(ChannelHandlerContext ctx, String path, ByteBuf buf) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        
        String mt = getMimeType(path);
        if(mt!=null) {
        	response.headers().set(HttpHeaderNames.CONTENT_TYPE, mt);
        }
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());

        // Write the response.
        ctx.channel().writeAndFlush(response);
    }
    

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
