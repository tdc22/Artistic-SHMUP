#version 330

in vec4 passPosition;

uniform sampler2D u_texture;
uniform int u_levelsizeX, u_levelsizeZ;

out vec4 out_Color;

void main(void) {
	vec2 texCoord = vec2(passPosition.x / u_levelsizeX, 1 - passPosition.z / u_levelsizeZ);
	out_Color = vec4(texture(u_texture, texCoord));
}