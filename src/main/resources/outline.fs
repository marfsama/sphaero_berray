#version 330

// Input vertex attributes (from vertex shader)
in vec2 fragTexCoord;
in vec4 fragColor;

// Input uniform values
uniform sampler2D texture0;
uniform vec4 colDiffuse;

uniform vec2 textureSize;
uniform float outlineSize;
uniform vec4 outlineColor;

// Output fragment color
out vec4 finalColor;

void main()
{
    vec4 texel = texture(texture0, fragTexCoord);   // Get texel color
    vec2 texelScale = vec2(0.0);
    texelScale.x = outlineSize/textureSize.x;
    texelScale.y = outlineSize/textureSize.y;

    // We sample four corner texels, but only for the alpha channel (this is for the outline)
    vec2 corners = vec2(0.0); // NW, SE
    corners.x = texture(texture0, fragTexCoord + vec2(-texelScale.x, -texelScale.y)).a;
    corners.y = texture(texture0, fragTexCoord + vec2(texelScale.x, texelScale.y)).a;

    vec4 texelColor;
    if (texel.a < 0.1) {
      // texel transparent. just keep the pixel
      texelColor = texel;
    } else if (corners.x < 0.1 && corners.y > 0.9) {
      // NE is transparent and SW is opaque. brighten pixel
      texelColor = mix(texel, vec4(1.0), 0.4);
    } else if (corners.y < 0.1 && corners.x > 0.9) {
      // SW is transparent and NE is opaque. darken pixel
      texelColor = mix(texel, vec4(0.0, 0.0, 0.0, 1.0), 0.3);
    } else {
      // pixel is not an edge. just keep the pixel
      texelColor = texel;
    }

    finalColor = texelColor * colDiffuse * fragColor;
}