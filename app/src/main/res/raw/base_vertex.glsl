#version 300 es
layout(location = 0) in vec2 pos;
layout(location = 1) in vec2 texPos;
out vec2 textureCoordinate;
uniform mat4 textureMatrix;
uniform float filpY;
void main() {
   gl_Position = vec4(pos, 0, 1);
   vec4 texTranformPos = textureMatrix * vec4(texPos, 0, 1);
   texTranformPos.y = filpY * (1.0f - texTranformPos.y) + (1.0f - filpY) * (texTranformPos.y);
   textureCoordinate = vec2(texTranformPos.x, texTranformPos.y);
}