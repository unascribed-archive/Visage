#version 130

uniform sampler2D tex;

in vec2 texCoord;

void main() {
	float threshold = 0.05f;

	float iu = texCoord.x * textureSize(tex, 0).x;
	float iv = texCoord.y * textureSize(tex, 0).y;
	float pu = iu - int(iu);
	float pv = iv - int(iv);
	
	vec4 result = texelFetch(tex, ivec2(iu, iv), 0);
	
	if (pu<threshold || pv<threshold) {
		vec4 nw = texelFetch(tex, ivec2(iu-1, iv-1), 0);
		vec4 ne = texelFetch(tex, ivec2(iu  , iv-1), 0);
		vec4 sw = texelFetch(tex, ivec2(iu-1, iv  ), 0);
		vec4 se = texelFetch(tex, ivec2(iu  , iv  ), 0);
		
		float up = clamp(pu/threshold, 0, 1);
		float uv = clamp(pv/threshold, 0, 1);
		
		vec4 n = mix(nw,ne, up);
		vec4 s = mix(sw,se, up);
		result = mix(n,s, uv);
	}

	gl_FragColor = result;
}