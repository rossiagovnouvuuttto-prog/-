#version 120

uniform vec2 location, rectSize;
uniform float radius;
uniform float alpha;

float roundSDF(vec2 p, vec2 b, float r) {
    return length(max(abs(p) - b, 0.0)) - r;
}

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec2 rectHalf = rectSize * 0.5;
    vec2 uv = gl_TexCoord[0].st * rectSize - rectHalf;

    float dist = roundSDF(uv, rectHalf - radius, radius);
    float mask = 1.0 - smoothstep(0.0, 1.0, dist);

    vec2 pixel = floor(gl_FragCoord.xy);
    float noise = hash(pixel);

    float gray = mix(0.75, 1.0, noise);

    gl_FragColor = vec4(vec3(gray), mask * alpha);
}
