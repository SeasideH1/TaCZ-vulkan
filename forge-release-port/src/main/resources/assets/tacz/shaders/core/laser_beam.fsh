#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:dynamictransforms.glsl>
uniform sampler2D Sampler0;
layout(location = 0) in vec2 laserUv;
layout(location = 1) in vec4 laserColor;
layout(location = 0) out vec4 fragColor;
void main() { fragColor = texture(Sampler0, laserUv) * laserColor * ColorModulator; }
