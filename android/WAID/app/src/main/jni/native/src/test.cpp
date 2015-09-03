#include <string>
#include <sstream>
#include <vector>

int main() {

        short number[]= {-2,3,-4,5,6,6,7,6};
        std::vector<short> mynumbers;
        std::string stuff;
        for(int i=0;i < 8; i++) {
 
	   std::stringstream stream;
           stream << number[i];
           std::string s = stream.str(); 
           if (i == 0) {
           stuff = s;
           } else {
            stuff = stuff +","+s; 
          }
         }
	printf("this is it[%s]\n",stuff.c_str());

}
