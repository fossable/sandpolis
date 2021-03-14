## This makefile provides convenient access to optional utilities. It doesn't
## actually build anything.

updateSubmodules:
	git submodule foreach git pull origin master

createVersionCommit:
	git commit -m "v$(shell date +'%Y.%m.%d')"
	git tag "v$(shell date +'%Y.%m.%d')"

updateJavaVersion:
	[ "${VERSION}" != "" ]

	find . -type f -name 'Dockerfile' -exec \
		sed -i "s/FROM adoptopenjdk:..-/FROM adoptopenjdk:${VERSION}-/g" {} \;

	find . -type f -name '*.yml' -exec \
		sed -i "s/java-version: ../java-version: ${VERSION}/g" {} \;

	find . -type f -name '*.gradle.kts' -exec \
		sed -i "s/JavaLanguageVersion.of(..)/JavaLanguageVersion.of(${VERSION})/g;s/jvmTarget = \"..\"/jvmTarget = \"${VERSION}\"/g;s/version = \"..\"/version = \"${VERSION}\"/g" {} \;

enableAgentMicro:
	git submodule update --init com.sandpolis.agent.micro

disableAgentMicro:
	git submodule deinit com.sandpolis.agent.micro

enableAgentNano:
	git submodule update --init com.sandpolis.agent.nano

disableAgentNano:
	git submodule deinit com.sandpolis.agent.nano

enableAgentVanilla:
	git submodule update --init com.sandpolis.agent.vanilla

disableAgentVanilla:
	git submodule deinit com.sandpolis.agent.vanilla

enableClientAscetic:
	git submodule update --init com.sandpolis.client.ascetic

disableClientAscetic:
	git submodule deinit com.sandpolis.client.ascetic

enableClientLifegem:
	git submodule update --init com.sandpolis.client.lifegem

disableClientLifegem:
	git submodule deinit com.sandpolis.client.lifegem

enableClientLockstone:
	git submodule update --init com.sandpolis.client.lockstone

disableClientLockstone:
	git submodule deinit com.sandpolis.client.lockstone

enableServerVanilla:
	git submodule update --init com.sandpolis.server.vanilla

disableServerVanilla:
	git submodule deinit com.sandpolis.server.vanilla

enableModules:
	git submodule update --init module

disableModules:
	git submodule deinit module

enablePlugins:
	git submodule update --init plugin

disablePlugins:
	git submodule deinit plugin
