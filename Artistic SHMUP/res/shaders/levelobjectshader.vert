#version 330

layout(location = 0)in vec4 in_Position;

uniform mat4 model;
uniform mat4 projection;
uniform mat4 view;

uniform int u_groundblocksizeX, u_groundblocksizeZ;

out vec4 passPosition;

void main(void) {
	passPosition = model * in_Position;
	gl_Position = projection * view * passPosition;
	passPosition.x = mod(passPosition.x, u_groundblocksizeX);
	passPosition.z = mod(passPosition.z, u_groundblocksizeZ);
}