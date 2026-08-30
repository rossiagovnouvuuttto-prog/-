#version 120

uniform vec2 rectSize;
uniform vec4 diskColor;
uniform vec4 trackColor;
uniform vec4 progressColor;
uniform float progress;   // 0..1
uniform float thickness;  // ring band thickness, px

const float PI2 = 6.28318530718;

void main() {
    vec2 p = (gl_TexCoord[0].st - 0.5) * rectSize;
    float r = length(p);
    float outerR = min(rectSize.x, rectSize.y) * 0.5;
    float innerR = outerR - thickness;

    // antialiased disk (inside the ring) and ring band masks
    float diskA = 1.0 - smoothstep(innerR - 1.0, innerR, r);
    float ringA = smoothstep(innerR - 1.0, innerR, r) * (1.0 - smoothstep(outerR - 1.0, outerR, r));
    if (diskA + ringA < 0.002) discard;

    // angle from the top (0), clockwise, normalized to 0..1
    float frac = atan(p.x, -p.y) / PI2;
    if (frac < 0.0) frac += 1.0;
    float edge = 1.0 / max(1.0, r * PI2); // ~1px angular antialias
    float prog = 1.0 - smoothstep(progress - edge, progress + edge, frac);
    prog = progress <= 0.0 ? 0.0 : (progress >= 1.0 ? 1.0 : prog);

    vec4 band = mix(trackColor, progressColor, prog);

    float aDisk = diskA * diskColor.a;
    float aRing = ringA * band.a;
    float a = aDisk + aRing;
    if (a < 0.002) discard;
    vec3 rgb = (diskColor.rgb * aDisk + band.rgb * aRing) / a;
    gl_FragColor = vec4(rgb, a);
}
