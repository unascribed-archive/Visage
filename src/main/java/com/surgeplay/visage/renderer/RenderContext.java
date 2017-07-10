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
	private static final int CANVAS_HEIGHT = 832;
	
	private static final int SKIN_SUPERSAMPLING = 16;
	
	private static final BufferedImage shadow;
	private static final BufferedImage skinUnderlay;
	static {
		try {
			shadow = ImageIO.read(ClassLoader.getSystemResource("shadow.png"));
			skinUnderlay = ImageIO.read(ClassLoader.getSystemResource("skin_underlay.png"));
		} catch (IOException e) {
			throw new InternalError(e);
		}
	}
	
	public int cubeVbo, planeVbo, skinTexture, shadowTexture, skinUnderlayTexture;
	
	public int fbo, fbo2, swapFbo, swapFboTex;
	public int skinFbo, skinFboTex;
	
	public int renderPass;
	
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
			if (!GL.getCapabilities().GL_ARB_texture_multisample) {
				throw new RuntimeException("ARB_texture_multisample is required");
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
			
			IntBuffer textures = BufferUtils.createIntBuffer(5);
			glGenTextures(textures);
			skinTexture = textures.get();
			shadowTexture = textures.get();
			skinFboTex = textures.get();
			skinUnderlayTexture = textures.get();
			swapFboTex = textures.get();
			checkGLError();
			
			glBindTexture(GL_TEXTURE_2D, skinTexture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			checkGLError();
			
			Textures.upload(shadow, GL_RGBA8, shadowTexture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			checkGLError();
			
			Textures.upload(skinUnderlay, GL_RGBA8, skinUnderlayTexture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			checkGLError();
			
			glBindTexture(GL_TEXTURE_2D, skinFboTex);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 64*SKIN_SUPERSAMPLING, 64*SKIN_SUPERSAMPLING, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			checkGLError();
			
			glBindTexture(GL_TEXTURE_2D, swapFboTex);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, CANVAS_WIDTH, CANVAS_HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			checkGLError();
			
			fbo = glGenFramebuffers();
			
			int depth = glGenRenderbuffers();
			
			int color = glGenRenderbuffers();
			
			glBindFramebuffer(GL_FRAMEBUFFER, fbo);
			glDrawBuffer(GL_COLOR_ATTACHMENT0);
			
			glBindRenderbuffer(GL_RENDERBUFFER, depth);
			glRenderbufferStorageMultisample(GL_RENDERBUFFER, 8, GL_DEPTH_COMPONENT24, CANVAS_WIDTH, CANVAS_HEIGHT);
			
			glBindRenderbuffer(GL_RENDERBUFFER, color);
			glRenderbufferStorageMultisample(GL_RENDERBUFFER, 8, GL_RGBA8, CANVAS_WIDTH, CANVAS_HEIGHT);
			
			glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depth);
			glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, color);
			checkFramebufferStatus();
			
			
			fbo2 = glGenFramebuffers();
			
			int depth2 = glGenRenderbuffers();
			int color2 = glGenRenderbuffers();
			
			glBindFramebuffer(GL_FRAMEBUFFER, fbo2);
			glDrawBuffer(GL_COLOR_ATTACHMENT0);
			
			glBindRenderbuffer(GL_RENDERBUFFER, depth2);
			glRenderbufferStorageMultisample(GL_RENDERBUFFER, 8, GL_DEPTH_COMPONENT24, CANVAS_WIDTH, CANVAS_HEIGHT);
			
			glBindRenderbuffer(GL_RENDERBUFFER, color2);
			glRenderbufferStorageMultisample(GL_RENDERBUFFER, 8, GL_RGBA8, CANVAS_WIDTH, CANVAS_HEIGHT);
			
			glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depth2);
			glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, color2);
			checkFramebufferStatus();
			
			
			skinFbo = glGenFramebuffers();
			
			glBindFramebuffer(GL_FRAMEBUFFER, skinFbo);
			glDrawBuffer(GL_COLOR_ATTACHMENT0);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, skinFboTex, 0);
			checkFramebufferStatus();
			
			glBindFramebuffer(GL_FRAMEBUFFER, 0);
			
			
			swapFbo = glGenFramebuffers();
			
			glBindFramebuffer(GL_FRAMEBUFFER, swapFbo);
			glDrawBuffer(GL_COLOR_ATTACHMENT0);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, swapFboTex, 0);
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
							.put("X-Test-Legacy", ImageIO.read(ClassLoader.getSystemResource("test_legacy_skin.png")))
							.put("Falkreon", ImageIO.read(URI.create("https://visage.surgeplay.com/skin/Falkreon").toURL()))
							.put("unascribed", ImageIO.read(URI.create("https://visage.surgeplay.com/skin/unascribed").toURL()))
							.put("Dinnerbone", ImageIO.read(URI.create("https://visage.surgeplay.com/skin/Dinnerbone").toURL()))
							.build();
					
					ByteBuffer fontBuffer = BufferUtils.createByteBuffer(276480);
					
					RenderConfiguration conf = new RenderConfiguration(Type.BODY, false, true, false);
					String[] skinKeys = skins.keySet().toArray(new String[skins.size()]);
					
					String[] bgs = {
							"Light Checkers",
							"Mid Checkers",
							"Dark Checkers",
							"Black",
							"White",
							"Magenta"
					};
					
					boolean[] showText = {true};
					int[] skinIdx = {1};
					int[] bgIdx = {1};
					boolean[] skinOnly = {false};
					boolean[] fbos = {true, true};
					
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
								} else if (key == GLFW_KEY_G) {
									bgIdx[0] = (bgIdx[0]+1)%bgs.length;
								} else if (key == GLFW_KEY_N) {
									skinOnly[0] = !skinOnly[0];
								} else if (key == GLFW_KEY_1) {
									fbos[0] = !fbos[0];
								} else if (key == GLFW_KEY_2) {
									fbos[1] = !fbos[1];
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
						drawContinuous(skins.get(skinKeys[skinIdx[0]]), skinKeys[skinIdx[0]], bgs[bgIdx[0]], conf, pattern, fontBuffer, showText[0], skinOnly[0], fbos[0], fbos[1]);
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
	private void drawContinuous(BufferedImage skin, String skinName, String bg, RenderConfiguration conf, ByteBuffer pattern, ByteBuffer fontBuf, boolean showText, boolean skinOnly, boolean fbo1Enabled, boolean fbo2Enabled) throws Exception {
		glColor3f(1, 1, 1);
		int h = CANVAS_WIDTH;
		if (conf.isFull()) {
			h = CANVAS_HEIGHT;
		}
		
		glFrontFace(GL_CCW);
		if (bg.endsWith("Checkers")) {
			float bgTone = 0;
			float fgTone = 0;
			if (bg.equals("Light Checkers")) {
				bgTone = 0.8f;
				fgTone = 1.0f;
			} else if (bg.equals("Mid Checkers")) {
				bgTone = 0.4f;
				fgTone = 0.6f;
			} else if (bg.equals("Dark Checkers")) {
				bgTone = 0.0f;
				fgTone = 0.2f;
			}
			glClearColor(bgTone, bgTone, bgTone, 1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glPolygonStipple(pattern);
			glEnable(GL_POLYGON_STIPPLE);
			glColor3f(fgTone, fgTone, fgTone);
			if (skinOnly) {
				int x = (CANVAS_WIDTH-384)/2;
				int y = (CANVAS_HEIGHT-384)/2;
				drawQuad(x, y, x+384, y+384);
			} else {
				glBegin(GL_QUADS); {
					glVertex2f(0, conf.isFull() ? 0 : CANVAS_HEIGHT-CANVAS_WIDTH);
					glVertex2f(CANVAS_WIDTH, conf.isFull() ? 0 : CANVAS_HEIGHT-CANVAS_WIDTH);
					glVertex2f(CANVAS_WIDTH, CANVAS_HEIGHT);
					glVertex2f(0, CANVAS_HEIGHT);
				} glEnd();
			}
			glDisable(GL_POLYGON_STIPPLE);
		} else if (bg.equals("Black")) {
			glClearColor(0, 0, 0, 1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		} else if (bg.equals("White")) {
			glClearColor(1, 1, 1, 1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		} else if (bg.equals("Magenta")) {
			glClearColor(1, 0, 1, 1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		}
		
		if (isKeyPressed(GLFW_KEY_DELETE)) {
			renderers.values().forEach(Renderer::destroy);
			renderers.clear();
		}
		
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glColor3f(1, 1, 1);
		
		glPushMatrix();
			draw(conf, CANVAS_WIDTH, h, new GameProfile(new UUID(0L, 0L), "continuous_test"), skin, Collections.emptyMap());
			glBindFramebuffer(GL_FRAMEBUFFER, 0);
			
			glDisable(GL_LIGHTING);
			glColor3f(1, 1, 1);
			glDisable(GL_ALPHA_TEST);
			glDisable(GL_CULL_FACE);
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			glOrtho(0, CANVAS_WIDTH, 0, CANVAS_HEIGHT, -10, 10);
			glViewport(0, 0, CANVAS_WIDTH, h);
			
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			
			glViewport(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
			
			if (skinOnly) {
				glBindTexture(GL_TEXTURE_2D, skinFboTex);
				int x = (CANVAS_WIDTH-384)/2;
				int y = (CANVAS_HEIGHT-384)/2;
				drawQuad(x, y, x+384, y+384, 0, 1, 1, 0);
			} else {
				if (fbo1Enabled) {
					glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
					glBindFramebuffer(GL_DRAW_FRAMEBUFFER, swapFbo);
					glBlitFramebuffer(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, GL_COLOR_BUFFER_BIT, GL_LINEAR);
					
					glBindFramebuffer(GL_FRAMEBUFFER, 0);
					glEnable(GL_BLEND);
					glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
					glBindTexture(GL_TEXTURE_2D, swapFboTex);
					drawQuad(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
				}
				
				if (fbo2Enabled) {
					glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo2);
					glBindFramebuffer(GL_DRAW_FRAMEBUFFER, swapFbo);
					glBlitFramebuffer(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, GL_COLOR_BUFFER_BIT, GL_LINEAR);
					
					glBindFramebuffer(GL_FRAMEBUFFER, 0);
					glEnable(GL_BLEND);
					glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
					glBindTexture(GL_TEXTURE_2D, swapFboTex);
					drawQuad(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
				}
			}
		glPopMatrix();
		
		glPushMatrix();
			setup2D(CANVAS_WIDTH, CANVAS_HEIGHT);
			
			glDisable(GL_LIGHTING);
			glDisable(GL_DEPTH_TEST);
			glDisable(GL_CULL_FACE);
			glDisable(GL_TEXTURE_2D);
			
			
			if (showText) {
				drawText(fontBuf, false, "backGround: "+bg, 5, color(true, isKeyPressed(GLFW_KEY_G)));
				drawText(fontBuf, false, "sKin: "+skinName, 15, color(true, isKeyPressed(GLFW_KEY_K)));
				
				if (!skinOnly) {
					drawText(fontBuf, false, "Type: "+conf.getType(), 25, color(true, isKeyPressed(GLFW_KEY_T)));
					drawText(fontBuf, true, "Slim          ", 5, color(conf.isSlim(), isKeyPressed(GLFW_KEY_S)));
					
					drawText(fontBuf, true, "fliP     ", 5, color(conf.isFlipped(), isKeyPressed(GLFW_KEY_P)));
					
					drawText(fontBuf, true, "Full", 5, color(conf.isFull(), isKeyPressed(GLFW_KEY_F)));
					drawText(fontBuf, true, "skiN only", 15, color(skinOnly, isKeyPressed(GLFW_KEY_N)));
					drawText(fontBuf, true, "1  ", 25, color(fbo1Enabled, isKeyPressed(GLFW_KEY_1)));
					drawText(fontBuf, true, "2", 25, color(fbo2Enabled, isKeyPressed(GLFW_KEY_2)));
					
					drawText(fontBuf, false, "DELETE to clear cache", (CANVAS_HEIGHT/2)-15, color(true, isKeyPressed(GLFW_KEY_DELETE)));
				} else {
					drawText(fontBuf, true, "skiN only", 5, color(skinOnly, isKeyPressed(GLFW_KEY_N)));
				}
				
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
		
		glClearColor(0, 0, 0, 0);
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
			Textures.upload(skin, GL_RGBA8, skinTexture);
			if (Visage.trace) Visage.log.finest("Rendering");
			
			glBindFramebuffer(GL_FRAMEBUFFER, skinFbo);
			glClearColor(0, 0, 0, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			glViewport(0, 0, 64*SKIN_SUPERSAMPLING, 64*SKIN_SUPERSAMPLING);
			glOrtho(0, 64, 0, 64, -1, 1);
			
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			
			glEnable(GL_TEXTURE_2D);
			
			glDisable(GL_LIGHTING);
			glColor3f(1, 1, 1);
			glDisable(GL_ALPHA_TEST);
			glDisable(GL_CULL_FACE);
			
			glBindTexture(GL_TEXTURE_2D, skinUnderlayTexture);
			drawQuad(0, 0, 64, 64);
			
			glBindTexture(GL_TEXTURE_2D, skinTexture);
			if (skin.getHeight() == 32) {
				drawQuad(0, 0, 64, 32);
				drawFlippedLimb(16, 48, 0, 16);
				drawFlippedLimb(32, 48, 40, 16);
			} else {
				drawQuad(0, 0, 64, 64);
			}
			
			
			
			glBindFramebuffer(GL_FRAMEBUFFER, fbo);
			
			glClearColor(0, 0, 0, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			renderPass = 1;
			renderer.render(width, height);
			
			glBindFramebuffer(GL_FRAMEBUFFER, fbo2);
			glClearColor(0, 0, 0, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo2);
			glBlitFramebuffer(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
			
			glBindFramebuffer(GL_FRAMEBUFFER, fbo2);
			
			renderPass = 2;
			renderer.render(width, height);
			
			
			
			if (!parent.config.getBoolean("continuous")) {
				if (Visage.trace) Visage.log.finest("Rendered - reading pixels");
				glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
				glBindFramebuffer(GL_DRAW_FRAMEBUFFER, swapFbo);
				glBlitFramebuffer(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, GL_COLOR_BUFFER_BIT, GL_LINEAR);
				
				glBindFramebuffer(GL_FRAMEBUFFER, 0);
				glEnable(GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				glBindTexture(GL_TEXTURE_2D, swapFboTex);
				drawQuad(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
			
				glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo2);
				glBindFramebuffer(GL_DRAW_FRAMEBUFFER, swapFbo);
				glBlitFramebuffer(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, GL_COLOR_BUFFER_BIT, GL_LINEAR);
				
				glBindFramebuffer(GL_FRAMEBUFFER, 0);
				glEnable(GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				glBindTexture(GL_TEXTURE_2D, swapFboTex);
				drawQuad(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
				
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

	private void drawFlippedLimb(int x, int y, int u, int v) {
		drawFlippedSkinQuad(x+4, y+4, u+4, v+4, 4, 12);
		drawFlippedSkinQuad(x+12, y+4, u+12, v+4, 4, 12);
		
		drawFlippedSkinQuad(x+4, y+0, u+4, v+0, 4, 4);
		drawFlippedSkinQuad(x+8, y+0, u+8, v+0, 4, 4);
		
		drawSkinQuad(x+0, y+4, u+8, v+4, 4, 12);
		drawSkinQuad(x+8, y+4, u+0, v+4, 4, 12);
	}

	private void drawFlippedSkinQuad(int x, int y, int u, int v, int w, int h) {
		drawQuad(x, y, x+w, y+h, (u+w)/64f, (v)/32f, (u)/64f, (v+h)/32f);
	}
	
	private void drawSkinQuad(int x, int y, int u, int v, int w, int h) {
		drawQuad(x, y, x+w, y+h, (u)/64f, (v)/32f, (u+w)/64f, (v+h)/32f);
	}

	private void drawQuad(float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2) {
		glBegin(GL_QUADS); {
			glTexCoord2f(u1, v1);
			glVertex2f(x1, y1);
			glTexCoord2f(u2, v1);
			glVertex2f(x2, y1);
			glTexCoord2f(u2, v2);
			glVertex2f(x2, y2);
			glTexCoord2f(u1, v2);
			glVertex2f(x1, y2);
		} glEnd();
	}
	
	private void drawQuad(float x1, float y1, float x2, float y2) {
		drawQuad(x1, y1, x2, y2, 0, 0, 1, 1);
	}
	
	public void finish() {
		run = false;
		interrupt();
	}

}

