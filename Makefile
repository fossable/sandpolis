## This makefile provides convenient access to optional utilities. It doesn't
## actually build anything.

updateSubmodules:
	git submodule foreach --recursive \
		git submodule update --init --remote --merge

createVersionCommit:
	git commit -m "v$(shell date +'%Y.%m.%d')"
	git tag "v$(shell date +'%Y.%m.%d')"

updateGitIgnore:
	find . -type f -name '.gitignore' -exec cp .gitignore {} \;

enableAgentMicro: enableModules
	git submodule update --init com.sandpolis.agent.micro
	(cd com.sandpolis.agent.micro; git checkout master; git pull origin master)

disableAgentMicro:
	git submodule deinit --force com.sandpolis.agent.micro

enableAgentNano: enableModules
	git submodule update --init com.sandpolis.agent.nano
	(cd com.sandpolis.agent.nano; git checkout master; git pull origin master)

disableAgentNano:
	git submodule deinit --force com.sandpolis.agent.nano

enableAgentVanilla: enableModules
	git submodule update --init com.sandpolis.agent.vanilla
	(cd com.sandpolis.agent.vanilla; git checkout master; git pull origin master)

disableAgentVanilla:
	git submodule deinit --force com.sandpolis.agent.vanilla

enableClientAscetic: enableModules
	git submodule update --init com.sandpolis.client.ascetic
	(cd com.sandpolis.client.ascetic; git checkout master; git pull origin master)

disableClientAscetic:
	git submodule deinit --force com.sandpolis.client.ascetic

enableClientLifegem: enableModules
	git submodule update --init com.sandpolis.client.lifegem
	(cd com.sandpolis.client.lifegem; git checkout master; git pull origin master)

disableClientLifegem:
	git submodule deinit --force com.sandpolis.client.lifegem

enableClientLockstone: enableModules
	git submodule update --init com.sandpolis.client.lockstone
	(cd com.sandpolis.client.lockstone; git checkout master; git pull origin master)

disableClientLockstone:
	git submodule deinit --force com.sandpolis.client.lockstone

enableDistagent:
	git submodule update --init com.sandpolis.distagent
	(cd com.sandpolis.distagent; git checkout master; git pull origin master)

disableDistagent:
	git submodule deinit --force com.sandpolis.distagent

enableServerVanilla: enableModules
	git submodule update --init com.sandpolis.server.vanilla
	(cd com.sandpolis.server.vanilla; git checkout master; git pull origin master)

disableServerVanilla:
	git submodule deinit --force com.sandpolis.server.vanilla

enableModules:
	git submodule update --init module
	(cd module; git submodule foreach 'git checkout master && git pull origin master')

disableModules:
	git submodule deinit --force module

enablePlugins:
	git submodule update --init plugin
	(cd plugin; git submodule foreach 'git checkout master && git pull origin master')

disablePlugins:
	git submodule deinit --force plugin

vLinux:
	vagrant status linux | grep -q 'running' || vagrant up linux
	vagrant ssh linux
