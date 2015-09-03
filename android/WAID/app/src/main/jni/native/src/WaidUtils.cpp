//
// Created by kodjo baah on 23/08/2015.
//

#include <WaidUtils.h>

std::string WaidUtils::base64Encode(const unsigned char *buffer, size_t length, char **b64text) { //Encodes a binary safe base 64 string

    BIO *bmem, *b64;
    BUF_MEM *bptr;

    b64 = BIO_new(BIO_f_base64());
    bmem = BIO_new(BIO_s_mem());
    b64 = BIO_push(b64, bmem);
    BIO_set_flags(bmem, BIO_FLAGS_BASE64_NO_NL);
    BIO_write(b64, buffer, length);
    BIO_flush(b64);
    BIO_get_mem_ptr(b64, &bptr);
    BIO_set_close(bmem, BIO_NOCLOSE);

    char *buf = (char *) malloc(bptr->length + 1);
    memcpy(buf, bptr->data, bptr->length);
    buf[bptr->length] = '\0';
    std::string message(buf);

    BIO_free_all(b64);
    return message; //success
}