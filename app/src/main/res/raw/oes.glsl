#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

in vec2 outTexPos;
uniform samplerExternalOES sTexture;
out vec4 color;
void main() {
   color = texture(sTexture, outTexPos);
}