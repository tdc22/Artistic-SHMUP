#version 330

in vec4 passPosition;

uniform sampler2D u_texture;
uniform int u_levelsizeX, u_levelsizeZ;

out vec4 out_Color;

void main(void) {
	vec2 texCoord = vec2(passPosition.x / u_levelsizeX, 1 - passPosition.z / u_levelsizeZ);
	vec4 texColor = vec4(texture(u_texture, texCoord));
	vec3 bottomColor = vec3(0.75, 0.75, 0.75);
	float mixcolor = min(0, -passPosition.y);
	
	out_Color = texColor;
	if(passPosition.y < 0)
		out_Color.rgb = sqrt(mix(texColor.rgb * texColor.rgb, bottomColor * bottomColor, -passPosition.y/2));
}