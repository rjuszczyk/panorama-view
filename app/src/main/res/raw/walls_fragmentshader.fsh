precision mediump float;        // Set the default precision to medium. We don't need as high of a
                                // precision in the fragment shader.
uniform vec3 u_LightPos;        // The position of the light in eye space.
uniform sampler2D u_Texture;    // The input texture.
 
varying vec3 v_Position;        // Interpolated position for this fragment.
varying vec4 v_Color;           // This is the color from the vertex shader interpolated across the
                                // triangle per fragment.
varying vec3 v_Normal;          // Interpolated normal for this fragment.
varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.
 
// The entry point for our fragment shader.
void main()
{
    float distance = length(u_LightPos - v_Position);

    vec3 lightVector = normalize(u_LightPos - v_Position);

    float diffuse = max(dot(normalize(v_Normal), lightVector), 0.0);

   if(diffuse < 0.0) {
           diffuse = 0.0;
       }
       if(diffuse > 1.0)
          diffuse = 1.0;
    diffuse = diffuse * 0.2;



       diffuse = diffuse + 0.65;

float d2 = (1.0 / (1.0 + (0.2 * distance))) * 0.15;
   diffuse = diffuse + d2;



    gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0) + (vec4(1.0, 1.0, 1.0, 0.0) *diffuse * texture2D(u_Texture, v_TexCoordinate));
  }

/*precision mediump float; 
uniform vec3 u_LightPos; 

varying vec3 v_Position;
varying vec4 v_Color;
        
varying vec3 v_Normal; 


void main()
{
	float distance = length(u_LightPos - v_Position);
	
	vec3 lightVector = normalize(u_LightPos - v_Position);
	
	float diffuse = max(dot(v_Normal, lightVector), 0.5);
	
	diffuse = diffuse * (1.0 / (1.0 + (0.002 * distance * distance)));
	
	gl_FragColor = v_Color * diffuse;
}*/