package com.surgeplay.visage.renderer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class SMAA {

	public static final String smaa;
	
	static {
		try {
			smaa = Resources.toString(ClassLoader.getSystemResource("smaa.glsl"), Charsets.UTF_8)+"\n\n";
		} catch (Exception e) {
			throw new InternalError(e);
		}
	}
	
	public static final String headerVs =
			  "#version 130\n"
			+ "#ifndef SMAA_PIXEL_SIZE\n"
			+ "#define SMAA_PIXEL_SIZE vec2(1.0 / 512.0, 1.0 / 832.0)\n"
			+ "#endif\n"
			+ "#define SMAA_PRESET_ULTRA 1\n"
			+ "#define SMAA_GLSL_3 1\n"
			+ "#define SMAA_ONLY_COMPILE_VS 1\n"
			+ smaa;
	
	public static final String headerFs =
			  "#version 130\n"
			+ "#ifndef SMAA_PIXEL_SIZE\n"
			+ "#define SMAA_PIXEL_SIZE vec2(1.0 / 512.0, 1.0 / 832.0)\n"
			+ "#endif\n"
			+ "#define SMAA_PRESET_ULTRA 1\n"
			+ "#define SMAA_GLSL_3 1\n"
			+ "#define SMAA_ONLY_COMPILE_PS 1\n"
			+ smaa;
	
	
	
	public static final String edgeVs =
			  headerVs
			+ "out vec2 texcoord;\n"
			+ "out vec4 offset[3];\n"
			+ "out vec4 dummy2;\n"
			+ "void main() {\n"
			+ "    texcoord = gl_MultiTexCoord0.xy;\n"
			+ "    vec4 dummy1 = vec4(0);\n"
			+ "    SMAAEdgeDetectionVS(dummy1, dummy2, texcoord, offset);\n"
			+ "    gl_Position = ftransform();\n"
			+ "}";
	
	public static final String edgeFs =
			  headerFs
			+ "uniform sampler2D albedo_tex;\n"
			+ "in vec2 texcoord;\n"
			+ "in vec4 offset[3];\n"
			+ "in vec4 dummy2;\n"
			+ "void main() {\n"
			+ "    #if SMAA_PREDICATION == 1\n"
			+ "        gl_FragColor = SMAAColorEdgeDetectionPS(texcoord, offset, albedo_tex, depthTex);\n"
			+ "    #else\n"
			+ "        gl_FragColor = SMAAColorEdgeDetectionPS(texcoord, offset, albedo_tex);\n"
			+ "    #endif\n"
			+ "}";
	
	
	
	public static final String blendVs =
			  headerVs
			+ "out vec2 texcoord;\n"
			+ "out vec2 pixcoord;\n"
			+ "out vec4 offset[3];\n"
			+ "out vec4 dummy2;\n"
			+ "void main() {\n"
			+ "    texcoord = gl_MultiTexCoord0.xy;\n"
			+ "    vec4 dummy1 = vec4(0);\n"
			+ "    SMAABlendingWeightCalculationVS(dummy1, dummy2, texcoord, pixcoord, offset);"
			+ "    gl_Position = ftransform();"
			+ "}";
	
	public static final String blendFs =
			  headerFs
			+ "uniform sampler2D edge_tex;\n"
			+ "uniform sampler2D area_tex;\n"
			+ "uniform sampler2D search_tex;\n"
			+ "in vec2 texcoord;\n"
			+ "in vec2 pixcoord;\n"
			+ "in vec4 offset[3];\n"
			+ "in vec4 dummy2;\n"
			+ "void main() {\n"
			+ "    gl_FragColor = SMAABlendingWeightCalculationPS(texcoord, pixcoord, offset, edge_tex, area_tex, search_tex, ivec4(0));\n"
			+ "}";
	
	
	
	public static final String neighborhoodVs =
			  headerVs
			+ "out vec2 texcoord;\n"
			+ "out vec4 offset[2];\n"
			+ "out vec4 dummy2;\n"
			+ "void main() {\n"
			+ "    texcoord = gl_MultiTexCoord0.xy;\n"
			+ "    vec4 dummy1 = vec4(0);\n"
			+ "    SMAANeighborhoodBlendingVS(dummy1, dummy2, texcoord, offset);\n"
			+ "    gl_Position = ftransform();\n"
			+ "}";
	
	public static final String neighborhoodFs =
			  headerFs
			+ "uniform sampler2D albedo_tex;\n"
			+ "uniform sampler2D blend_tex;\n"
			+ "in vec2 texcoord;\n"
			+ "in vec4 offset[2];\n"
			+ "in vec4 dummy2;\n"
			+ "void main() {\n"
			+ "    gl_FragColor = SMAANeighborhoodBlendingPS(texcoord, offset, albedo_tex, blend_tex);\n"
			+ "}";
}
