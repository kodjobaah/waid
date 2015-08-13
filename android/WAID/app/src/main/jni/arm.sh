#!/bin/bash
export PATH=/opt/android-toolchain-arm/bin:$PATH
#export OUTPUT_DIR=/tmp/zeromq-android-arm
export OUTPUT_DIR=/Users/kodjobaah/AndroidStudioProjects/WAID/app/src/main/jni/zeromq
cd zeromq4-x/
./autogen.sh
#./configure --enable-static --disable-shared --host=arm-linux-androideabi --prefix=$OUTPUT_DIR LDFLAGS="-L$OUTPUT_DIR/lib" CPPFLAGS="-Werror -Wno-deprecated -Wno-deprecated-declarations -fPIC -I$OUTPUT_DIR/include" LIBS="-lgcc -lstdc++ -Wno-deprecated"
./configure --host=arm-linux-androideabi --prefix=$OUTPUT_DIR LDFLAGS="-L$OUTPUT_DIR/lib" CPPFLAGS="-Werror -Wno-deprecated -Wno-deprecated-declarations -fPIC -I$OUTPUT_DIR/include" LIBS="-lgcc -lstdc++ -Wno-deprecated"
make
make install
