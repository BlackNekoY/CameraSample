#version 300 es 
precision mediump float;

in vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
out vec4 color;
void main() {
   vec4 currColor = texture(inputImageTexture, textureCoordinate);
   float grey = 0.3f * currColor.r + 0.59f * currColor.g + 0.11f * currColor.b;
   color = vec4(grey, grey, grey, 1);
}