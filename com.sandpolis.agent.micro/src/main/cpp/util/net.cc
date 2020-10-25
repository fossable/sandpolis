//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
#include "util/net.hh"

int OpenConnection(const std::string _hostname, const int _port) {
	const char *hostname = _hostname.c_str();
	char port[32];
	sprintf(port, "%d", _port);

	struct addrinfo hints = { 0 }, *addrs;
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_protocol = IPPROTO_TCP;

	const int status = getaddrinfo(hostname, port, &hints, &addrs);
	if (status != 0) {
		fprintf(stderr, "%s: %s\n", hostname, gai_strerror(status));
	}

	int sfd;
	for (struct addrinfo *addr = addrs; addr != nullptr; addr = addr->ai_next) {
		sfd = socket(addrs->ai_family, addrs->ai_socktype, addrs->ai_protocol);
		if (sfd == -1) {
			continue;
		}

		if (connect(sfd, addr->ai_addr, addr->ai_addrlen) == 0) {
			// Connection success
			break;
		}

		close(sfd);
		sfd = -1;
	}

	freeaddrinfo(addrs);
	return sfd;
}

/*SSL_CTX* InitSSL_CTX(void) {
const SSL_METHOD *method = TLS_client_method();
SSL_CTX *ctx = SSL_CTX_new(method);

if (ctx == nullptr) {
ERR_print_errors_fp (stderr);
exit(1);
}
return ctx;
}

void DisplayCerts(SSL *ssl) {
X509 *cert = SSL_get_peer_certificate(ssl);
if (cert != nullptr) {
printf("Server certificates:\n");
char *line = X509_NAME_oneline(X509_get_subject_name(cert), 0, 0);
printf("Subject: %s\n", line);
delete line;
line = X509_NAME_oneline(X509_get_issuer_name(cert), 0, 0);
printf("Issuer: %s\n", line);
delete line;
X509_free(cert);
} else {
printf("Info: No client certificates configured.\n");
}
}

int main(int argc, char const *argv[]) {
SSL_CTX *ctx = InitSSL_CTX();
SSL *ssl = SSL_new(ctx);
if (ssl == nullptr) {
fprintf(stderr, "SSL_new() failed\n");
exit (EXIT_FAILURE);
}

const int sfd = OpenConnection("127.0.0.1", argv[1]);
SSL_set_fd(ssl, sfd);

const int status = SSL_connect(ssl);
if (status != 1) {
SSL_get_error(ssl, status);
ERR_print_errors_fp (stderr);
fprintf(stderr, "SSL_connect failed with SSL_get_error code %d\n",
status);
exit (EXIT_FAILURE);
}

printf("Connected with %s encryption\n", SSL_get_cipher(ssl));
const char *chars = "Hello World, 123!";
SSL_write(ssl, chars, strlen(chars));
SSL_free(ssl);
close(sfd);
SSL_CTX_free(ctx);
return 0;
}
*/
