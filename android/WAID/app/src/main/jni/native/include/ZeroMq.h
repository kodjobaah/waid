//
// Created by kodjo baah on 18/08/2015.
//

#ifndef WAID_ZEROMQ_H
#define WAID_ZEROMQ_H

#include <vector>

#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <zmq.h>

#include <NativeCommunicator.h>
#include <stdio.h>
#include <stdlib.h>

namespace waid {

    class ZeroMq {

    private:

        void *context;
        void *socket;
        void *monitor;
        void *monitorSocket;

        std::string identifier;
        int connectionStatus;

        waid::NativeCommunicator *nativeCommunicator;

        boost::mutex m_mutex;   // The mutex to synchronise on
        boost::condition_variable connection_cond;// The condition to wait for
        boost::shared_ptr<boost::thread> connectionMonitorThread;


        int get_monitor_event (void *monitor, int *value, char **address);
        int read_event_msg(void* s, zmq_event_t* event, char* ep);


        pthread_mutex_t mutex_mainSocket;
        pthread_mutex_t mutex_monitorSocket;
        bool mainSocketAvailable;
        bool monitorSocketAvailable;

        void monitorConnection();

    public:

        static const std::string INIT_MESSAGE;

        ZeroMq(std::string id);

        ~ZeroMq();

        int create_context();

        int create_socket(std::string auth, const char *urlPath);

        char *s_recv ();

        std::vector<std::string> s_recv_multi(int num);

        int s_send (char *string);

        int s_sendmore (char *string);

        int s_sendmultiple(std::vector<std::string> messages);

        void setNativeCommunicator(NativeCommunicator *nc);

        void stopAll();

        int getConnectionStatus();
    };

}
#endif //WAID_ZEROMQ_H
