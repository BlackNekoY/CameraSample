#version 300 es
precision mediump float;

in vec2 outTexPos;
uniform sampler2D sTexture;
uniform vec2 singleStepOffset;
uniform mediump float params;
out vec4 color;

const highp vec3 W = vec3(0.299,0.587,0.114);
vec2 blurCoordinates[20];

float hardLight(float color)
{
    if(color <= 0.5)
    color = color * color * 2.0;
    else
    color = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);
    return color;
}

void main(){
    vec3 centralColor = texture(sTexture, outTexPos).rgb;
    blurCoordinates[0] = outTexPos.xy + singleStepOffset * vec2(0.0, -10.0);
    blurCoordinates[1] = outTexPos.xy + singleStepOffset * vec2(0.0, 10.0);
    blurCoordinates[2] = outTexPos.xy + singleStepOffset * vec2(-10.0, 0.0);
    blurCoordinates[3] = outTexPos.xy + singleStepOffset * vec2(10.0, 0.0);
    blurCoordinates[4] = outTexPos.xy + singleStepOffset * vec2(5.0, -8.0);
    blurCoordinates[5] = outTexPos.xy + singleStepOffset * vec2(5.0, 8.0);
    blurCoordinates[6] = outTexPos.xy + singleStepOffset * vec2(-5.0, 8.0);
    blurCoordinates[7] = outTexPos.xy + singleStepOffset * vec2(-5.0, -8.0);
    blurCoordinates[8] = outTexPos.xy + singleStepOffset * vec2(8.0, -5.0);
    blurCoordinates[9] = outTexPos.xy + singleStepOffset * vec2(8.0, 5.0);
    blurCoordinates[10] = outTexPos.xy + singleStepOffset * vec2(-8.0, 5.0);
    blurCoordinates[11] = outTexPos.xy + singleStepOffset * vec2(-8.0, -5.0);
    blurCoordinates[12] = outTexPos.xy + singleStepOffset * vec2(0.0, -6.0);
    blurCoordinates[13] = outTexPos.xy + singleStepOffset * vec2(0.0, 6.0);
    blurCoordinates[14] = outTexPos.xy + singleStepOffset * vec2(6.0, 0.0);
    blurCoordinates[15] = outTexPos.xy + singleStepOffset * vec2(-6.0, 0.0);
    blurCoordinates[16] = outTexPos.xy + singleStepOffset * vec2(-4.0, -4.0);
    blurCoordinates[17] = outTexPos.xy + singleStepOffset * vec2(-4.0, 4.0);
    blurCoordinates[18] = outTexPos.xy + singleStepOffset * vec2(4.0, -4.0);
    blurCoordinates[19] = outTexPos.xy + singleStepOffset * vec2(4.0, 4.0);

    float sampleColor = centralColor.g * 20.0;
    sampleColor += texture(sTexture, blurCoordinates[0]).g;
    sampleColor += texture(sTexture, blurCoordinates[1]).g;
    sampleColor += texture(sTexture, blurCoordinates[2]).g;
    sampleColor += texture(sTexture, blurCoordinates[3]).g;
    sampleColor += texture(sTexture, blurCoordinates[4]).g;
    sampleColor += texture(sTexture, blurCoordinates[5]).g;
    sampleColor += texture(sTexture, blurCoordinates[6]).g;
    sampleColor += texture(sTexture, blurCoordinates[7]).g;
    sampleColor += texture(sTexture, blurCoordinates[8]).g;
    sampleColor += texture(sTexture, blurCoordinates[9]).g;
    sampleColor += texture(sTexture, blurCoordinates[10]).g;
    sampleColor += texture(sTexture, blurCoordinates[11]).g;
    sampleColor += texture(sTexture, blurCoordinates[12]).g * 2.0;
    sampleColor += texture(sTexture, blurCoordinates[13]).g * 2.0;
    sampleColor += texture(sTexture, blurCoordinates[14]).g * 2.0;
    sampleColor += texture(sTexture, blurCoordinates[15]).g * 2.0;
    sampleColor += texture(sTexture, blurCoordinates[16]).g * 2.0;
    sampleColor += texture(sTexture, blurCoordinates[17]).g * 2.0;
    sampleColor += texture(sTexture, blurCoordinates[18]).g * 2.0;
    sampleColor += texture(sTexture, blurCoordinates[19]).g * 2.0;

    sampleColor = sampleColor / 48.0;

    float highPass = centralColor.g - sampleColor + 0.5;

    for(int i = 0; i < 5;i++)
    {
        highPass = hardLight(highPass);
    }
    float luminance = dot(centralColor, W);

    float alpha = pow(luminance, params);

    vec3 smoothColor = centralColor + (centralColor-vec3(highPass))*alpha*0.1;

    color = vec4(mix(smoothColor.rgb, max(smoothColor, centralColor), alpha), 1.0);
}