//
// Created by kodjo baah on 27/07/2015.
//

#include <android/log.h>


#include <string.h>
#include <vector>
#include <cv.h>

#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#ifndef HELLO_GL2_VIDEORENDERERNOVBO_H
#define HELLO_GL2_VIDEORENDERERNOVBO_H


namespace  waid {
    class VideoRendererVbo {

    private:


        glm::mat4 Model, View, Projection;

        GLchar *FRAGMENT_SHADER;
        GLchar *VERTEX_SHADER;
        std::string vertexShader;
        std::string fragmentShader;

        GLfloat vertices[12];
        GLfloat texCoords[8];
        GLushort indices[6];

        GLfloat projMat[16];


        GLuint frameBuffer;
        GLuint textureId;
        GLuint depthStencil;

        GLuint gProgram;
        GLuint aPosition;
        GLuint aTexCoord;
        GLuint uSampler;
        GLuint projectionUniform;
        GLint modelUniform;
        GLint viewUniform;

        GLuint createShader(GLenum shaderType, const char* src);
        GLuint createProgram(const char* vtxSrc, const char* fragSrc);
        void printGlString(const char* name, GLenum s);
        bool checkGlError(const char* funcName);
        void loadShaders();
        void init(int screenWidth, int screenHeight);
        void renderFrameBuffer(int screenWidth, int screenHeight, cv::Mat frame);
    public:
        VideoRendererVbo();

        bool setupGraphics(int w, int h);
        void renderFrame(int w, int h, cv::Mat frame);

    };

}
#endif //HELLO_GL2_VIDEORENDERERNOVBO_H
