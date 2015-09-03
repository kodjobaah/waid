/*
 * androidlog.h
 *
 *  Created on: 2011/12/30
 *      Author: nobnak
 */

#ifndef ANDROIDLOG_H_
#define ANDROIDLOG_H_

class Log {
public:
    static void d(const char *msg);
private:
    static const char *LOG_TAG;
};

#endif /* ANDROIDLOG_H_ */