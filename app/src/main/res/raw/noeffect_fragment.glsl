#version 300 es
precision mediump float;

in vec2 outTexPos;
uniform sampler2D sTexture;
out vec4 color;
void main() {
   color = texture(sTexture, outTexPos);
}