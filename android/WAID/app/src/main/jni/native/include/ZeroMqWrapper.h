//
// Created by kodjo baah on 23/08/2015.
//

#ifndef WAID_ZEROMQWRAPPER_H
#define WAID_ZEROMQWRAPPER_H
#include <thread>
#include <mutex>
#include <ZeroMq.h>
#include <vector>
#include <string>
#include <NativeCommunicator.h>

namespace waid {

    class ZeroMqWrapper {

    private:
        std::mutex mtx_create_socket;
        std::mutex mtx_create_context;
        std::mutex mtx_s_recv_multi;
        std::mutex mtx_s_sendmultiple;
        waid::ZeroMq *zeroMq;
        bool stopped = false;
        bool sendMultipleSuccess = true;
    public:

        ZeroMqWrapper(std::string id);
        ~ZeroMqWrapper();
        int create_context();
        int create_socket(std::string auth, const char *urlPath);
        void s_sendmultiple(std::vector<std::string> messages);
        std::vector<std::string> s_recv_multi(int num);
        void setNativeCommunicator(NativeCommunicator *nc);
        void stopAll();
        int s_send(char *string);
    };
}


#endif //WAID_ZEROMQWRAPPER_H
