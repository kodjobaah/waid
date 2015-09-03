#ifndef __WAID_HPP_INCLUDED__
#define __WAID_HPP_INCLUDED__

//  Include a bunch of headers that we will need in the examples

#include <zmq.hpp> // https://github.com/zeromq/cppzmq


#define LOG_TAG_MONITOR    "ZEROMQ_MONITOR"
#define LOGM(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG_MONITOR, __VA_ARGS__)

namespace  waid {

    class WaidMonitor : public zmq::monitor_t {


    public:

        void on_event_connect_delayed(const zmq_event_t &event_, const char* addr_) {
            LOGM("ON_EVENT_CONNECT_DELAYED");

        }
        void on_event_connect_retried(const zmq_event_t &event_, const char* addr_) {
            LOGM("ON_EVENT_CONNECT_RETRIED");
        }
        void on_event_accept_failed(const zmq_event_t &event_, const char* addr_) {
            LOGM("ON_EVENT_ACCEPT_FAILED");
        }
    };
}
#endif
