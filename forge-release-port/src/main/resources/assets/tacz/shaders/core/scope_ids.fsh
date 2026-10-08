#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D Sampler0;
layout(location = 0) flat in float ocularId;
layout(location = 1) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    if (texture(Sampler0, texCoord).a < 0.1) discard;
    fragColor = vec4(ocularId, 0.0, 0.0, 1.0);
}
