//
// Created by kodjo baah on 24/08/2015.
//

#ifndef WAID_ZMQEXCEPTION_H
#define WAID_ZMQEXCEPTION_H

#include <iostream>
#include <exception>

class zmq_multi_send_exception_def : public std::exception {
    virtual  char const* what() {
       return "Zmq Socket closed";
    }
} zmq_multi_send_exception;

#endif //WAID_ZMQEXCEPTION_H
