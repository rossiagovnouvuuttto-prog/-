#version 120

uniform float time;
uniform vec3 ringColor;
uniform float ringWidth;
uniform float pulseWidth;

void main() {
    vec2 uv = gl_TexCoord[0].st;
    float d = length(uv);

    float ringInner  = 1.0 - ringWidth;
    float ringOuter  = 1.0 + ringWidth * 0.7;

    float pulseT     = mod(time, 1800.0) / 1800.0;
    float pulseR     = 0.10 + 0.90 * pulseT;
    float pInner     = max(0.0, pulseR - pulseWidth);
    float pOuter     = pulseR + pulseWidth;

    if (d > max(ringOuter, pOuter)) discard;

    float ringAlpha = 0.0;
    if (d >= ringInner && d <= ringOuter) {
        float distFromPeak;
        if (d <= 1.0) {
            distFromPeak = (1.0 - d) / ringWidth;
        } else {
            distFromPeak = (d - 1.0) / (ringWidth * 0.7);
        }
        float falloff = 1.0 - distFromPeak;
        ringAlpha = falloff * falloff * 0.85;
    }

    float pulseAlpha = 0.0;
    if (d >= pInner && d <= pOuter) {
        float t = (d - pInner) / (pOuter - pInner);
        float dist = abs(t - 0.5) * 2.0;
        float falloff = 1.0 - dist;
        float pulseAlphaMax = (1.0 - pulseT) * (1.0 - pulseT) * 0.55;
        pulseAlpha = falloff * falloff * pulseAlphaMax;
    }

    float a = ringAlpha + pulseAlpha;
    if (a < 0.002) discard;
    gl_FragColor = vec4(ringColor, a);
}
