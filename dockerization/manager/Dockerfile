FROM harness/ubi:safe-ubi8.5

USER root
RUN microdnf install -y wget \
  	&& microdnf  clean all \
  	&& rm -rf /var/cache/yum
      
RUN wget https://github.com/mikefarah/yq/releases/download/3.3.2/yq_linux_amd64 -O /usr/bin/yq -O /usr/bin/yq
RUN chmod +x /usr/bin/yq
USER nobody

CMD [ "./run.sh" ]
