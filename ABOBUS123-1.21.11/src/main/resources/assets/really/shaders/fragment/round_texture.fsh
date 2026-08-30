#version 120

uniform vec2 location, rectSize;
uniform sampler2D texture;
uniform float radius;
uniform float u, v, w, h;
uniform vec4 tintColor;

float roundSDF(vec2 p, vec2 b, float r) {
    return length(max(abs(p) - b, 0.0)) - r;
}

void main() {
    vec2 rectHalf = rectSize * 0.5;

    vec2 texCoords = gl_TexCoord[0].st;
    texCoords.x = u + texCoords.x * w;
    texCoords.y = v + texCoords.y * h;

    float smoothedAlpha = (1.0 - smoothstep(0.0, 1.0, roundSDF(rectHalf - (gl_TexCoord[0].st * rectSize), rectHalf - radius - 1.0, radius)));
    vec4 color = texture2D(texture, texCoords);

    gl_FragColor = vec4(color.rgb * tintColor.rgb, color.a * tintColor.a * smoothedAlpha);
}