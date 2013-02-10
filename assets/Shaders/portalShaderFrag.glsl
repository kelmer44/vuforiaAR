precision mediump float;

varying vec3 lightDir,normal;

//uniform vec4 ambientColor;
uniform sampler2D textureUnit0;
uniform sampler2D textureUnit1;

void main()
{
    vec3 ct,cf;
    vec4 texel;
    float intensity,at,af;
    intensity = max(dot(lightDir,normalize(normal)),0.0);
 
    cf = intensity * (gl_FrontMaterial.diffuse).rgb +
                  gl_FrontMaterial.ambient.rgb;
    af = gl_FrontMaterial.diffuse.a;
    texel = texture2D(textureUnit1,gl_TexCoord[0].st);//vec4(0.5,0.5,0.5,0.5);//
    
     
    ct = texel.rgb;
    at = texel.a;
    
 
	//vec4 base = texture2D(textureUnit0, texCoord);
	//gl_FragDepth = gl_FragCoord.z;
	vec2 texCoord;
	texCoord.x = gl_FragCoord.x/800.;
	texCoord.y = gl_FragCoord.y/600.;
	vec4 base = texture2D(textureUnit0,texCoord);
	if(base.r > 0.1)
	{
		discard;
		//gl_FragColor = vec4(0.5,0.5,0.5,0.5);
	}
	else
	{
	//gl_FragColor = base;//vec4(gl_FragDepth, gl_FragDepth, 0.0, 0.5);//(vAmbient*base + vDiffuse*base + vSpecular) * att*2.0;
		gl_FragColor = vec4(ct * cf, at * af);
		//gl_FragColor = col;
		//gl_FragColor = vec4(0.,0.,0.,0.);
	}
}
