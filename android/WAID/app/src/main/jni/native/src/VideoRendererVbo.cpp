//
// Created by kodjo baah on 27/07/2015.
//


#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <math.h>

#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

#define PI 3.14159265

#include <VideoRendererVbo.h>


// Utility for logging:
#define LOG_TAG    "CAMERA_RENDERER"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

namespace waid {


    VideoRendererVbo::VideoRendererVbo() {

        vertexShader = std::string("uniform mat4 u_projection;   \n"
                                           "uniform mat4 u_model;        \n"
                                           "uniform mat4 u_view;         \n"
                                           "attribute vec4 a_position;   \n"
                                           "attribute vec2 a_texCoord;   \n"
                                           "varying vec2 v_texCoord;     \n"
                                           "varying vec3 vPosition;     \n"
                                           "void main()                  \n"
                                           "{                            \n"
                                           //                                    "    gl_Position = u_projection * a_position; \n"
                                           //                                     "    gl_Position = a_position; \n"
                                           "    gl_Position = u_projection * u_view * u_model * a_position; \n"
                                           //                                     "    gl_Position = u_projection * u_view * a_position; \n"
                                           "   v_texCoord = a_texCoord;  \n"
                                           //                                   "   vPosition = gl_Position.xyz;  \n"
                                           "}  \n");

        VERTEX_SHADER = const_cast<GLchar * >(vertexShader.c_str());

        fragmentShader = std::string("precision mediump float;                         \n"
                                             "varying vec2 v_texCoord;                            \n"
                                             //                                       "varying vec3 vPosition;                             \n"
                                             "uniform sampler2D s_texture;                        \n"
                                             "void main()                                         \n"
                                             "{                                                   \n"
                                             "                                                    \n"
                                             "  vec4 texCol = texture2D( s_texture, v_texCoord );   \n"
                                             "  gl_FragColor = texCol;   \n"
                                             "}                                                   \n");

        FRAGMENT_SHADER = const_cast<GLchar *>(fragmentShader.c_str());


        vertices[0] = -1.0f;
        vertices[1] = 1.0f;
        vertices[2] = 0.0f;
        vertices[3] = -1.0f;
        vertices[4] = -1.0f;
        vertices[5] = 0.0f;
        vertices[6] = 1.0f;
        vertices[7] = 1.0f;
        vertices[8] = 0.0f;
        vertices[9] = 1.0f;
        vertices[10] = -1.0f;
        vertices[11] = 0.0f;

        texCoords[0] = 0.0f;
        texCoords[1] = 1.0;
        texCoords[2] = 0.0;
        texCoords[3] = 0.0f;
        texCoords[4] = 1.0f;
        texCoords[5] = 1.0f;
        texCoords[6] = 1.0f;
        texCoords[7] = 0.0f;

        indices[0] = 0;
        indices[1] = 1;
        indices[2] = 2;
        indices[3] = 0;
        indices[4] = 2;
        indices[5] = 3;

        // Set the projection matrix
        //Projection = glm::ortho(-4.0f/3.0f, 4.0f/3.0f, -1.0f, 1.0f, -1.0f, 1.0f);
        //Projection = glm::ortho(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);
        Projection = glm::mat4(1.0);

        // Translation
        Model = glm::translate(Model, glm::vec3(0.1f, 0.1f, 0.1f));

        // Rotation around Oz with 45 degrees
        Model = glm::rotate(Model, 0.0f, glm::vec3(0.0f, 0.0f, 1.0f));

    }

    bool VideoRendererVbo::checkGlError(const char *funcName) {
        GLint err = glGetError();
        if (err != GL_NO_ERROR) {
            LOGE("GL error after %s(): 0x%08x\n", funcName, err);
            return true;
        }
        return false;
    }

    void VideoRendererVbo::printGlString(const char *name, GLenum s) {
        const char *v = (const char *) glGetString(s);
        LOG("GL %s: %s\n", name, v);
    }


    void VideoRendererVbo::init(int screenWidth, int screenHeight) {

        //framebuffer
        glGenFramebuffers(1, &frameBuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

        //texture
        glGenTextures(1, &textureId);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        //width and height: screen size
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, screenWidth, screenHeight, 0, GL_RGB,
                     GL_UNSIGNED_SHORT_5_6_5, NULL);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        //depth and stencil
        glGenRenderbuffers(1, &depthStencil);
        glBindRenderbuffer(GL_RENDERBUFFER, depthStencil);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8_OES, screenWidth, screenHeight);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER,
                                  depthStencil);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER,
                                  depthStencil);

    }


    void VideoRendererVbo::renderFrameBuffer(int screenWidth, int screenHeight, cv::Mat frame) {

        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

        glViewport(10, 10, screenWidth, screenHeight);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);


        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, frame.cols, frame.rows, GL_RGB,
                        GL_UNSIGNED_SHORT_5_6_5, frame.ptr());

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

    }

    void VideoRendererVbo::renderFrame(int screenWidth, int screenHeight, cv::Mat frame) {

        glUseProgram(gProgram);

        renderFrameBuffer(screenWidth, screenHeight, frame);

        //clear
        glEnable(GL_DEPTH_TEST);
        glClear(GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        //blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        //binding texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(uSampler, 0);

        //set attributes
        // glUniformMatrix4fv(projectionUniform, 1, GL_FALSE, projMat);

        // Transfer the transformation matrices to the shader program
        glUniformMatrix4fv(modelUniform, 1, GL_FALSE, glm::value_ptr(Model));

        glUniformMatrix4fv(viewUniform, 1, GL_FALSE, glm::value_ptr(View));

        glUniformMatrix4fv(projectionUniform, 1, GL_FALSE, glm::value_ptr(Projection));

        glVertexAttribPointer(aPosition, 3, GL_FLOAT, GL_FALSE, 0, &vertices[0]);
        glEnableVertexAttribArray(aPosition);
        glVertexAttribPointer(aTexCoord, 2, GL_FLOAT, GL_FALSE, 0, &texCoords[0]);
        glEnableVertexAttribArray(aTexCoord);

        //draw
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    }


    void VideoRendererVbo::loadShaders() {
        printGlString("Version", GL_VERSION);
        printGlString("Vendor", GL_VENDOR);
        printGlString("Renderer", GL_RENDERER);
        printGlString("Extensions", GL_EXTENSIONS);

        LOG("setupGraphics");
        gProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (!gProgram) {
            LOGE("Could not create program.");
            return;
        }
        aPosition = glGetAttribLocation(gProgram, "a_position");
        aTexCoord = glGetAttribLocation(gProgram, "a_texCoord");

        uSampler = glGetAttribLocation(gProgram, "s_texture");

        projectionUniform = glGetUniformLocation(gProgram, "u_projection");

        modelUniform = glGetUniformLocation(gProgram, "u_model");
        viewUniform = glGetUniformLocation(gProgram, "u_view");


    }

    bool VideoRendererVbo::setupGraphics(int screenWidth, int screenHeight) {

        loadShaders();
        init(screenWidth, screenHeight);

    }


    /*
    * Create a shader object, load the shader source, and
    * compile the shader.
    */
    GLuint VideoRendererVbo::createShader(GLenum shaderType, const char *src) {
        GLuint shader = glCreateShader(shaderType);
        if (!shader) {
            checkGlError("glCreateShader");
            return 0;
        }
        glShaderSource(shader, 1, &src, NULL);

        GLint compiled = GL_FALSE;
        glCompileShader(shader);
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLogLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLogLen);
            if (infoLogLen > 0) {
                GLchar *infoLog = (GLchar *) malloc(infoLogLen);
                if (infoLog) {
                    glGetShaderInfoLog(shader, infoLogLen, NULL, infoLog);
                    LOGE("Could not compile %s shader:\n%s\n",
                         shaderType == GL_VERTEX_SHADER ? "vertex" : "fragment",
                         infoLog);
                    free(infoLog);
                }
            }
            glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    GLuint VideoRendererVbo::createProgram(const char *vtxSrc, const char *fragSrc) {
        GLuint vtxShader = 0;
        GLuint fragShader = 0;
        GLuint program = 0;
        GLint linked = GL_FALSE;

        vtxShader = createShader(GL_VERTEX_SHADER, vtxSrc);
        if (!vtxShader)
            goto exit;

        fragShader = createShader(GL_FRAGMENT_SHADER, fragSrc);
        if (!fragShader)
            goto exit;

        program = glCreateProgram();
        if (!program) {
            checkGlError("glCreateProgram");
            goto exit;
        }
        glAttachShader(program, vtxShader);
        glAttachShader(program, fragShader);

        glLinkProgram(program);
        glGetProgramiv(program, GL_LINK_STATUS, &linked);
        if (!linked) {
            LOGE("Could not link program");
            GLint infoLogLen = 0;
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLogLen);
            if (infoLogLen) {
                GLchar *infoLog = (GLchar *) malloc(infoLogLen);
                if (infoLog) {
                    glGetProgramInfoLog(program, infoLogLen, NULL, infoLog);
                    LOGE("Could not link program:\n%s\n", infoLog);
                    free(infoLog);
                }
            }
            glDeleteProgram(program);
            program = 0;
        }

        exit:
        glDeleteShader(vtxShader);
        glDeleteShader(fragShader);
        glReleaseShaderCompiler();
        return program;
    }

}

