/*
 * IReadable.h
 *
 *  Created on: 2012/01/01
 *      Author: nobnak
 */

#ifndef IREADABLE_H_
#define IREADABLE_H_

template <typename T> class IWritable {
public:
    virtual ~IWritable() {};
    virtual void write(T* src, int n) = 0;
    virtual void end() = 0;
};

template <typename T> class IReadable {
public:
    virtual ~IReadable() {};
    virtual void pipe(IWritable<T> *dst) = 0;
};

#endif /* IREADABLE_H_ */