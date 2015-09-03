//
// Created by kodjo baah on 23/08/2015.
//

#ifndef WAID_WAIDUTILS_H
#define WAID_WAIDUTILS_H


namespace waid {
class WaidUtils {

public:
    std::string base64Encode(const unsigned char *buffer, size_t length,char **b64text);

    };
}


#endif //WAID_WAIDUTILS_H
