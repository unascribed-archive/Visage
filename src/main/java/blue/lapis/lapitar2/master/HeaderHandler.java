package blue.lapis.lapitar2.master;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

public class HeaderHandler extends HandlerWrapper {
	private final String header, value;
	public HeaderHandler(String header, String value, Handler handler) {
		this.header = header;
		this.value = value;
		setHandler(handler);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		if (value != null) {
			response.setHeader(header, value);
		}
		super.handle(target, baseRequest, request, response);
	}
}
