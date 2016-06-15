#version 330

in vec2 passTexCoord;

uniform sampler2D u_texture;
uniform vec4 u_color;

out vec4 out_Color;

void main(void) {
	out_Color = vec4(u_color.rgb, texture(u_texture, passTexCoord).a);
}