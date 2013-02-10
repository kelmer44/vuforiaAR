varying vec3 lightDir,normal;

uniform sampler2D textureUnit0;
uniform sampler2D textureUnit1;

attribute vec4 position;
attribute vec3 normal;
attribute vec4 tangent;
attribute vec2 texture0;
 
void main()
{
    normal = normalize(gl_NormalMatrix * gl_Normal);
 
    lightDir = normalize(vec3(gl_LightSource[0].position));
    gl_TexCoord[0] = gl_MultiTexCoord0;
 
    gl_Position = ftransform();
}

