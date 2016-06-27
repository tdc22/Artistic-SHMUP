#version 330

in vec2 passTexCoord;
in float passRelativeLife;

uniform sampler2D u_texture;

out vec4 out_Color;

void main(void) {
	out_Color = vec4(1, 1, 1, 1);
	if(passTexCoord.x <= passRelativeLife)
		out_Color = vec4(1, 0, 0, 1);
}