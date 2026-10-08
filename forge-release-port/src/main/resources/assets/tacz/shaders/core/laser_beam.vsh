#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:projection.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:sample_lightmap.glsl>
layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
uniform sampler2D Sampler2;
layout(location = 0) out vec2 laserUv;
layout(location = 1) out vec4 laserColor;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    laserUv = UV0;
    laserColor = Color * sample_lightmap(Sampler2, UV2);
}
