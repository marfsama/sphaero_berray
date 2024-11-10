#version 330

// Input vertex attributes (from vertex shader)
in vec2 fragTexCoord;
in vec4 fragColor;

// Input uniform values
uniform sampler2D texture0;
uniform vec4 colDiffuse;

uniform vec4 textureSize;

uniform vec4 highlightMask;
uniform vec4 lowlightMask;

// Output fragment color
out vec4 finalColor;

void main()
{
    vec4 texel = texture(texture0, fragTexCoord);   // Get texel color
    vec2 texelScale = vec2(0.0);
    texelScale.x = 1.0 / textureSize.x;
    texelScale.y = 1.0 / textureSize.y;

    // We sample four corner texels, but only for the alpha channel (this is for the outline)
    vec4 corners = vec4(0.0);
    corners.x = texture(texture0, fragTexCoord + vec2(-texelScale.x, -texelScale.y)).a; // NW
    corners.y = texture(texture0, fragTexCoord + vec2(-texelScale.x, texelScale.y)).a; // SW
    corners.z = texture(texture0, fragTexCoord + vec2(texelScale.x, texelScale.y)).a; // SE
    corners.a = texture(texture0, fragTexCoord + vec2(texelScale.x, -texelScale.y)).a; // NE

    corners.a = 1.0;
    vec4 texelColor = corners;

    //finalColor = texelColor * colDiffuse * fragColor;
    finalColor = texelColor;
}