FROM navikt/kafka-plain-saslserver-2-ad:2.0_0.56

COPY ldapconfig.yaml /etc/kafka
COPY kafka_server_jaas.conf /etc/kafka
RUN echo "security.protocol=SASL_PLAINTEXT" >> /etc/kafka/producer.properties
RUN echo "sasl.mechanism=PLAIN" >> /etc/kafka/producer.properties
