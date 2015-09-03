//
// Created by kodjo baah on 23/08/2015.
//

#include <ZmqException.hpp>
#include <ZeroMqWrapper.h>

namespace waid {


    ZeroMqWrapper::~ZeroMqWrapper() {
        delete zeroMq;
    }

    ZeroMqWrapper::ZeroMqWrapper(std::string id) {
        zeroMq = new waid::ZeroMq(id);
    }

    int ZeroMqWrapper::s_send(char *string) {
        return zeroMq->s_send(string);
    }
    int ZeroMqWrapper::create_context() {
        std::unique_lock <std::mutex> lock(mtx_create_context);
        return zeroMq->create_context();
    }

    int ZeroMqWrapper::create_socket(std::string auth, const char *urlPath) {
        std::unique_lock <std::mutex> lock(mtx_create_socket);
        return zeroMq->create_socket(auth, urlPath);
    }

    void ZeroMqWrapper::s_sendmultiple(std::vector < std::string > messages) {
        std::unique_lock <std::mutex> lock(mtx_s_sendmultiple);
        if(sendMultipleSuccess) {
            int result = zeroMq->s_sendmultiple(messages);
            if (result != 0) {
                sendMultipleSuccess = false;
            }
        } else {

            throw zmq_multi_send_exception;
        }
    }

    std::vector <std::string> ZeroMqWrapper::s_recv_multi(int num) {
        std::unique_lock <std::mutex> lock(mtx_s_recv_multi);
        return zeroMq->s_recv_multi(num);
    }

    void ZeroMqWrapper::setNativeCommunicator(waid::NativeCommunicator * nc) {
        zeroMq->setNativeCommunicator(nc);
    }

    void ZeroMqWrapper::stopAll() {
        if (!stopped) {
            zeroMq->stopAll();
            stopped = true;
        }
    }
}