#version 300 es
precision mediump float;

in vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
out vec4 color;
void main() {
   color = texture(inputImageTexture, textureCoordinate);
}