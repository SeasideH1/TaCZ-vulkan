#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D OcularIds;
layout(location = 0) flat in float ocularId;
layout(location = 0) out vec4 fragColor;
void main() {
    int stored = int(round(texelFetch(OcularIds, ivec2(gl_FragCoord.xy), 0).r * 255.0));
    int reference = int(round(ocularId * 255.0));
    if (stored != reference) discard;
    fragColor = vec4(float(255 - stored) / 255.0, 0.0, 0.0, 1.0);
}
