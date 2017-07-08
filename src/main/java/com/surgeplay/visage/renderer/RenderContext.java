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
import org.spacehq.mc.auth.GameProfile;

import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.sixlegs.png.PngImage;
import com.surgeplay.visage.RenderMode;
import com.surgeplay.visage.Visage;
import com.surgeplay.visage.renderer.render.Renderer;
import com.surgeplay.visage.renderer.util.Textures;
import com.surgeplay.visage.util.Profiles;

import static com.surgeplay.visage.renderer.util.Errors.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
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
	
	private static final BufferedImage shadow;
	private static final ByteBuffer areatex;
	private static final ByteBuffer searchtex;
	static {
		try {
			shadow = ImageIO.read(ClassLoader.getSystemResource("shadow.png"));
			byte[] areatexHeap = Resources.toByteArray(ClassLoader.getSystemResource("smaa_area.raw"));
			byte[] searchtexHeap = Resources.toByteArray(ClassLoader.getSystemResource("smaa_search.raw"));
			areatex = BufferUtils.createByteBuffer(areatexHeap.length);
			searchtex = BufferUtils.createByteBuffer(searchtexHeap.length);
			areatex.put(areatexHeap);
			searchtex.put(searchtexHeap);
		} catch (IOException e) {
			throw new InternalError(e);
		}
	}
	
	public int cubeVbo, planeVbo, texture, shadowTexture;
	public int albedoTex, edgeTex, blendTex;
	public int areaTex, searchTex;
	
	public int albedoFbo, edgeFbo, blendFbo;
	public int edgeProgram, blendProgram, neighborhoodProgram;
	
	private static int nextId = 1;
	public VisageRenderer parent;
	private Renderer[] renderers;
	private boolean run = true;
	private BlockingDeque<Delivery> toProcess = new LinkedBlockingDeque<>();
	
	public RenderContext(VisageRenderer parent) {
		super("Render thread #"+(nextId++));
		this.parent = parent;
		RenderMode[] modes = RenderMode.values();
		renderers = new Renderer[modes.length];
		for (int i = 0; i < modes.length; i++) {
			renderers[i] = modes[i].newRenderer(this);
		}
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
			
			long window = glfwCreateWindow(512, 832, "Visage ["+getName()+"]", NULL, NULL);
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
			
			IntBuffer textures = BufferUtils.createIntBuffer(7);
			glGenTextures(textures);
			texture = textures.get();
			shadowTexture = textures.get();
			albedoTex = textures.get();
			edgeTex = textures.get();
			blendTex = textures.get();
			areaTex = textures.get();
			searchTex = textures.get();
			checkGLError();
			
			Textures.upload(shadow, GL_RGBA8, shadowTexture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			checkGLError();
			
			glBindTexture(GL_TEXTURE_2D, areaTex);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RG8, 160, 560, 0, GL_RG, GL_UNSIGNED_BYTE, areatex);
			checkGLError();
			
			glBindTexture(GL_TEXTURE_2D, searchTex);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, 66, 33, 0, GL_RED, GL_UNSIGNED_BYTE, searchtex);
			checkGLError();
			
			setupFboTex(albedoTex);
			setupFboTex(edgeTex);
			setupFboTex(blendTex);
			
			IntBuffer fbos = BufferUtils.createIntBuffer(3);
			glGenFramebuffers(fbos);
			albedoFbo = fbos.get();
			edgeFbo = fbos.get();
			blendFbo = fbos.get();
			
			int albedoDepth = glGenRenderbuffers();
			
			
			glBindFramebuffer(GL_FRAMEBUFFER, albedoFbo);
			glDrawBuffers(GL_COLOR_ATTACHMENT0);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, albedoTex, 0);
			glBindRenderbuffer(GL_RENDERBUFFER, albedoDepth);
			glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, 512, 832);
			glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, albedoDepth);
			checkFramebufferStatus();
			
			glBindFramebuffer(GL_FRAMEBUFFER, edgeFbo);
			glDrawBuffers(GL_COLOR_ATTACHMENT0);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, edgeTex, 0);
			checkFramebufferStatus();
			
			glBindFramebuffer(GL_FRAMEBUFFER, blendFbo);
			glDrawBuffers(GL_COLOR_ATTACHMENT0);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, blendTex, 0);
			checkFramebufferStatus();
			
			
			edgeProgram = glCreateProgram();
			
			createShader(SMAA.edgeVs, SMAA.edgeFs, edgeProgram);
			glUseProgram(edgeProgram);
			glUniform1i(glGetUniformLocation(edgeProgram, "albedo_tex"), 0);
			glUseProgram(0);
			validateShader(edgeProgram);
			checkGLError();
			
			
			blendProgram = glCreateProgram();
			
			createShader(SMAA.blendVs, SMAA.blendFs, blendProgram);
			glUseProgram(blendProgram);
			glUniform1i(glGetUniformLocation(blendProgram, "edge_tex"), 0);
			glUniform1i(glGetUniformLocation(blendProgram, "area_tex"), 1);
			glUniform1i(glGetUniformLocation(blendProgram, "search_tex"), 2);
			glUseProgram(0);
			validateShader(blendProgram);
			checkGLError();
			
			neighborhoodProgram = glCreateProgram();
			
			createShader(SMAA.neighborhoodVs, SMAA.neighborhoodFs, neighborhoodProgram);
			glUseProgram(neighborhoodProgram);
			glUniform1i(glGetUniformLocation(neighborhoodProgram, "albedo_tex"), 0);
			glUniform1i(glGetUniformLocation(neighborhoodProgram, "blend_tex"), 1);
			glUseProgram(0);
			validateShader(neighborhoodProgram);
			checkGLError();
			
			
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
					glClearColor(0.4f, 0.4f, 0.4f, 1);
					
					BufferedImage alex = ImageIO.read(ClassLoader.getSystemResource("alex.png"));
					BufferedImage steve = ImageIO.read(ClassLoader.getSystemResource("steve.png"));
					BufferedImage test = ImageIO.read(ClassLoader.getSystemResource("test_skin.png"));
					
					ByteBuffer fontBuffer = BufferUtils.createByteBuffer(276480);
					
					RenderMode[] modes = {RenderMode.FACE, RenderMode.FRONT, RenderMode.FRONTFULL, RenderMode.BUST, RenderMode.FULL};
					BufferedImage[] skins = {alex, steve, test, test};
					String[] names = {"alex", "steve", "test (slim)", "test"};
					
					int[] skin = {0};
					int[] mode = {4};
					
					glfwSetKeyCallback(window, new GLFWKeyCallback() {
						@Override
						public void invoke(long window, int key, int scancode, int action, int mods) {
							if (action == GLFW_PRESS) {
								if (key == GLFW_KEY_RIGHT) {
									mode[0] = (mode[0]+1)%modes.length;
								} else if (key == GLFW_KEY_LEFT) {
									mode[0] = mode[0]-1;
									if (mode[0] < 0) {
										mode[0] = modes.length+mode[0];
									}
								} else if (key == GLFW_KEY_UP) {
									skin[0] = (skin[0]+1)%skins.length;
								} else if (key == GLFW_KEY_DOWN) {
									skin[0] = skin[0]-1;
									if (skin[0] < 0) {
										skin[0] = skins.length+skin[0];
									}
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
					
					while (!glfwWindowShouldClose(window)) {
						glfwPollEvents();
						
						drawContinuous(skins[skin[0]], pattern, fontBuffer, (skin[0] % 2 == 0) ? modes[mode[0]].slim() : modes[mode[0]], modes[mode[0]].name().toLowerCase()+" - "+names[skin[0]]);
						
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
				for (Renderer r : renderers) {
					if (r != null) {
						r.destroy();
					}
				}
			} catch (Exception e) {
				Visage.log.log(Level.SEVERE, "A fatal error has occurred in the render thread run loop.", e);
			}
		} catch (Exception e) {
			Visage.log.log(Level.SEVERE, "A fatal error has occurred while setting up a render thread.", e);
		}
	}
	
	private void setupFboTex(int tex) {
		glBindTexture(GL_TEXTURE_2D, tex);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 512, 832, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
	}

	private void createShader(String vertexText, String fragText, int program) {
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, vertexText);
		glCompileShader(vs);
		
		System.out.println(glGetShaderInfoLog(vs));
		
		glAttachShader(program, vs);
		glDeleteShader(vs);
		
		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, fragText);
		glCompileShader(fs);
		
		System.out.println(glGetShaderInfoLog(fs).trim());
		
		glAttachShader(program, fs);
		glDeleteShader(fs);
		
		glLinkProgram(program);
	}
	
	private void validateShader(int program) {
		glValidateProgram(program);
		System.out.println(glGetProgramInfoLog(program).trim());
	}

	private void drawContinuous(BufferedImage skin, ByteBuffer pattern, ByteBuffer fontBuf, RenderMode mode, String modeName) throws Exception {
		glColor3f(1, 1, 1);
		int h = 512;
		if (mode .isTall()) {
			h = 832;
		}
		
		glFrontFace(GL_CCW);
		glClearColor(0.4f, 0.4f, 0.4f, 1);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glPolygonStipple(pattern);
		glEnable(GL_POLYGON_STIPPLE);
		glColor3f(0.6f, 0.6f, 0.6f);
		glBegin(GL_QUADS); {
			glVertex2f(0, mode.isTall() ? 0 : 320);
			glVertex2f(512, mode.isTall() ? 0 : 320);
			glVertex2f(512, 832);
			glVertex2f(0, 832);
		} glEnd();
		glDisable(GL_POLYGON_STIPPLE);
		
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glColor3f(1, 1, 1);
		
		glPushMatrix();
			draw(mode, 512, h, new GameProfile(new UUID(0L, 0L), "continuous_test"), skin, Collections.emptyMap());
		glPopMatrix();
		
		glPushMatrix();
			setup2D(512, 832);
			
			glDisable(GL_LIGHTING);
			glDisable(GL_DEPTH_TEST);
			glDisable(GL_CULL_FACE);
			glDisable(GL_TEXTURE_2D);
			
			glScalef(2, 2, 1);
			
			fontBuf.rewind();
			int amt = STBEasyFont.stb_easy_font_print(5, 5, "visage v"+Visage.VERSION+" - "+modeName+"\n< - prev mode | > - next mode\n^ - prev skin | v - next skin", null, fontBuf);
			
			glColor3f(1, 1, 1);
			glBegin(GL_QUADS);
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					fontBuf.rewind();
					if (x == 0 && y == 0) {
						continue;
					}
					for (int i = 0; i < amt*4; i++) {
						glVertex2f(fontBuf.getFloat()+x, fontBuf.getFloat()+y);
						fontBuf.position(fontBuf.position()+8);
					}
				}
			}
			glEnd();
			
			glTranslatef(0, 0, 1);
			glColor3f(0, 0, 0);
			fontBuf.rewind();
			glBegin(GL_QUADS);
			for (int i = 0; i < amt*4; i++) {
				glVertex2f(fontBuf.getFloat(), fontBuf.getFloat());
				fontBuf.position(fontBuf.position()+8);
			}
			glEnd();
			
			
			
		glPopMatrix();
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
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		byte[] pngBys = draw(mode, width, height, profile, skin, params);
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

	public byte[] draw(RenderMode mode, int width, int height, GameProfile profile, BufferedImage skin, Map<String, String[]> params) throws Exception {
		if (Profiles.isSlim(profile)) {
			mode = mode.slim();
		}
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
		if (Visage.trace) Visage.log.finest(mode.name());
		switch (mode) {
			case SKIN:
				out = skin;
				break;
			default: {
				Renderer renderer = renderers[mode.ordinal()];
				if (!renderer.isInitialized()) {
					if (Visage.trace) Visage.log.finest("Initialized renderer");
					renderer.init();
				}
				try {
					if (Visage.trace) Visage.log.finest("Uploading");
					renderer.setSkin(skin);
					if (Visage.trace) Visage.log.finest("Rendering");
					
					// albedo
					glUseProgram(0);
					glBindFramebuffer(GL_FRAMEBUFFER, albedoFbo);
					glClearColor(0, 0, 0, 0);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
					
					glEnable(GL_CULL_FACE);
					glActiveTexture(GL_TEXTURE0);
					renderer.render(width, height);
					glDisable(GL_CULL_FACE);
					glDisable(GL_LIGHTING);
					glDisable(GL_DEPTH_TEST);
					glDisable(GL_ALPHA_TEST);
					glEnable(GL_BLEND);
					glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
					
					glUseProgram(0);
					glBindFramebuffer(GL_FRAMEBUFFER, 0);
					
					
					// edge
					glBindFramebuffer(GL_FRAMEBUFFER, edgeFbo);
					glClearColor(0, 0, 0, 0);
					glClear(GL_COLOR_BUFFER_BIT);
					
					glUseProgram(edgeProgram);
					
					glActiveTexture(GL_TEXTURE0);
					glBindTexture(GL_TEXTURE_2D, albedoTex);
					
					setup2D(width, height);
					drawQuad(width, height);
					
					glUseProgram(0);
					System.out.print(glGetProgramInfoLog(edgeProgram).trim());
					glBindFramebuffer(GL_FRAMEBUFFER, 0);
					
					
					// blend
					glBindFramebuffer(GL_FRAMEBUFFER, blendFbo);
					glClearColor(0, 0, 0, 0);
					glClear(GL_COLOR_BUFFER_BIT);
					
					glUseProgram(blendProgram);
					
					glActiveTexture(GL_TEXTURE0);
					glBindTexture(GL_TEXTURE_2D, edgeTex);
					
					glActiveTexture(GL_TEXTURE1);
					glBindTexture(GL_TEXTURE_2D, areaTex);
					
					glActiveTexture(GL_TEXTURE2);
					glBindTexture(GL_TEXTURE_2D, searchTex);
					
					setup2D(width, height);
					drawQuad(width, height);
					
					glUseProgram(0);
					System.out.print(glGetProgramInfoLog(blendProgram).trim());
					glBindFramebuffer(GL_FRAMEBUFFER, 0);
					
					
					// neighborhood
					glUseProgram(neighborhoodProgram);
					
					glClearColor(0, 0, 0, 0);
					glClear(GL_COLOR_BUFFER_BIT);
					glDisable(GL_DEPTH_TEST);
					
					glActiveTexture(GL_TEXTURE0);
					glBindTexture(GL_TEXTURE_2D, albedoTex);
					
					glActiveTexture(GL_TEXTURE1);
					glBindTexture(GL_TEXTURE_2D, blendTex);
					
					setup2D(width, height);
					drawQuad(width, height);
					
					glUseProgram(0);
					System.out.print(glGetProgramInfoLog(neighborhoodProgram).trim());
					glActiveTexture(GL_TEXTURE0);
					
					
					
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
				break;
			}
		}
		if (out == null) return null;
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(out, "PNG", png);
		if (Visage.trace) Visage.log.finest("Wrote png");
		return png.toByteArray();
	}

	private void drawQuad(int width, int height) {
		glBegin(GL_QUADS); {
			glTexCoord2f(0, height/832f);
			glVertex2f(0, 0);
			glTexCoord2f(width/512f, height/832f);
			glVertex2f(width, 0);
			glTexCoord2f(width/512f, 0);
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

