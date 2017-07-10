/*
 * The MIT License
 *
 * Copyright (c) 2015-2017, William Thompson (unascribed)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.surgeplay.visage.renderer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.zip.InflaterInputStream;

import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.stb.STBEasyFont;
import com.github.steveice10.mc.auth.data.GameProfile;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.sixlegs.png.PngImage;
import com.surgeplay.visage.RenderMode;
import com.surgeplay.visage.Visage;
import com.surgeplay.visage.renderer.RenderConfiguration.Type;
import com.surgeplay.visage.renderer.render.Renderer;
import com.surgeplay.visage.renderer.util.Textures;
import com.surgeplay.visage.util.Profiles;

import static com.surgeplay.visage.renderer.util.Errors.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class RenderContext extends Thread {
	public static final float[] vertices = {
		// Front
		-1.0f, -1.0f,  1.0f,
		0, 0, 1,
		 1.0f, -1.0f,  1.0f,
		0, 0, 1,
		 1.0f,  1.0f,  1.0f,
		 0, 0, 1,
		-1.0f,  1.0f,  1.0f,
		0, 0, 1,
		// Back
		-1.0f, -1.0f, -1.0f,
		0, 0, -1,
		 1.0f, -1.0f, -1.0f,
		0, 0, -1,
		 1.0f,  1.0f, -1.0f,
		0, 0, -1,
		-1.0f,  1.0f, -1.0f,
		0, 0, -1,
		// Top
		-1.0f,  1.0f,  1.0f,
		0, 1, 0,
		 1.0f,  1.0f,  1.0f,
		 0, 1, 0,
		 1.0f,  1.0f, -1.0f,
		 0, 1, 0,
		-1.0f,  1.0f, -1.0f,
		0, 1, 0,
		// Bottom
		-1.0f, -1.0f, -1.0f,
		0, -1, 0,
		 1.0f, -1.0f, -1.0f,
		0, -1, 0,
		 1.0f, -1.0f,  1.0f,
		0, -1, 0,
		-1.0f, -1.0f,  1.0f,
		0, -1, 0,
		// Left
		 1.0f, -1.0f,  1.0f,
		1, 0, 0,
		 1.0f, -1.0f, -1.0f,
		1, 0, 0,
		 1.0f,  1.0f, -1.0f,
		1, 0, 0,
		 1.0f,  1.0f,  1.0f,
		1, 0, 0,
		// Right
		-1.0f, -1.0f, -1.0f,
		-1, 0, 0,
		-1.0f, -1.0f,  1.0f,
		-1, 0, 0,
		-1.0f,  1.0f,  1.0f,
		-1, 0, 0,
		-1.0f,  1.0f, -1.0f,
		-1, 0, 0,
	};
	public static final float[] planeVertices = {
		-1.0f,  0.0f,  1.0f,
		 0, 1, 0,
		 1.0f,  0.0f,  1.0f,
		 0, 1, 0,
		 1.0f,  0.0f, -1.0f,
		 0, 1, 0,
		-1.0f,  0.0f, -1.0f,
		 0, 1, 0,
	};
	
	private static final int CANVAS_WIDTH = 512;
	private static final float CANVAS_WIDTHf = CANVAS_WIDTH;
	private static final int CANVAS_HEIGHT = 832;
	private static final float CANVAS_HEIGHTf = CANVAS_HEIGHT;
	
	private static final int SUPERSAMPLING = 4;
	
	private static final BufferedImage shadow;
	static {
		try {
			shadow = ImageIO.read(ClassLoader.getSystemResource("shadow.png"));
		} catch (IOException e) {
			throw new InternalError(e);
		}
	}
	
	public int cubeVbo, planeVbo, texture, shadowTexture;
	
	public int fbo, fboTex;
	public int fxaaProgram;
	
	private static int nextId = 1;
	public VisageRenderer parent;
	private Map<RenderConfiguration, Renderer> renderers = Maps.newHashMap();
	private boolean run = true;
	private BlockingDeque<Delivery> toProcess = new LinkedBlockingDeque<>();
	
	public RenderContext(VisageRenderer parent) {
		super("Render thread #"+(nextId++));
		this.parent = parent;
	}
	
	@Override
	public void run() {
		try {
			Visage.log.info("Creating window");
			
			if (!glfwInit()) {
				checkGLFWError();
				throw new RuntimeException("Failed to initialize GLFW");
			}
			
			glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
			
			glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
			
			if (!parent.config.getBoolean("visible")) {
				glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
				glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_FALSE);
			} else {
				glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
				glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
			}
			
			long window = glfwCreateWindow(CANVAS_WIDTH, CANVAS_HEIGHT, "Visage v"+Visage.VERSION+" ["+getName()+"]", NULL, NULL);
			if (window == NULL) {
				checkGLFWError();
				throw new RuntimeException("Failed to create window");
			}
			glfwMakeContextCurrent(window);
			GL.createCapabilities();
			
			if (!GL.getCapabilities().OpenGL30) {
				throw new RuntimeException("OpenGL 3.0 is required");
			}
			
			if (parent.config.getBoolean("continuous") && parent.config.getBoolean("visible")) {
				glfwSwapInterval(60);
			}
			
			if (Visage.debug) {
				GLUtil.setupDebugMessageCallback();
			}
			
			IntBuffer ids = BufferUtils.createIntBuffer(2);
			glGenBuffers(ids);
			cubeVbo = ids.get();
			planeVbo = ids.get();
			checkGLError();
			
			IntBuffer textures = BufferUtils.createIntBuffer(3);
			glGenTextures(textures);
			texture = textures.get();
			shadowTexture = textures.get();
			fboTex = textures.get();
			checkGLError();
			
			Textures.upload(shadow, GL_RGBA8, shadowTexture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			checkGLError();
			
			glBindTexture(GL_TEXTURE_2D, fboTex);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, CANVAS_WIDTH*SUPERSAMPLING, CANVAS_HEIGHT*SUPERSAMPLING, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
			
			fbo = glGenFramebuffers();
			
			int depth = glGenRenderbuffers();
			
			glBindFramebuffer(GL_FRAMEBUFFER, fbo);
			glDrawBuffer(GL_COLOR_ATTACHMENT0);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTex, 0);
			glBindRenderbuffer(GL_RENDERBUFFER, depth);
			glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, CANVAS_WIDTH*SUPERSAMPLING, CANVAS_HEIGHT*SUPERSAMPLING);
			glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depth);
			checkFramebufferStatus();
			
			glBindFramebuffer(GL_FRAMEBUFFER, 0);
			
			
			FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
			vertexBuffer.put(vertices);
			vertexBuffer.flip();
			glBindBuffer(GL_ARRAY_BUFFER, cubeVbo);
			glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
			checkGLError();
			
			FloatBuffer planeVertexBuffer = BufferUtils.createFloatBuffer(planeVertices.length);
			planeVertexBuffer.put(planeVertices);
			planeVertexBuffer.flip();
			glBindBuffer(GL_ARRAY_BUFFER, planeVbo);
			glBufferData(GL_ARRAY_BUFFER, planeVertexBuffer, GL_STATIC_DRAW);
			checkGLError();
			
			glClearColor(0, 0, 0, 0);
			glClearDepth(1.0);
			checkGLError();
			
			glShadeModel(GL_SMOOTH);
			glCullFace(GL_BACK);
			checkGLError();
			
			glEnable(GL_DEPTH_TEST);
			glDepthFunc(GL_LEQUAL);
			checkGLError();
			
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			checkGLError();
			
			FloatBuffer lightColor = BufferUtils.createFloatBuffer(4);
			lightColor.put(3f);
			lightColor.put(3f);
			lightColor.put(3f);
			lightColor.put(1.0f);
			lightColor.flip();
			glLightfv(GL_LIGHT0, GL_AMBIENT, lightColor);
			
			FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
			lightPosition.put(-4f);
			lightPosition.put(-2f);
			lightPosition.put(1f);
			lightPosition.put(1000f);
			lightPosition.flip();
			glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);

			glEnable(GL_LIGHTING);
			glEnable(GL_LIGHT0);
			glEnable(GL_RESCALE_NORMAL);
			glFrontFace(GL_CW);
			glShadeModel(GL_SMOOTH);
			checkGLError();
			
			Visage.log.info("Waiting for jobs");
			try {
				if (parent.config.getBoolean("continuous")) {
					
					Map<String, BufferedImage> skins = ImmutableMap.<String, BufferedImage>builder()
							.put("X-Alex", ImageIO.read(ClassLoader.getSystemResource("alex.png")))
							.put("X-Steve", ImageIO.read(ClassLoader.getSystemResource("steve.png")))
							.put("X-Test", ImageIO.read(ClassLoader.getSystemResource("test_skin.png")))
							.put("Falkreon", ImageIO.read(URI.create("https://visage.surgeplay.com/skin/Falkreon").toURL()))
							.put("unascribed", ImageIO.read(URI.create("https://visage.surgeplay.com/skin/unascribed").toURL()))
							.put("Dinnerbone", ImageIO.read(URI.create("https://visage.surgeplay.com/skin/Dinnerbone").toURL()))
							.build();
					
					ByteBuffer fontBuffer = BufferUtils.createByteBuffer(276480);
					
					RenderConfiguration conf = new RenderConfiguration(Type.BODY, false, true, false);
					String[] skinKeys = skins.keySet().toArray(new String[skins.size()]);
					
					boolean[] showText = {true};
					int[] skinIdx = {1};
					
					glfwSetKeyCallback(window, new GLFWKeyCallback() {
						@Override
						public void invoke(long window, int key, int scancode, int action, int mods) {
							if (action == GLFW_PRESS) {
								if (key == GLFW_KEY_K) {
									skinIdx[0] = (skinIdx[0]+1)%skins.size();
								} else if (key == GLFW_KEY_T) {
									conf.setType(Type.values()[(conf.getType().ordinal()+1)%Type.values().length]);
								} else if (key == GLFW_KEY_F) {
									conf.setFull(!conf.isFull());
								} else if (key == GLFW_KEY_P) {
									conf.setFlipped(!conf.isFlipped());
								} else if (key == GLFW_KEY_S) {
									conf.setSlim(!conf.isSlim());
								} else if (key == GLFW_KEY_SPACE) {
									showText[0] = !showText[0];
								} else if (key == GLFW_KEY_DELETE) {
									renderers.values().forEach(Renderer::destroy);
									renderers.clear();
								}
							}
						}
					});
					ByteBuffer pattern = BufferUtils.createByteBuffer(128);
					pattern.put(new byte[] { 0, 0, -1, -1, 0, 0, -1, -1, 0, 0,
							-1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0,
							0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1,
							0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1,
							-1, 0, 0, -1, -1, 0, 0, -1, -1, -1, -1, 0, 0, -1,
							-1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0,
							-1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0,
							0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1,
							0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, });
					pattern.flip();
					
					while (run) {
						glfwPollEvents();
						drawContinuous(skins.get(skinKeys[skinIdx[0]]), skinKeys[skinIdx[0]], conf, pattern, fontBuffer, showText[0]);
						glfwSwapBuffers(window);
					}
				} else {
					while (run) {
						if (parent.config.getBoolean("visible")) {
							glfwPollEvents();
						}
						Delivery delivery = toProcess.take();
						try {
							processDelivery(delivery);
						} catch (Exception e) {
							Visage.log.log(Level.SEVERE, "An unexpected error occurred while rendering", e);
							BasicProperties props = delivery.getProperties();
							BasicProperties replyProps = new BasicProperties.Builder().correlationId(props.getCorrelationId()).build();
							ByteArrayOutputStream ex = new ByteArrayOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(ex);
							oos.writeObject(e);
							oos.flush();
							parent.channel.basicPublish("", props.getReplyTo(), replyProps, buildResponse(1, ex.toByteArray()));
							parent.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
						}
						if (parent.config.getBoolean("visible")) {
							glfwSwapBuffers(window);
						}
					}
				}
				glfwDestroyWindow(window);
				renderers.values().forEach(Renderer::destroy);
				renderers.clear();
			} catch (Exception e) {
				Visage.log.log(Level.SEVERE, "A fatal error has occurred in the render thread run loop.", e);
			}
		} catch (Exception e) {
			Visage.log.log(Level.SEVERE, "A fatal error has occurred while setting up a render thread.", e);
		}
	}

	// TODO the debug interface should be moved to a separate class
	private void drawContinuous(BufferedImage skin, String skinName, RenderConfiguration conf, ByteBuffer pattern, ByteBuffer fontBuf, boolean showText) throws Exception {
		glColor3f(1, 1, 1);
		int h = CANVAS_WIDTH;
		if (conf.isFull()) {
			h = CANVAS_HEIGHT;
		}
		
		glFrontFace(GL_CCW);
		glClearColor(0.4f, 0.4f, 0.4f, 1);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glPolygonStipple(pattern);
		glEnable(GL_POLYGON_STIPPLE);
		glColor3f(0.6f, 0.6f, 0.6f);
		glBegin(GL_QUADS); {
			glVertex2f(0, conf.isFull() ? 0 : CANVAS_HEIGHT-CANVAS_WIDTH);
			glVertex2f(CANVAS_WIDTH, conf.isFull() ? 0 : CANVAS_HEIGHT-CANVAS_WIDTH);
			glVertex2f(CANVAS_WIDTH, CANVAS_HEIGHT);
			glVertex2f(0, CANVAS_HEIGHT);
		} glEnd();
		glDisable(GL_POLYGON_STIPPLE);
		
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glColor3f(1, 1, 1);
		
		glPushMatrix();
			draw(conf, CANVAS_WIDTH, h, new GameProfile(new UUID(0L, 0L), "continuous_test"), skin, Collections.emptyMap());
		glPopMatrix();
		
		glPushMatrix();
			setup2D(CANVAS_WIDTH, CANVAS_HEIGHT);
			
			glDisable(GL_LIGHTING);
			glDisable(GL_DEPTH_TEST);
			glDisable(GL_CULL_FACE);
			glDisable(GL_TEXTURE_2D);
			
			
			if (showText) {
				drawText(fontBuf, false, "sKin: "+skinName, 5, color(true, isKeyPressed(GLFW_KEY_K)));
				drawText(fontBuf, false, "Type: "+conf.getType(), 15, color(true, isKeyPressed(GLFW_KEY_T)));
				
				drawText(fontBuf, true, "Slim          ", 5, color(conf.isSlim(), isKeyPressed(GLFW_KEY_S)));
				
				drawText(fontBuf, true, "fliP     ", 5, color(conf.isFlipped(), isKeyPressed(GLFW_KEY_P)));
				
				drawText(fontBuf, true, "Full", 5, color(conf.isFull(), isKeyPressed(GLFW_KEY_F)));
				
				drawText(fontBuf, false, "DELETE to clear cache", (CANVAS_HEIGHT/2)-15, color(true, isKeyPressed(GLFW_KEY_DELETE)));
				drawText(fontBuf, true, "SPACE to hide text", (CANVAS_HEIGHT/2)-15, color(true, isKeyPressed(GLFW_KEY_SPACE)));
			}
			
		glPopMatrix();
	}
	
	private boolean isKeyPressed(int key) {
		return glfwGetKey(glfwGetCurrentContext(), key) == GLFW_PRESS;
	}

	private int color(boolean lit, boolean colored) {
		int a;
		if (lit) {
			a = 0xFF000000;
		} else {
			a = 0x55000000;
		}
		return (colored ? 0x00FFFF : 0xFFFFFF) | a;
	}

	private void drawText(ByteBuffer fontBuf, boolean alignRight, String text, int y, int color) {
		fontBuf.rewind();
		
		int amt = STBEasyFont.stb_easy_font_print(0, 0, text, null, fontBuf);
		
		int x = alignRight ? ((CANVAS_WIDTH/2)-STBEasyFont.stb_easy_font_width(text))-5 : 5;
		
		float r = ((color >> 16)&0xFF)/255f;
		float g = ((color >>  8)&0xFF)/255f;
		float b = ((color      )&0xFF)/255f;
		float a = ((color >> 24)&0xFF)/255f;
		
		float darkR = r*0.25f;
		float darkG = g*0.25f;
		float darkB = b*0.25f;
		
		glPushMatrix(); glPushAttrib(GL_ALL_ATTRIB_BITS); {
			glScalef(2, 2, 1);
			glTranslatef(x, y, 1);
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glEnable(GL_DEPTH_TEST);
			glDepthFunc(GL_NOTEQUAL);
			
			glColor4f(darkR, darkG, darkB, a);
			glBegin(GL_QUADS);
			for (int xOfs = -1; xOfs <= 1; xOfs++) {
				for (int yOfs = -1; yOfs <= 1; yOfs++) {
					fontBuf.rewind();
					if (xOfs == 0 && yOfs == 0) {
						continue;
					}
					for (int i = 0; i < amt*4; i++) {
						glVertex2f(fontBuf.getFloat()+xOfs, fontBuf.getFloat()+yOfs);
						fontBuf.position(fontBuf.position()+8);
					}
				}
			}
			glEnd();
			
			glColorMask(true, true, true, false);
			
			glTranslatef(0, 0, 1);
			glColor4f(r, g, b, a);
			fontBuf.rewind();
			glBegin(GL_QUADS); {
				for (int i = 0; i < amt*4; i++) {
					glVertex2f(fontBuf.getFloat(), fontBuf.getFloat());
					fontBuf.position(fontBuf.position()+8);
				}
			} glEnd();
		} glPopAttrib(); glPopMatrix();
	}

	private void setup2D(int width, int height) {
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, width, height, 0, -10, 10);
		glViewport(0, 0, width, height);
		
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
	}

	public void process(Delivery delivery) throws IOException {
		toProcess.addLast(delivery);
	}
	
	private void processDelivery(Delivery delivery) throws Exception {
		BasicProperties props = delivery.getProperties();
		BasicProperties replyProps = new BasicProperties.Builder().correlationId(props.getCorrelationId()).build();
		DataInputStream data = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(delivery.getBody())));
		RenderMode mode = RenderMode.values()[data.readUnsignedByte()];
		int width = data.readUnsignedShort();
		int height = data.readUnsignedShort();
		GameProfile profile = Profiles.readGameProfile(data);
		Map<String, String[]> params = Maps.newHashMap();
		int len = data.readUnsignedShort();
		for (int i = 0; i < len; i++) {
			String key = data.readUTF();
			String[] val = new String[data.readUnsignedByte()];
			for (int v = 0; v < val.length; v++) {
				val[v] = data.readUTF();
			}
			params.put(key, val);
		}
		byte[] skinData = new byte[data.readInt()];
		data.readFully(skinData);
		BufferedImage skin = new PngImage().read(new ByteArrayInputStream(skinData), false);
		Visage.log.info("Received a job to render a "+width+"x"+height+" "+mode.name().toLowerCase()+" for "+(profile == null ? "null" : profile.getName()));
		
		RenderConfiguration conf = new RenderConfiguration(Type.fromMode(mode), Profiles.isSlim(profile), mode.isTall(), Profiles.isFlipped(profile));
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		byte[] pngBys = draw(conf, width, height, profile, skin, params);
		if (Visage.trace) Visage.log.finest("Got png bytes");
		parent.channel.basicPublish("", props.getReplyTo(), replyProps, buildResponse(0, pngBys));
		if (Visage.trace) Visage.log.finest("Published response");
		parent.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
		if (Visage.trace) Visage.log.finest("Ack'd message");
	}

	private byte[] buildResponse(int type, byte[] payload) throws IOException {
		if (Visage.trace) Visage.log.finest("Building response of type "+type);
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		new DataOutputStream(result).writeUTF(parent.name);
		result.write(type);
		result.write(payload);
		byte[] resp = result.toByteArray();
		if (Visage.trace) Visage.log.finest("Built - "+resp.length+" bytes long");
		return resp;
	}

	public byte[] draw(RenderConfiguration conf, int width, int height, GameProfile profile, BufferedImage skin, Map<String, String[]> params) throws Exception {
		//BufferedImage cape;
		BufferedImage out;
		if (skin.getHeight() == 32) {
			if (Visage.debug) Visage.log.finer("Skin is legacy; painting onto new-style canvas");
			BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = canvas.createGraphics();
			g.drawImage(skin, 0, 0, null);
			g.drawImage(flipLimb(skin.getSubimage(0, 16, 16, 16)), 16, 48, null);
			g.drawImage(flipLimb(skin.getSubimage(40, 16, 16, 16)), 32, 48, null);
			g.dispose();
			skin = canvas;
		}
		int color = skin.getRGB(32, 8);
		boolean equal = true;
		for (int x = 32; x < 64; x++) {
			for (int y = 0; y < 16; y++) {
				if (x < 40 && y < 8) continue;
				if (x > 54 && y < 8) continue;
				if (skin.getRGB(x, y) != color) {
					equal = false;
					break;
				}
			}
		}
		if (equal) {
			if (Visage.trace) Visage.log.finest("Skin has solid colored helm, stripping");
			skin.setRGB(32, 0, 32, 16, new int[32*64], 0, 32);
		}
		if (Visage.trace) Visage.log.finest("Got skin");
		if (Visage.trace) Visage.log.finest(conf.toString());
		if (!renderers.containsKey(conf)) {
			renderers.put(conf.copy().lock(), conf.createRenderer(this));
		}
		Renderer renderer = renderers.get(conf);
		try {
			if (Visage.trace) Visage.log.finest("Uploading");
			renderer.setSkin(skin);
			if (Visage.trace) Visage.log.finest("Rendering");
			
			glUseProgram(0);
			glBindFramebuffer(GL_FRAMEBUFFER, fbo);
			glClearColor(0, 0, 0, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			renderer.render(width*SUPERSAMPLING, height*SUPERSAMPLING);
			
			glBindFramebuffer(GL_FRAMEBUFFER, 0);
			
			glDisable(GL_LIGHTING);
			glColor3f(1, 1, 1);
			glDisable(GL_ALPHA_TEST);
			glDisable(GL_CULL_FACE);
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glBindTexture(GL_TEXTURE_2D, fboTex);
			
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			glOrtho(-1, 1, -1, 1, -10, 10);
			glViewport(0, 0, width, height);
			
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			
			glViewport(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
			
			float s = 1f;
			
			glBegin(GL_QUADS); {
				glTexCoord2f(0, 0);
				glVertex2f(-s, -s);
				glTexCoord2f(1, 0);
				glVertex2f(s, -s);
				glTexCoord2f(1, 1);
				glVertex2f(s, s);
				glTexCoord2f(0, 1);
				glVertex2f(-s, s);
			} glEnd();
			glUseProgram(0);
			
			if (!parent.config.getBoolean("continuous")) {
				if (Visage.trace) Visage.log.finest("Rendered - reading pixels");
				out = renderer.readPixels(width, height);
			} else {
				out = null;
			}
		} finally {
			renderer.finish();
			if (Visage.trace) Visage.log.finest("Finished renderer");
		}
		if (out == null) return null;
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(out, "PNG", png);
		if (Visage.trace) Visage.log.finest("Wrote png");
		if (parent.config.getBoolean("pngquant")) {
			Stopwatch sw = null;
			if (Visage.debug) {
				sw = Stopwatch.createStarted();
			}
			Process proc = Runtime.getRuntime().exec(new String[]{"pngquant", "--speed", "2", "-"});
			png.writeTo(proc.getOutputStream());
			ByteStreams.copy(proc.getErrorStream(), System.err);
			byte[] quant = ByteStreams.toByteArray(proc.getInputStream());
			if (Visage.debug) {
				sw.stop();
				int savings = png.size() - quant.length;
				int pct = (int)((((float)savings)/png.size())*100);
				Visage.log.finer("Original: "+png.size()+" / pngquant'd: "+quant.length);
				Visage.log.finer("Saved "+savings+" bytes ("+pct+"%) using pngquant in "+sw); 
			}
			return quant;
		} else {
			return png.toByteArray();
		}
	}

	private void drawQuad(int width, int height) {
		glBegin(GL_QUADS); {
			glTexCoord2f(0, height/CANVAS_HEIGHTf);
			glVertex2f(0, 0);
			glTexCoord2f(width/CANVAS_WIDTHf, height/CANVAS_HEIGHTf);
			glVertex2f(width, 0);
			glTexCoord2f(width/CANVAS_WIDTHf, 0);
			glVertex2f(width, height);
			glTexCoord2f(0, 0);
			glVertex2f(0, height);
		} glEnd();
	}

	private BufferedImage flipLimb(BufferedImage in) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		BufferedImage front = flipHorziontally(in.getSubimage(4, 4, 4, 12));
		BufferedImage back = flipHorziontally(in.getSubimage(12, 4, 4, 12));
		
		BufferedImage top = flipHorziontally(in.getSubimage(4, 0, 4, 4));
		BufferedImage bottom = flipHorziontally(in.getSubimage(8, 0, 4, 4));
		
		BufferedImage left = in.getSubimage(8, 4, 4, 12);
		BufferedImage right = in.getSubimage(0, 4, 4, 12);
		
		Graphics2D g = out.createGraphics();
		g.drawImage(front, 4, 4, null);
		g.drawImage(back, 12, 4, null);
		g.drawImage(top, 4, 0, null);
		g.drawImage(bottom, 8, 0, null);
		g.drawImage(left, 0, 4, null); // left goes to right
		g.drawImage(right, 8, 4, null); // right goes to left
		g.dispose();
		return out;
	}
	
	private BufferedImage flipHorziontally(BufferedImage in) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(in, 0, 0, in.getWidth(), in.getHeight(), in.getWidth(), 0, 0, in.getHeight(), null);
		g.dispose();
		return out;
	}
	
	public void finish() {
		run = false;
		interrupt();
	}

}

