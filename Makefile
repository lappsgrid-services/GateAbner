VERSION=$(shell cat VERSION)
WAR=GateAbner\#$(VERSION).war
TGZ=GateAbner-$(VERSION).tgz

include ../master.mk

docker:
	cd src/main/docker && make

start:
	./src/test/integration/start.sh
	
stop:
	./src/test/integration/stop.sh
	
test:
	lsd ./src/test/integration/test.lsd

test-lif:
	lsd ./src/test/integration/test-lif.lsd
	
	
test-remote:
	lsd ./src/test/integration/deployment.lsd
