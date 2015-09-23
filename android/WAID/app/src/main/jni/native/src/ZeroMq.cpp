//
// Created by kodjo baah on 18/08/2015.
//

#include <ZeroMq.h>

#include <android/log.h>

#define LOG_TAG    "ZEROMQ"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)


namespace waid {

    const std::string ZeroMq::INIT_MESSAGE = "INIT";

    ZeroMq::ZeroMq(std::string id) {

        identifier = id;
        mainSocketAvailable = true;

        monitorSocketAvailable = true;

        pthread_mutex_init(&mutex_mainSocket, NULL);

        pthread_mutex_init(&mutex_monitorSocket, NULL);

    }

    ZeroMq::~ZeroMq() {

    }

    void ZeroMq::stopAll() {
        LOG("ZMQ_EVENT:STARTING_STOP_ALL[%s]",identifier.c_str());
        int linger = 0;

        LOG("ZMQ_EVENT:STOPING MAIN SOCKET[%s]",identifier.c_str());
        pthread_mutex_lock(&mutex_mainSocket);
        if (mainSocketAvailable) {
            int rc = zmq_setsockopt(socket, ZMQ_LINGER, &linger, sizeof(linger));
            zmq_close(socket);
            mainSocketAvailable = false;
        }
        pthread_mutex_unlock(&mutex_mainSocket);

        LOG("ZMQ_EVENT:STOPING MONITOR SOCKET[%s]",identifier.c_str());
        pthread_mutex_lock(&mutex_monitorSocket);
        if (monitorSocketAvailable) {
            linger = 0;
            int rc = zmq_setsockopt(monitorSocket, ZMQ_LINGER, &linger, sizeof(linger));
            zmq_close(monitorSocket);
            monitorSocketAvailable = false;
        }
        pthread_mutex_unlock(&mutex_monitorSocket);

        zmq_ctx_shutdown (context);
        zmq_ctx_term(context);
        pthread_mutex_destroy(&mutex_monitorSocket);
        pthread_mutex_destroy(&mutex_mainSocket);
        LOG("ZMQ_EVENT:FINNISH_STOP_AL[%s]",identifier.c_str());
    }

    int ZeroMq::create_context() {
        context = zmq_ctx_new();
        if (context == NULL) {
            LOG("CONTEXT_CREATION_ERROR(-1)[%s]",identifier.c_str());
            return -1;
        }

        int rc = zmq_ctx_set(context, ZMQ_IO_THREADS, 1);
        if (rc != 0) {
            LOG("CONTEXT_CREATION_ERROR(-2)[%s]",identifier.c_str());
            return -2;
        }

        rc = zmq_ctx_set(context, ZMQ_MAX_SOCKETS, ZMQ_MAX_SOCKETS_DFLT);
        if (rc != 0) {
            LOG("CONTEXT_CREATION_ERROR(-3)[%s]",identifier.c_str());
            return -3;
        }

        return 0;
    }

    int ZeroMq::create_socket(std::string auth, const char *urlPath) {
        socket = zmq_socket(context, ZMQ_DEALER);
        if (socket == NULL) {
            LOG("ZMQ_EVENT_CREATE_SOCKET_ERROR_MINUS_[%s]",identifier.c_str());
            return -4;
        }

        int linger = 0;
        int rc = zmq_setsockopt(socket, ZMQ_LINGER, &linger, sizeof(linger));

        if (rc != 0) {
            LOG("ZMQ_EVENT_CREATE_SOCKET_ERROR_MINUS_[%s]",identifier.c_str());
            return -5;
        }

        std::string id = auth+":"+identifier;
        rc = zmq_setsockopt(socket, ZMQ_IDENTITY, id.c_str(), (long) id.length());

        if (rc != 0) {
            LOG("ZMQ_EVENT_CREATE_SOCKET_ERROR_MINUS_[%s]",identifier.c_str());
            return -6;
        }

        rc = zmq_connect(socket, urlPath);

        if (rc != 0) {
            LOG("ZMQ_EVENT_CREATE_SOCKET_ERROR_MINUS_7[%s]",urlPath);
            return -7;
        }


        // REP socket monitor, all events
        std::string monitorsocket = "inproc://monitor.dealer-"+identifier;
        rc = zmq_socket_monitor(socket,monitorsocket.c_str() , ZMQ_EVENT_ALL);
        if (rc != 0) {
            LOG("ZMQ_EVENT_CREATE_SOCKET_ERROR_MINUS_8[%s]",identifier.c_str());
            return -8;
        }


        connectionStatus = 0;

        boost::shared_ptr <boost::thread> pt(
                new boost::thread(&ZeroMq::monitorConnection, this));
        connectionMonitorThread.swap(pt);

        boost::unique_lock <boost::mutex> lock(m_mutex);

        std::vector <std::string> init_message;
        init_message.push_back(INIT_MESSAGE);
        s_sendmultiple(init_message);

        LOG("ZMQ_EVENT: waitking for connection to complete[%s]",identifier.c_str());
        connection_cond.wait(lock);
        LOG("ZMQ_EVENT: able to connect[%s]",identifier.c_str());

        return connectionStatus;
    }

    int ZeroMq::read_event_msg(void *s, zmq_event_t *event, char *ep) {

        pthread_mutex_lock(&mutex_monitorSocket);
        int result = 0;
        if (monitorSocketAvailable) {
            int rc;
            zmq_msg_t msg1; // binary part
            zmq_msg_init(&msg1);
            zmq_msg_t msg2; // address part
            zmq_msg_init(&msg2);
            rc = zmq_msg_recv(&msg1, s, 0);
            if (rc == -1 && zmq_errno() == ETERM)
                return 1;

            assert(rc != -1);
            assert(zmq_msg_more(&msg1) != 0);
            rc = zmq_msg_recv(&msg2, s, 0);
            if (rc == -1 && zmq_errno() == ETERM)
                return 1;
            assert(rc != -1);
            assert(zmq_msg_more(&msg2) == 0);
            // copy binary data to event struct
            const char *data = (char *) zmq_msg_data(&msg1);
            memcpy(&(event->event), data, sizeof(event->event));
            memcpy(&(event->value), data + sizeof(event->event), sizeof(event->value));
            // copy address part
            const size_t len = zmq_msg_size(&msg2);
            ep = static_cast<char *>(memcpy(ep, zmq_msg_data(&msg2), len));
            *(ep + len) = 0;
        } else {
            result = -1;
        }
        pthread_mutex_unlock(&mutex_monitorSocket);
        return result;
    }

    void ZeroMq::monitorConnection() {
        zmq_event_t event;
        static char addr[1025];
        int rc;

        std::string monMessage = "ZMQ_EVENT:starting monitor..."+identifier+"\n";
        LOG("%s",monMessage.c_str());

        monitorSocket = zmq_socket(context, ZMQ_PAIR);
        int linger = 0;
        rc = zmq_setsockopt(monitorSocket, ZMQ_LINGER, &linger, sizeof(linger));
        std::string monitorsocket = "inproc://monitor.dealer-"+identifier;
        rc = zmq_connect(monitorSocket, monitorsocket.c_str());
        bool continueProcessing = true;
        while (continueProcessing && !read_event_msg(monitorSocket, &event, addr)) {
            switch (event.event) {
                case ZMQ_EVENT_LISTENING:
                    LOG("ZMQ_EVENT:listening socket descriptor %d [%s]\n", event.value,identifier.c_str());
                    LOG("ZMQ_EVENT:listening socket address %s [%s]\n", addr,identifier.c_str());
                    break;
                case ZMQ_EVENT_ACCEPTED:
                    LOG("ZMQ_EVENT:accepted socket descriptor %d [%s]\n", event.value,identifier.c_str());
                    LOG("ZMQ_EVENT:accepted socket address %s [%s]\n", addr,identifier.c_str());
                    break;
                case ZMQ_EVENT_CLOSE_FAILED:
                    LOG("ZMQ_EVENT:socket close failure error code %d [%s]\n", event.value,identifier.c_str());
                    LOG("ZMQ_EVENT:socket address %s [%s]\n", addr,identifier.c_str());
                    break;
                case ZMQ_EVENT_CLOSED:
                    LOG("ZMQ_EVENT:closed socket descriptor %d [%s]\n", event.value,identifier.c_str());
                    LOG("ZMQ_EVENT:closed socket address %s [%s] \n", addr,identifier.c_str());
                    nativeCommunicator->unableToConnectZmq();
                    connectionStatus = -1;
                    connection_cond.notify_one();
                    continueProcessing = false;
                    break;
                case ZMQ_EVENT_DISCONNECTED:
                    LOG("ZMQ_EVENT:disconnected socket descriptor %d [%s]\n", event.value,identifier.c_str());
                    LOG("ZMQ_EVENT:disconnected socket address %s [%s]\n", addr,identifier.c_str());
                    nativeCommunicator->unableToConnectZmq();
                    connectionStatus = -1;
                    connection_cond.notify_one();
                    continueProcessing = false;
                    break;
                case ZMQ_EVENT_CONNECT_DELAYED:
                    LOG("ZMQ_EVENT:connect_delayed socket descriptor %d [%s]\n", event.value,identifier.c_str());
                    LOG("ZMQ_EVENT:connect_delayed socket address %s [%s]\n", addr,identifier.c_str());
                    break;
                case ZMQ_EVENT_CONNECTED:
                    LOG("ZMQ_EVENT:connected socket descriptor %d [%s]\n", event.value,identifier.c_str());
                    LOG("ZMQ_EVENT:connected socket address %s [%s]\n", addr,identifier.c_str());
                    connection_cond.notify_one();
                    break;
                case ZMQ_EVENT_MONITOR_STOPPED:
                    LOG("ZMQ_EVENT:monitor socket descriptor %d [%s]\n", event.value,identifier.c_str());
                    LOG("ZMQ_EVENT:monitor socket address %s [%s]\n", addr,identifier.c_str());
                    //nativeCommunicator->unableToConnectZmq();
                    continueProcessing = false;
                    break;

            }
        }
         LOG("ZMQ_EVENT:monitor closed [%s]",identifier.c_str());
    }

    std::vector<std::string> ZeroMq::s_recv_multi(int num)  {
        int64_t more;
        size_t more_size = sizeof more;
        std::vector<std::string> messages;
        int count = 0;
        pthread_mutex_lock(&mutex_mainSocket);
        if (mainSocketAvailable) {
            do {
                /* Create an empty ØMQ message to hold the message part */
                zmq_msg_t part;
                int rc = zmq_msg_init(&part);
                assert(rc == 0);
                /* Block until a message is available to be received from socket */
                int size = zmq_msg_recv(&part, socket, 0);
                assert(size != -1);
                char *m = (char *) malloc(size + 1);
                memcpy(m, zmq_msg_data(&part), size);
                std::string msg(m);
                messages.push_back(msg);
                /* Determine if more message parts are to follow */
                rc = zmq_getsockopt(socket, ZMQ_RCVMORE, &more, &more_size);
                assert(rc == 0);
                zmq_msg_close(&part);
                count = count + 1;
            } while ((more) && (count < num));
        }
        pthread_mutex_unlock(&mutex_mainSocket);
        return messages;
    }
    char *ZeroMq::s_recv() {
        char *result;
        pthread_mutex_lock(&mutex_mainSocket);
        if (mainSocketAvailable) {
            zmq_msg_t part;
            int rc = zmq_msg_init(&part);
            assert(rc == 0);
            /* Block until a message is available to be received from socket */
            int size = zmq_msg_recv(&part, socket, 0);
            assert(size != -1);
            result = (char *) malloc(size + 1);
            memcpy(result, zmq_msg_data(&part), size);
        }
        pthread_mutex_unlock(&mutex_mainSocket);
        return result;
    }

    int ZeroMq::s_send(char *string) {
        pthread_mutex_lock(&mutex_mainSocket);

        int size = -1;
        if (mainSocketAvailable) {
            size = zmq_send(socket, string, strlen(string), 0);
        }
        pthread_mutex_unlock(&mutex_mainSocket);
        return size;
    }

    int ZeroMq::s_sendmore(char *string) {
        pthread_mutex_lock(&mutex_mainSocket);
        int size =  -1;
        if (mainSocketAvailable) {
            size = zmq_send(socket, string, strlen(string), ZMQ_SNDMORE);
        }
        pthread_mutex_unlock(&mutex_mainSocket);
        return size;
    }

    int ZeroMq::s_sendmultiple(std::vector<std::string> messages) {

        int result = 0;
        //LOG("START_SEND_MUTLIPLE");
        pthread_mutex_unlock(&mutex_mainSocket);
        if (mainSocketAvailable) {
            int size = messages.size();
            for (int i = 0; i < (size - 1); i++) {
                std::string msg = messages[i];
                zmq_send(socket, msg.c_str(), msg.length(), ZMQ_SNDMORE);
            }
            std::string msg = messages[size - 1];
            zmq_send(socket, msg.c_str(), msg.length(), 0);
        } else {
            result = -1;
        }
        //LOG("FINNISH_SEND_MULTIPLE");
        pthread_mutex_unlock(&mutex_mainSocket);
        return result;
    }

    void  ZeroMq::setNativeCommunicator(NativeCommunicator *nc) {
        nativeCommunicator =  nc;
    }
}