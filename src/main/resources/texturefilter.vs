#version 130

out vec2 texCoord;

void main() {
	gl_TexCoord[0] = vec4(gl_MultiTexCoord0.xy,0,0);
	texCoord = gl_MultiTexCoord0.xy;
	gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
}